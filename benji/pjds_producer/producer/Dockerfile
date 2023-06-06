#Build
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /producerapp
COPY src ./
COPY pom.xml ./
COPY electric_data_ex.txt ./
RUN mvn -f ./pom.xml clean package

#Package
FROM openjdk:21-ea-17-slim-buster

COPY --from=build /producerapp/target/producer-1.0-SNAPSHOT.jar ./jars/ProducerApp.jar

ENTRYPOINT ["java","-jar","./jars/ProducerApp.jar"]