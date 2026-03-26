FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src/ src/

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

ENV PORT=8080

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar /app/app.jar"]
