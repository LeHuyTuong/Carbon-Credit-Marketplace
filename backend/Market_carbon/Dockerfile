FROM openjdk:17

ARG FILE_JAR=target/spring-rest-auth-vehicles-kyc-0.0.1-SNAPSHOT.jar
ADD ${FILE_JAR} app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]

EXPOSE 8082
