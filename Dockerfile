# ETAPA 1: Construcción (Usamos Maven con Java 23)
FROM maven:3.9-eclipse-temurin-23 AS build
WORKDIR /app

# Copiamos primero el pom.xml y descargamos las dependencias
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiamos el código fuente y lo compilamos
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.compiler.parameters=true

# ETAPA 2: Ejecución
FROM eclipse-temurin:23-jre
WORKDIR /app

# Copiamos solo el archivo .jar de la Etapa 1
COPY --from=build /app/target/*.jar app.jar

# Exponemos el puerto estándar de Spring Boot
EXPOSE 8080

# Comando para arrancar el Tomcat embebido
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
