FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY common-service/pom.xml common-service/
COPY inventory-service/pom.xml inventory-service/
COPY gateway-service/pom.xml gateway-service/
COPY common-service/src common-service/src
COPY order-service/pom.xml order-service/
COPY order-service/src order-service/src
RUN mvn -pl order-service -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/order-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
