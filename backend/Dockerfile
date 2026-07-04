FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
# Compilar o projeto primeiro
RUN mvn compile -DskipTests
# Compilar relatórios JasperReports (.jrxml -> .jasper) - opcional, falha silenciosa
RUN mvn exec:java -Dexec.mainClass="com.financeflow.reports.JasperReportCompiler" -Dexec.classpathScope=compile 2>&1 || echo "Compilação de relatórios opcional - continuando..."
# Fazer o package final (inclui os .jasper compilados se existirem)
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache wget

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
