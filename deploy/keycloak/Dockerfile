# =============================================================================
# Dockerfile Keycloak 26.6.0 Personalisé IAM Local UOB v0.0.0
# Basé sur votre distribution buildée localement
# =============================================================================

FROM eclipse-temurin:17-jre-alpine

# Variables d'environnement
ENV KC_HTTP_PORT=8080
ENV KC_HOSTNAME_STRICT=false
ENV KC_FEATURES=preview,account-api,admin-fine-grained-authz
ENV KC_LOG_LEVEL=INFO
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
# Désactiver la création automatique d'admin
ENV KC_BOOTSTRAP_ADMIN_USERNAME=""
ENV KC_BOOTSTRAP_ADMIN_PASSWORD=""

# Création de l'utilisateur keycloak
RUN addgroup -S keycloak && adduser -S -G keycloak keycloak

# Installation des dépendances
RUN apk update && apk add --no-cache \
    curl \
    && rm -rf /var/cache/apk/*

# Copie de la distribution Keycloak personnalisée
COPY --chown=keycloak:keycloak . /opt/keycloak

# Permissions
RUN chmod +x /opt/keycloak/bin/*.sh

# Création des répertoires de logs et données
RUN mkdir -p /opt/keycloak/logs /opt/keycloak/data && \
    chown -R keycloak:keycloak /opt/keycloak

# Changement d'utilisateur
USER keycloak

# Ports exposés
EXPOSE 8080 8787

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=30 \
    CMD curl -f http://localhost:8080/health/ready || exit 1

# Commande de démarrage
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--optimized"]
