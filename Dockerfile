# CHANGE 11: Dockerfile — multi-stage build.
#
# Stage 1 (builder): Uses Maven to compile and package the app into a JAR.
# Stage 2 (runtime): Uses a minimal JRE image — no build tools, smaller attack surface.
# Result: Production image is ~200MB instead of ~500MB.

# ── Stage 1: Build ────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml first — Docker caches this layer.
# If only source files change (not dependencies), Maven won't re-download the internet.
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Run ──────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user for security — never run as root in production
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
