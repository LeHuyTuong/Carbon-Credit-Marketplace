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
        List<String> problems = new ArrayList<>();

        Metamodel metamodel = entityManager.getMetamodel();
        for (EntityType<?> type : metamodel.getEntities()) {
            String tableName = tableName(type);
            Map<String, String> columns = sql.get(tableName);
            if (columns == null) {
                problems.add("core_ccm.sql has no table for entity " + type.getJavaType().getSimpleName()
                        + " (" + tableName + ")");
                continue;
            }
            checkAttributes(type, columns, problems);
        }

        assertThat(problems).isEmpty();
    }

    private void checkAttributes(EntityType<?> type, Map<String, String> columns, List<String> problems) {
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

            String sqlType = columns.get(columnName);
            if (sqlType == null) {
                continue;
            }
            if (basic.getJavaType() == BigDecimal.class && column != null && column.precision() > 0) {
                String expected = "decimal(" + column.precision() + "," + column.scale() + ")";
                if (!normalize(sqlType).startsWith(expected)) {
                    problems.add(type.getJavaType().getSimpleName() + "." + columnName
                            + " maps BigDecimal(" + column.precision() + "," + column.scale()
                            + ") but core_ccm.sql declares " + normalize(sqlType));
                }
            } else if (basic.getJavaType() == String.class && column != null && column.length() > 0
                    && column.columnDefinition().isEmpty()) {
                String expected = "varchar(" + column.length() + ")";
                if (!normalize(sqlType).startsWith(expected)) {
                    problems.add(type.getJavaType().getSimpleName() + "." + columnName
                            + " maps String(" + column.length() + ")"
                            + " but core_ccm.sql declares " + normalize(sqlType));
                }
            }
            jakarta.persistence.Enumerated enumerated =
                    field.getAnnotation(jakarta.persistence.Enumerated.class);
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
