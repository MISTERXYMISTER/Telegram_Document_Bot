FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy entire project
COPY . .

# Build inside container
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Run the jar
ENTRYPOINT ["java", "-jar", "target/telegram-doc-bot-0.0.1-SNAPSHOT.jar"]