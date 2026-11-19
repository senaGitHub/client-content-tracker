# --- Build stage: compile the jar with the Maven wrapper ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy only the build definition first: this layer (all dependencies) stays cached
# until pom.xml changes, so code-only changes rebuild much faster.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q package -DskipTests && cp target/*.jar app.jar

# --- Runtime stage: JRE + jar only ---
FROM eclipse-temurin:21-jre

# "Today" (overdue, due this week) must follow Turkish time, not the host's UTC.
ENV TZ=Europe/Istanbul
# Let the JVM size its heap from the container limit (Render free tier: 512 MB).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
