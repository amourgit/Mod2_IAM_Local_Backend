#!/usr/bin/env bash
# =============================================================================
# Script de déploiement IAM Local UOB
# Usage : ./deploy.sh [start|stop|restart|logs|status|clean]
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"
ENV_FILE="${SCRIPT_DIR}/.env"

# Couleurs pour les logs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()    { echo -e "${GREEN}[IAM-DEPLOY]${NC} $1"; }
log_warn()    { echo -e "${YELLOW}[IAM-DEPLOY]${NC} $1"; }
log_error()   { echo -e "${RED}[IAM-DEPLOY]${NC} $1"; }
log_section() { echo -e "\n${BLUE}=== $1 ===${NC}"; }

# =============================================================================
# Fonctions
# =============================================================================

build_spi() {
    log_section "Build du SPI Keycloak"

    cd "${PROJECT_ROOT}"
    log_info "Compilation du module iam-compte-keycloak-spi..."

    mvn clean package \
        -pl iam-compte-keycloak-spi \
        -am \
        -DskipTests \
        -q

    SPI_JAR="${PROJECT_ROOT}/iam-compte-keycloak-spi/target/iam-compte-keycloak-spi.jar"

    if [ ! -f "${SPI_JAR}" ]; then
        log_error "Le JAR SPI n'a pas été produit : ${SPI_JAR}"
        exit 1
    fi

    log_info "Copie du SPI dans la distribution Keycloak personnalisée..."
    cp "${SPI_JAR}" "${PROJECT_ROOT}/quarkus/dist/target/keycloak-26.6.0/providers/"

    log_info "SPI déployé dans votre distribution Keycloak : $(ls -lh ${PROJECT_ROOT}/quarkus/dist/target/keycloak-26.6.0/providers/)"
}

build_iam_compte() {
    log_section "Build du module iam-compte"

    cd "${PROJECT_ROOT}"
    log_info "Compilation et packaging de iam-compte..."

    mvn clean package \
        -pl iam-compte \
        -am \
        -DskipTests \
        -q

    log_info "iam-compte buildé avec succès"
}

start() {
    log_section "Démarrage de IAM Local UOB - Version Native 0.0.0"

    # Build des modules
    build_spi
    build_iam_compte

    # Copie de la distribution Keycloak personnalisée
    log_info "Copie de votre distribution Keycloak personnalisée..."
    cp -r "${PROJECT_ROOT}/quarkus/dist/target/keycloak-26.6.0"/* "${SCRIPT_DIR}/keycloak/"

    # Vérification du fichier .env
    if [ ! -f "${ENV_FILE}" ]; then
        log_error "Fichier .env manquant : ${ENV_FILE}"
        log_error "Copiez deploy/.env.example et adaptez les valeurs"
        exit 1
    fi

    log_info "Démarrage des containers..."
    docker compose \
        --file "${COMPOSE_FILE}" \
        --env-file "${ENV_FILE}" \
        up -d \
        --build \
        --remove-orphans

    log_section "Attente de la disponibilité des services"

    log_info "Attente de PostgreSQL..."
    wait_for_healthy "iam-postgres" 60

    log_info "Attente de Kafka..."
    wait_for_healthy "iam-kafka" 90

    log_info "Attente de Keycloak..."
    wait_for_healthy "iam-keycloak" 120

    log_section "Statut final"
    status

    echo ""
    log_info "========================================"
    log_info "IAM Local UOB v0.0.0 - Native est opérationnel !"
    log_info "========================================"
    log_info "Keycloak Admin Console : http://localhost:8080"
    log_info "  Login : admin / Admin_UOB_2025!"
    log_info "  Version : Keycloak 26.6.0 + Modules IAM natifs"
    log_info "Kafka UI              : http://localhost:8888"
    log_info "PostgreSQL            : localhost:5432"
    log_info "  Base unique : keycloak_iam_local (user: keycloak)"
    log_info "  Vos champs natifs : ID_COMPTE_LOCAL, IDENTIFIANT_NATIONAL, STATUT_COMPTE"
    log_info "========================================"
    log_info "🚀 Votre aventure commence avec la version 0.0.0 !"
    log_info "========================================"
}

stop() {
    log_section "Arrêt de IAM Local UOB"
    docker compose \
        --file "${COMPOSE_FILE}" \
        --env-file "${ENV_FILE}" \
        down
    log_info "Tous les services sont arrêtés"
}

restart() {
    log_section "Redémarrage de IAM Local UOB"
    stop
    start
}

logs() {
    local service="${1:-}"
    docker compose \
        --file "${COMPOSE_FILE}" \
        --env-file "${ENV_FILE}" \
        logs -f --tail=100 ${service}
}

status() {
    docker compose \
        --file "${COMPOSE_FILE}" \
        --env-file "${ENV_FILE}" \
        ps
}

clean() {
    log_section "Nettoyage complet (volumes inclus)"
    log_warn "ATTENTION : ceci supprime toutes les données PostgreSQL !"
    read -p "Confirmez en tapant 'OUI' : " confirm
    if [ "${confirm}" = "OUI" ]; then
        docker compose \
            --file "${COMPOSE_FILE}" \
            --env-file "${ENV_FILE}" \
            down -v --remove-orphans
        log_info "Nettoyage terminé"
    else
        log_info "Nettoyage annulé"
    fi
}

wait_for_healthy() {
    local container="$1"
    local timeout="$2"
    local elapsed=0

    while [ "${elapsed}" -lt "${timeout}" ]; do
        local health
        health=$(docker inspect \
            --format='{{.State.Health.Status}}' \
            "${container}" 2>/dev/null || echo "absent")

        if [ "${health}" = "healthy" ]; then
            log_info "${container} est prêt ✓"
            return 0
        fi

        log_info "  ${container} : ${health} (${elapsed}s / ${timeout}s)..."
        sleep 5
        elapsed=$((elapsed + 5))
    done

    log_error "${container} n'est pas devenu healthy en ${timeout}s"
    docker logs "${container}" --tail=50
    exit 1
}

# =============================================================================
# Point d'entrée
# =============================================================================

case "${1:-start}" in
    start)   start   ;;
    stop)    stop    ;;
    restart) restart ;;
    logs)    logs "${2:-}"   ;;
    status)  status  ;;
    clean)   clean   ;;
    build-spi)        build_spi         ;;
    build-iam-compte) build_iam_compte  ;;
    *)
        echo "Usage : $0 [start|stop|restart|logs|status|clean|build-spi|build-iam-compte]"
        exit 1
        ;;
esac
