FROM node:20-bookworm-slim AS frontend-builder

WORKDIR /app/bms-FE

COPY bms-FE/package.json bms-FE/package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY bms-FE/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend-builder

WORKDIR /app/bms

COPY bms/pom.xml ./
COPY bms/src ./src
COPY --from=frontend-builder /app/bms-FE/dist/bms-fe/browser/ ./src/main/resources/static/
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=backend-builder /app/bms/target/*.jar /app/app.jar

ENV PORT=8080

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar /app/app.jar"]
