# ============================================================
# Stage 1: Build
# ============================================================
FROM eclipse-temurin:17-jdk-focal AS build

# Install Maven
RUN apt-get update && apt-get install -y --no-install-recommends \
    maven \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /build

# Copy build files first for better layer caching
COPY pom.xml .
COPY lib/ ./lib/

# Install local JAR (martyr) to local Maven repo, then download dependencies
RUN mvn install:install-file \
      -Dfile=./lib/martyr-1.0.jar \
      -DgroupId=f00f.net.irc \
      -DartifactId=martyr \
      -Dversion="1.0" \
      -Dpackaging=jar \
      -DgeneratePom=true

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -DskipTests

# Copy source and build
COPY src/ ./src/
RUN mvn clean package -DskipTests

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:17-jre-focal

# Install MySQL client for database bootstrap
RUN apt-get update && apt-get install -y --no-install-recommends \
    mysql-client \
    curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r qlink && useradd -r -g qlink -d /home/qlink -m qlink

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /build/target/qlink-0.1.0.jar ./qlink.jar

# Copy runtime scripts and SQL files
COPY dockerrun ./dockerrun
COPY run ./run
COPY bootstrap ./bootstrap
COPY schema.sql ./schema.sql
COPY dev_privileges.sql ./dev_privileges.sql
COPY skern.sql ./skern.sql
COPY src/main/resources/ ./resources/

# Fix Windows line endings (CRLF -> LF) and make scripts executable
RUN sed -i 's/\r$//' ./dockerrun ./run ./bootstrap \
    && chmod +x ./dockerrun ./run ./bootstrap

# Create logs directory for the application
RUN mkdir -p /app/logs && chown qlink:qlink /app/logs

# Switch to non-root user
USER qlink

# Expose Q-Link ports
EXPOSE 5190 1986

# Health check - verify the server is responding
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:5190/ || exit 1

# Environment variables with sensible defaults
ENV QLINK_DB_HOST=mysql
ENV QLINK_DB_PORT=3306
ENV QLINK_DB_USERNAME=qlinkuser
ENV QLINK_DB_PASSWORD=qlinkpass
ENV QLINK_DB_JDBC_URI=jdbc:mysql://${QLINK_DB_HOST}:${QLINK_DB_PORT}/qlink
ENV QLINK_SHOULD_CREATE_DB=true

ENTRYPOINT ["/bin/bash", "/app/dockerrun"]
