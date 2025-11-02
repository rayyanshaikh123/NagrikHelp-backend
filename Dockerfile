FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Copy everything and build the application
COPY . /workspace

# Ensure the Gradle wrapper is executable and build the bootJar
RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the jar produced by the build stage
COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080

# Allow Render (or other hosts) to set PORT; default to 8080
ENTRYPOINT ["sh","-c","java -jar /app/app.jar --server.port=${PORT:-8080}"]
