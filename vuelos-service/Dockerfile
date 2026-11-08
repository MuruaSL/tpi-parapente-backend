# Etapa 1: compila el jar dentro del contenedor (no hace falta Java ni Maven en la máquina)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# Etapa 2: imagen liviana que sólo corre el jar
FROM eclipse-temurin:21-jre
# curl se usa en el healthcheck de docker-compose (la imagen jre no lo trae)
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN useradd --system --no-create-home app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
