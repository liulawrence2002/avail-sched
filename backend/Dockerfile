FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon
COPY src/ src/
RUN ./gradlew bootJar --no-daemon

# Alpine/musl runtime. Flyway and the PostgreSQL JDBC driver are pure Java so they do not
# need glibc; this cuts the runtime image from ~260MB (Debian) to ~180MB and trims attack
# surface. (Phase 1.5 hardening.)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# - curl is needed for the HEALTHCHECK below.
# - Non-root user 'goblin' (uid 1001) owns /app and runs the process so a container break-out
#   lands as an unprivileged user.
RUN apk add --no-cache curl \
    && addgroup -g 1001 goblin \
    && adduser -D -u 1001 -G goblin goblin

COPY --from=build /app/build/libs/*.jar app.jar
RUN chown -R goblin:goblin /app
USER goblin

# JVM flags sized for Fly.io's 512MB shared-cpu-1x free tier:
# - MaxRAMPercentage=75 lets the JVM use 384MB of 512MB, leaving room for the JIT/metaspace
#   and the container runtime itself.
# - UseSerialGC drops the ~40MB G1 footprint that is overkill for a single-host scheduler.
# - TieredStopAtLevel=1 skips C2 compilation, trading a small steady-state throughput hit
#   for a faster cold start and lower code-cache usage.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
