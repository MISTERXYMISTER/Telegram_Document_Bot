FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

ENTRYPOINT ["java", "-jar", "target/telegram-doc-bot-0.0.1-SNAPSHOT.jar"]