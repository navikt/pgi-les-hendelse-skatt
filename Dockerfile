FROM gcr.io/distroless/java21-debian13:nonroot

WORKDIR /app

ENV TZ="Europe/Oslo"

COPY build/libs/pgi-les-hendelse-skatt.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]