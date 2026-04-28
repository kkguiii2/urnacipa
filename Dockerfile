# Estágio de build
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app

# Copia apenas o pom.xml primeiro para baixar as dependências (cache)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o código-fonte e compila a aplicação
COPY src ./src
RUN mvn clean package -DskipTests

# Estágio de execução
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copia o JAR compilado do estágio anterior
COPY --from=build /app/target/sistema-votacao-cipa-1.0.0.jar app.jar

# Cria o diretório de uploads (necessário para o disco persistente no Render)
RUN mkdir -p /app/uploads

# Expõe a porta
EXPOSE 8080

# Comando de inicialização
ENTRYPOINT ["java", "-jar", "app.jar"]
