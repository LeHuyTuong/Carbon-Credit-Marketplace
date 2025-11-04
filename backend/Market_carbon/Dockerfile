# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
# Cache dependency để build nhanh
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests dependency:go-offline

# Chỉ copy mã nguồn runtime, không copy src/test
COPY src/main ./src/main

# skip cả compile & run test
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests -Dmaven.test.skip=true clean package

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
