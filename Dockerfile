FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN useradd --system --no-create-home appuser \
    && mkdir -p /data/uploads \
    && chown appuser:appuser /data/uploads

COPY target/memorycard-0.0.1.jar /app/app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
