FROM registry.access.redhat.com/ubi9/openjdk-17:1.16-3

# Use the JAR file built by Gradle from the build/libs directory
ARG JAR_FILE=build/libs/uamadapterreport-0.0.1-SNAPSHOT.jar

WORKDIR /app

# Copy the JAR file to the container
COPY ${JAR_FILE} app.jar

# Run the Spring Boot app with no active profile
ENTRYPOINT ["java", "-jar", "app.jar"]
