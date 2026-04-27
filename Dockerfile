FROM eclipse-temurin:21-jre

WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring

ARG JAR_FILE
ARG SPRING_PROFILES_ACTIVE=prod

COPY ${JAR_FILE} /app/application.jar

RUN chown spring:spring /app/application.jar

USER spring:spring

ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}

ENTRYPOINT ["java", "-jar", "/app/application.jar"]