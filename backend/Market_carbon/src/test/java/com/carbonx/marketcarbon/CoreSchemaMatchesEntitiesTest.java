package com.carbonx.marketcarbon;

import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.type.BasicType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The prod profile runs {@code spring.jpa.hibernate.ddl-auto=validate}: Hibernate refuses to
 * start when a mapped table or column is missing, or when the column TYPE differs from the
 * mapping (it compares types, not only names).
 *
 * <p>Nothing else in the suite guards that: the prod config test only string-matches
 * application-prod.properties, and the SQL dump is never loaded. This test parses
 * {@code backend/database/core_ccm.sql} and checks it against the real JPA metamodel, so a
 * column that the entities map but the dump does not create (or that it creates with the wrong
 * type, precision, length or enum values) fails here instead of in production.</p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class CoreSchemaMatchesEntitiesTest {

    private static final Path CORE_SQL = Path.of("..", "database", "core_ccm.sql");

    @Autowired private EntityManager entityManager;

    @Test
    void coreSqlCreatesEveryTableAndColumnTheEntitiesMap() throws Exception {
        Map<String, Map<String, String>> sql = parseCoreSql();
        List<String> migrationTargets = applyMigrations(sql);
        List<String> problems = new ArrayList<>();

        Metamodel metamodel = entityManager.getMetamodel();
        Map<String, java.util.Set<String>> mappedColumns = new LinkedHashMap<>();
        for (EntityType<?> type : metamodel.getEntities()) {
            String tableName = tableName(type);
            Map<String, String> columns = sql.get(tableName);
            if (columns == null) {
                problems.add("core_ccm.sql has no table for entity " + type.getJavaType().getSimpleName()
                        + " (" + tableName + ")");
                continue;
            }
            java.util.Set<String> mapped = new java.util.LinkedHashSet<>();
            mappedColumns.put(tableName, mapped);
            checkAttributes(type, columns, mapped, problems);
        }

        // Mỗi ALTER trong migration phải trỏ tới một cột mà entity thực sự map: gõ sai
        // tên cột thì script chạy nhưng không làm gì, và DB cũ vẫn lệch với entity.
        for (String target : migrationTargets) {
            String[] parts = target.split("\\.");
            String table = parts[0];
            String column = parts.length > 1 ? parts[1] : "";
            java.util.Set<String> mapped = mappedColumns.get(table);
            if (mapped == null || !mapped.contains(column)) {
                problems.add("the migrations alter " + target
                        + " which the entities do not map (typo?)");
            }
        }

        assertThat(problems).isEmpty();
    }

    /**
     * Áp dụng các script trong {@code backend/database/migrations/} lên schema đã đọc từ
     * core_ccm.sql: đây mới là đường đi thật của prod (dump + migration), và nhiều thay
     * đổi kiểu/enum chỉ nằm trong migration (ví dụ WITHDRAWAL_REFUND).
     */
    private List<String> applyMigrations(Map<String, Map<String, String>> sql) throws Exception {
        List<String> targets = new ArrayList<>();
        Path migrations = Path.of("..", "database", "migrations");
        if (!Files.isDirectory(migrations)) {
            return targets;
        }
        // Câu lệnh ALTER nằm trong string literal; MODIFY có thể cắt qua hai literal
        // nên phần kiểu có thể rỗng — khi đó giữ kiểu sẵn có trong dump.
        Pattern alter = Pattern.compile(
                "'ALTER TABLE `(\\w+)` (ADD|MODIFY) COLUMN `(\\w+)`\\s*([^']*)'");
        try (java.util.stream.Stream<Path> files = Files.list(migrations)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".sql")).sorted().toList()) {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                Matcher matcher = alter.matcher(content);
                while (matcher.find()) {
                    String table = matcher.group(1);
                    String column = matcher.group(3);
                    String type = matcher.group(4).trim();
                    Map<String, String> columns = sql.computeIfAbsent(table, key -> new LinkedHashMap<>());
                    if (!type.isBlank()) {
                        columns.put(column, type);
                    } else if (!columns.containsKey(column)) {
                        columns.put(column, "");
                    }
                    targets.add(table + "." + column);
                }
            }
        }
        return targets;
    }

    private void checkAttributes(EntityType<?> type, Map<String, String> columns,
                                 java.util.Set<String> mapped, List<String> problems) {
        for (Attribute<?, ?> attribute : type.getAttributes()) {
            if (!(attribute instanceof SingularAttribute<?, ?> singular)) {
                // Collections (OneToMany/ManyToMany) live in a join table, not a column.
                continue;
            }
            Object member = singular.getJavaMember();
            if (!(member instanceof Field field)) {
                continue;
            }
            if (field.getAnnotation(jakarta.persistence.Transient.class) != null) {
                continue;
            }

            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            if (joinColumn != null && !joinColumn.name().isEmpty()) {
                requireColumn(type, columns, joinColumn.name(), problems);
                mapped.add(joinColumn.name());
                continue;
            }
            jakarta.persistence.OneToOne oneToOne =
                    field.getAnnotation(jakarta.persistence.OneToOne.class);
            boolean inverseSide = oneToOne != null && !oneToOne.mappedBy().isEmpty();
            if (!inverseSide && (field.getAnnotation(jakarta.persistence.ManyToOne.class) != null
                    || oneToOne != null)) {
                // Quan hệ sở hữu cột (không khai @JoinColumn): Hibernate đặt tên cột
                // <property>_id. Quan hệ inverse (mappedBy) không có cột trong bảng này.
                String defaultColumn = toSnakeCase(field.getName()) + "_id";
                requireColumn(type, columns, defaultColumn, problems);
                mapped.add(defaultColumn);
                continue;
            }

            if (!(singular.getType() instanceof BasicType<?> basic)) {
                continue;
            }
            Column column = field.getAnnotation(Column.class);
            String columnName = column != null && !column.name().isEmpty()
                    ? column.name()
                    : toSnakeCase(singular.getName());
            requireColumn(type, columns, columnName, problems);
            mapped.add(columnName);

            String sqlType = columns.get(columnName);
            if (sqlType == null) {
                continue;
            }
            String sqlFamily = family(sqlType);
            jakarta.persistence.Enumerated enumerated =
                    field.getAnnotation(jakarta.persistence.Enumerated.class);
            List<String> expectedFamilies = expectedFamilies(basic, column, enumerated);
            if (expectedFamilies != null && !expectedFamilies.contains(sqlFamily)) {
                problems.add(type.getJavaType().getSimpleName() + "." + columnName
                        + " maps " + expectedFamilies + " but core_ccm.sql declares " + sqlFamily
                        + " (" + normalize(sqlType) + ")");
            }
            if (enumerated != null && basic.getJavaType().isEnum()) {
                List<String> expectedValues = new ArrayList<>();
                for (Object constant : basic.getJavaType().getEnumConstants()) {
                    expectedValues.add(((Enum<?>) constant).name().toUpperCase(Locale.ROOT));
                }
                List<String> sqlValues = enumValues(sqlType);
                if (!sqlValues.containsAll(expectedValues)) {
                    problems.add(type.getJavaType().getSimpleName() + "." + columnName
                            + " can write " + expectedValues
                            + " but core_ccm.sql only allows " + sqlValues);
                }
            }
        }
    }

    /**
     * Họ kiểu JDBC mà Hibernate dùng để validate. Validator của Hibernate so sánh MÃ
     * KIỂU (type code), không so sánh độ dài hay độ chính xác — nên một cột VARCHAR(20)
     * vẫn hợp lệ với mapping String(255), còn DOUBLE thay cho DECIMAL thì không.
     */
    private List<String> expectedFamilies(BasicType<?> basic, Column column,
                                         jakarta.persistence.Enumerated enumerated) {
        Class<?> javaType = basic.getJavaType();
        if (javaType == BigDecimal.class) {
            return List.of("decimal");
        }
        if (javaType == String.class) {
            // columnDefinition = "text" cố ý đổi kiểu, nên không so sánh.
            return column != null && !column.columnDefinition().isEmpty() ? null : List.of("varchar");
        }
        if (javaType == Integer.class) {
            return List.of("int");
        }
        if (javaType == Long.class) {
            return List.of("bigint");
        }
        if (javaType == Short.class) {
            return List.of("smallint");
        }
        if (javaType == Byte.class) {
            return List.of("tinyint");
        }
        if (javaType == Boolean.class) {
            return List.of("boolean", "bit", "tinyint");
        }
        if (javaType == LocalDate.class) {
            return List.of("date");
        }
        if (javaType == LocalDateTime.class || javaType == OffsetDateTime.class
                || javaType == Instant.class) {
            return List.of("datetime");
        }
        if (javaType.isEnum()) {
            // Enum lưu theo chuỗi thì cột là ENUM/VARCHAR; theo thứ tự thì là số nhỏ.
            boolean ordinal = enumerated == null
                    || enumerated.value() == jakarta.persistence.EnumType.ORDINAL;
            return ordinal ? List.of("tinyint", "int", "smallint") : List.of("enum", "varchar");
        }
        return null;
    }

    /** Họ kiểu của khai báo SQL trong core_ccm.sql. */
    private String family(String sqlType) {
        String normalized = normalize(sqlType);
        if (normalized.startsWith("enum(")) {
            return "enum";
        }
        if (normalized.startsWith("decimal(")) {
            return "decimal";
        }
        if (normalized.startsWith("varchar(")) {
            return "varchar";
        }
        if (normalized.startsWith("text")) {
            return "text";
        }
        if (normalized.startsWith("int")) {
            return "int";
        }
        if (normalized.startsWith("tinyint")) {
            return "tinyint";
        }
        if (normalized.startsWith("smallint")) {
            return "smallint";
        }
        if (normalized.startsWith("bigint")) {
            return "bigint";
        }
        if (normalized.startsWith("double")) {
            return "double";
        }
        if (normalized.startsWith("float")) {
            return "float";
        }
        if (normalized.startsWith("datetime")) {
            return "datetime";
        }
        if (normalized.startsWith("date")) {
            return "date";
        }
        if (normalized.startsWith("bit")) {
            return "boolean";
        }
        if (normalized.startsWith("binary") || normalized.startsWith("varbinary")
                || normalized.startsWith("blob")) {
            return "binary";
        }
        return normalized;
    }

    private void requireColumn(EntityType<?> type, Map<String, String> columns,
                               String columnName, List<String> problems) {
        if (!columns.containsKey(columnName)) {
            problems.add("core_ccm.sql has no column " + type.getJavaType().getSimpleName() + "."
                    + columnName);
        }
    }

    private String tableName(EntityType<?> type) {
        jakarta.persistence.Table table =
                type.getJavaType().getAnnotation(jakarta.persistence.Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        // Không có @Table: Hibernate dùng naming strategy (CamelCase -> snake_case).
        return toSnakeCase(type.getJavaType().getSimpleName());
    }

    /** table name -> column name -> declared SQL type (MySQL syntax, comments stripped). */
    private Map<String, Map<String, String>> parseCoreSql() throws Exception {
        String sql = Files.readString(CORE_SQL, StandardCharsets.UTF_8);
        Map<String, Map<String, String>> tables = new LinkedHashMap<>();
        Matcher tableMatcher = Pattern.compile(
                "CREATE TABLE IF NOT EXISTS `([^`]+)` \\((.*?)\\n\\) *ENGINE", Pattern.DOTALL).matcher(sql);
        while (tableMatcher.find()) {
            Map<String, String> columns = new LinkedHashMap<>();
            for (String part : splitTopLevel(stripComment(tableMatcher.group(2)))) {
                String cleaned = part.trim();
                Matcher columnMatcher = Pattern.compile("^`([^`]+)`\\s+(.+)$").matcher(cleaned);
                if (columnMatcher.matches()) {
                    columns.put(columnMatcher.group(1), columnMatcher.group(2).trim());
                }
            }
            tables.put(tableMatcher.group(1), columns);
        }
        return tables;
    }

    private List<String> splitTopLevel(String body) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : body.toCharArray()) {
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
            if (c == ',' && depth == 0) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts;
    }

    /** Strip "--" comments line by line (a body has many lines, one "--" must not cut the rest). */
    private String stripComment(String block) {
        StringBuilder cleaned = new StringBuilder();
        for (String line : block.split("\n")) {
            int index = line.indexOf("--");
            cleaned.append(index >= 0 ? line.substring(0, index) : line).append('\n');
        }
        return cleaned.toString();
    }

    private String normalize(String type) {
        String normalized = type.toLowerCase(Locale.ROOT).trim();
        normalized = normalized.replace("numeric", "decimal")
                .replace("timestamp(6) with time zone", "datetime(6)")
                .replace("timestamp(6)", "datetime(6)")
                .replace("timestamp", "datetime")
                .replace("integer", "int")
                .replace("character varying", "varchar")
                .replace("enum (", "enum(");
        normalized = normalized.replaceAll("\\s+", " ").replaceAll("\\(\\s*", "(").replaceAll("\\s*\\)", ")");
        normalized = normalized.replace(", ", ",");
        normalized = normalized.replace("default ", "");
        int defaultIndex = normalized.indexOf(" default ");
        if (defaultIndex > 0) {
            normalized = normalized.substring(0, defaultIndex);
        }
        return normalized.trim();
    }

    private List<String> enumValues(String sqlType) {
        String normalized = normalize(sqlType);
        if (!normalized.startsWith("enum(")) {
            return List.of();
        }
        String body = normalized.substring("enum(".length(), normalized.lastIndexOf(')'));
        return Arrays.stream(body.split(","))
                .map(value -> value.trim().replace("'", "").toUpperCase(Locale.ROOT))
                .toList();
    }

    private String toSnakeCase(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
