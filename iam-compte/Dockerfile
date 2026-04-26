# ──────────────────────────────────────────────────────────────────────
# Dockerfile Application IAM Compte
# Application Spring Boot avec dépendances Keycloak
# ──────────────────────────────────────────────────────────────────────

FROM maven:3.9.4-openjdk-17-slim as builder

# Variables d'environnement pour le build
ENV MAVEN_OPTS="-Xmx512m"
ENV SPRING_PROFILES_ACTIVE=dev

# Copie du code source
WORKDIR /app
COPY --chown=mvn:mvn pom.xml .
COPY --chown=mvn:mvn src ./src

# Build de l'application
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests

# ──────────────────────────────────────────────────────────────────────
# Image de production
# ──────────────────────────────────────────────────────────────────────

FROM openjdk:17-jre-slim

# Installation des utilitaires nécessaires
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Création de l'utilisateur de l'application
RUN groupadd -r iam && useradd -r -g iam iam

# Variables d'environnement par défaut
ENV SPRING_PROFILES_ACTIVE=dev
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Copie du JAR
WORKDIR /app
COPY --from=builder --chown=iam:iam /app/target/iam-compte-26.6.0.jar app.jar

# Création du répertoire de logs
RUN mkdir -p /app/logs && chown -R iam:iam /app/logs

# Permissions
RUN chown -R iam:iam /app

# Changement d'utilisateur
USER iam

# Ports exposés
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]
