FROM maven:3.8.5-openjdk-17 AS build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package

FROM openjdk:21-ea-17-slim-buster
COPY --from=build /usr/src/app/target/producerapp-1.0-SNAPSHOT.jar /usr/app/producerapp-1.0-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/usr/app/producerapp-1.0-SNAPSHOT.jar"]
