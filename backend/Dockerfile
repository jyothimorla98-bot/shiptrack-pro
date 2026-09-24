# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- run ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd -ms /bin/bash shiptrack && mkdir -p /app/uploads && chown -R shiptrack /app
COPY --from=build /build/target/*.jar app.jar
USER shiptrack
EXPOSE 8080
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
