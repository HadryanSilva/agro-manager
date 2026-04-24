# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copia os arquivos de configuração do Gradle antes do código fonte
# para que o cache desta camada seja reaproveitado quando apenas o código mudar
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon --quiet

# Copia o código fonte e gera o JAR executável
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test --quiet

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Cria usuário sem privilégios para executar a aplicação
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

# JAVA_TOOL_OPTIONS é lido automaticamente pela JVM — configurado via docker-compose
ENTRYPOINT ["java", "-jar", "app.jar"]