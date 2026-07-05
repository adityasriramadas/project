FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY common-service/pom.xml common-service/
COPY order-service/pom.xml order-service/
COPY gateway-service/pom.xml gateway-service/
COPY common-service/src common-service/src
COPY inventory-service/pom.xml inventory-service/
COPY inventory-service/src inventory-service/src
RUN mvn -pl inventory-service -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/inventory-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
