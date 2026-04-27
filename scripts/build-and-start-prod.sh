#!/usr/bin/env bash
# =============================================================================
# IAM Local UOB v0.0.0 — Build Production + Démarrage
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

DIST_TARGET="${ROOT}/quarkus/dist/target"
ARCHIVE_ZIP="${DIST_TARGET}/keycloak-26.6.0.zip"
ARCHIVE_TGZ="${DIST_TARGET}/keycloak-26.6.0.tar.gz"
DIST_DIR="${DIST_TARGET}/keycloak-26.6.0"
KC="${DIST_DIR}/bin/kc.sh"
PID_FILE="${ROOT}/keycloak.pid"
LOG_FILE="${ROOT}/keycloak.log"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info()    { echo -e "${GREEN}[IAM-PROD]${NC} $1"; }
log_warn()    { echo -e "${YELLOW}[IAM-PROD]${NC} $1"; }
log_error()   { echo -e "${RED}[IAM-PROD]${NC} $1"; }
log_section() { echo -e "\n${BLUE}====== $1 ======${NC}"; }

# =============================================================================
charger_env() {
    local env_file="${ROOT}/.env"
    if [ -f "${env_file}" ]; then
        set -a
        source <(grep -v '^#' "${env_file}" | grep -v '^$')
        set +a
    fi
    export KC_DB_PASSWORD="${POSTGRES_KEYCLOAK_PASSWORD:-keycloak_secret_uob_2025}"
    export KC_DB_URL_HOST="${POSTGRES_HOST:-localhost}"
    export KC_DB_URL_PORT="${POSTGRES_PORT:-5432}"
    export KC_DB_URL_DATABASE="${POSTGRES_KEYCLOAK_DB:-keycloak_iam_local}"
    export KC_DB_USERNAME="${POSTGRES_KEYCLOAK_USER:-keycloak}"
    
    # Désactiver explicitement la création automatique d'admin
    # Supprime les variables d'environnement externes
    unset KEYCLOAK_ADMIN KEYCLOAK_ADMIN_PASSWORD
    unset KC_BOOTSTRAP_ADMIN_USERNAME KC_BOOTSTRAP_ADMIN_PASSWORD
}

# =============================================================================
build_maven() {
    log_section "Build Maven complet"
    cd "${ROOT}"

    log_info "Compilation de tous les modules (-DskipTests)..."
    mvn clean install \
        -DskipTests \
        -DskipITs \
        -Dskip.npm=true \
        -Dmaven.test.skip=true \
        --no-transfer-progress \
        -q

    log_info "Build Maven terminé"
    extraire_archive
}

# =============================================================================
extraire_archive() {
    log_section "Extraction de la distribution"

    # Supprimer l'ancienne extraction si elle existe
    if [ -d "${DIST_DIR}" ]; then
        log_info "Suppression de l'ancienne distribution extraite..."
        rm -rf "${DIST_DIR}"
    fi

    # Choisir zip ou tar.gz selon ce qui est disponible
    if [ -f "${ARCHIVE_ZIP}" ]; then
        log_info "Extraction de ${ARCHIVE_ZIP##*/}..."
        cd "${DIST_TARGET}"
        unzip -q "${ARCHIVE_ZIP}"
        log_info "Archive ZIP extraite"

    elif [ -f "${ARCHIVE_TGZ}" ]; then
        log_info "Extraction de ${ARCHIVE_TGZ##*/}..."
        cd "${DIST_TARGET}"
        tar -xzf "${ARCHIVE_TGZ}"
        log_info "Archive TAR.GZ extraite"

    else
        log_error "Aucune archive trouvée dans ${DIST_TARGET}"
        log_error "Fichiers présents :"
        ls -lh "${DIST_TARGET}" 2>/dev/null || true
        exit 1
    fi

    # Rendre kc.sh exécutable
    chmod +x "${KC}"
    log_info "Distribution disponible : ${DIST_DIR}"
    log_info "kc.sh : ${KC}"
}

# =============================================================================
build_quarkus() {
    log_section "Build Quarkus optimisé (mode production)"

    if [ ! -f "${KC}" ]; then
        log_error "Distribution absente — lancez d'abord : $0 build"
        exit 1
    fi

    log_info "Lancement de kc.sh build..."
    "${KC}" build \
        --db=postgres \
        --features=token-exchange,admin-fine-grained-authz \
        --health-enabled=true \
        --metrics-enabled=true \
        --cache=local

    log_info "Build Quarkus optimisé terminé"
}

# =============================================================================
start_db() {
    log_section "Démarrage de la base de données PostgreSQL"
    
    local compose_file="${ROOT}/docker-compose-iam.yml"
    
    if [ ! -f "${compose_file}" ]; then
        log_error "Fichier Docker Compose introuvable : ${compose_file}"
        exit 1
    fi
    
    # Vérifier si les services existent déjà
    if docker-compose -f "${compose_file}" ps -q | grep -q .; then
        log_warn "Services IAM déjà en cours d'exécution."
        read -p "Voulez-vous recréer la base de données ? [y/N] : " -r
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            clean_db
        else
            log_info "Utilisation des services existants."
            return 0
        fi
    fi
    
    log_info "Démarrage des services IAM..."
    cd "${ROOT}"
    docker-compose -f "${compose_file}" up -d
    
    log_info "Attente de disponibilité de PostgreSQL..."
    local max=60 elapsed=0
    while [ "${elapsed}" -lt "${max}" ]; do
        if docker-compose -f "${compose_file}" exec -T postgres pg_isready -U keycloak -d keycloak_iam_local >/dev/null 2>&1; then
            log_info "✓ PostgreSQL est prêt !"
            return 0
        fi
        sleep 2; elapsed=$((elapsed + 2))
        log_info "  ... ${elapsed}s"
    done
    
    log_error "Timeout : PostgreSQL n'est pas disponible"
    exit 1
}

# =============================================================================
clean_db() {
    log_section "Nettoyage complet de l'environnement IAM"
    
    local compose_file="${ROOT}/docker-compose-iam.yml"
    
    if [ ! -f "${compose_file}" ]; then
        log_warn "Fichier Docker Compose introuvable : ${compose_file}"
        return 0
    fi
    
    cd "${ROOT}"
    
    log_info "Arrêt et suppression des services..."
    docker-compose -f "${compose_file}" down -v --remove-orphans 2>/dev/null || true
    
    log_info "Suppression des volumes persistants..."
    docker volume rm iam_postgres_data 2>/dev/null || true
    docker volume rm iam_postgres_logs 2>/dev/null || true
    
    log_info "Suppression des réseaux orphelins..."
    docker network prune -f 2>/dev/null || true
    
    log_info "Nettoyage terminé"
}

# =============================================================================
start() {
    log_section "Démarrage Keycloak IAM Local UOB v0.0.0"

    if [ ! -f "${KC}" ]; then
        log_error "Distribution absente — lancez d'abord : $0 build"
        exit 1
    fi

    if [ -f "${PID_FILE}" ]; then
        local pid; pid=$(cat "${PID_FILE}")
        if kill -0 "${pid}" 2>/dev/null; then
            log_warn "Keycloak tourne déjà (PID=${pid}) — lancez '$0 stop' d'abord"
            exit 1
        else
            rm -f "${PID_FILE}"
        fi
    fi

    # Démarrer la base de données
    start_db

    charger_env

    log_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log_info "  URL         : http://localhost:8080"
    log_info "  Admin       : http://localhost:8080/admin"
    log_info "  Health      : http://localhost:8080/health/ready"
    log_info "  PostgreSQL  : localhost:5432/keycloak_iam_local"
    log_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log_warn "  PREMIER DÉMARRAGE : ouvrez http://localhost:8080"
    log_warn "  pour créer votre compte admin via le navigateur."
    log_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    "${KC}" start --optimized 2>&1 | tee "${LOG_FILE}" &
    local pid=$!
    echo "${pid}" > "${PID_FILE}"

    log_info "Keycloak démarré (PID=${pid}) — logs : ${LOG_FILE}"
    log_info "Attente de disponibilité..."

    local max=120 elapsed=0
    while [ "${elapsed}" -lt "${max}" ]; do
        if curl -sf http://localhost:8080/health/ready > /dev/null 2>&1; then
            log_info "✓ Keycloak est prêt ! → http://localhost:8080"
            return 0
        fi
        if ! kill -0 "${pid}" 2>/dev/null; then
            log_error "Keycloak s'est arrêté — voir : cat ${LOG_FILE}"
            rm -f "${PID_FILE}"; exit 1
        fi
        sleep 3; elapsed=$((elapsed + 3))
        log_info "  ... ${elapsed}s"
    done
    log_warn "Timeout — Keycloak tourne, vérifiez : tail -f ${LOG_FILE}"
}

# =============================================================================
stop() {
    log_section "Arrêt de Keycloak"
    if [ -f "${PID_FILE}" ]; then
        local pid; pid=$(cat "${PID_FILE}")
        if kill -0 "${pid}" 2>/dev/null; then
            kill -SIGTERM "${pid}"
            local w=0
            while kill -0 "${pid}" 2>/dev/null && [ "${w}" -lt 30 ]; do
                sleep 1; w=$((w+1))
            done
            kill -0 "${pid}" 2>/dev/null && kill -SIGKILL "${pid}" 2>/dev/null || true
        fi
        rm -f "${PID_FILE}"
        log_info "Keycloak arrêté"
    else
        pkill -f "kc.sh start" 2>/dev/null || true
        log_info "Processus kc.sh terminés"
    fi
}

# =============================================================================
rebuild_spi() {
    log_section "Rebuild SPI + re-extraction + rebuild Quarkus"
    cd "${ROOT}"

    log_info "Rebuild iam-compte-keycloak-spi..."
    mvn clean package \
        -pl iam-compte-keycloak-spi \
        -am \
        -DskipTests -q

    # Le SPI est déclaré comme dépendance dans quarkus/dist/pom.xml
    # → rebuild de la dist pour intégrer le nouveau JAR
    log_info "Rebuild quarkus/dist pour intégrer le nouveau SPI..."
    mvn clean package \
        -pl quarkus/dist \
        -am \
        -DskipTests -q

    extraire_archive
    build_quarkus
    log_info "SPI déployé et Quarkus optimisé"
}

logs()   { [ -f "${LOG_FILE}" ] && tail -f "${LOG_FILE}" || log_warn "Pas de logs"; }
status() {
    if [ -f "${PID_FILE}" ]; then
        local pid; pid=$(cat "${PID_FILE}")
        kill -0 "${pid}" 2>/dev/null \
            && log_info "En cours (PID=${pid})" \
            || log_warn "PID présent mais processus mort"
    else
        log_warn "Keycloak non démarré"
    fi
    curl -sf http://localhost:8080/health 2>/dev/null | python3 -m json.tool || true
}

# =============================================================================
case "${1:-help}" in
    build)
        charger_env
        build_maven      # compile tout + produit l'archive ZIP
        build_quarkus    # optimise avec kc.sh build
        log_section "Prêt — lancez : $0 start"
        ;;
    rebuild)
        rebuild_spi
        ;;
    extract)
        extraire_archive
        ;;
    start-db)
        start_db
        ;;
    clean-db)
        clean_db
        ;;
    start)    start   ;;
    stop)     stop    ;;
    restart)  stop; sleep 2; start ;;
    logs)     logs    ;;
    status)   status  ;;
    help|*)
        echo ""
        echo "  IAM Local UOB v0.0.0"
        echo ""
        echo "  build     → Build Maven complet + extraction + optimisation Quarkus"
        echo "  rebuild   → Rebuild SPI + re-extraction + re-optimisation Quarkus"
        echo "  extract   → Extraire l'archive ZIP/TGZ uniquement"
        echo "  start-db  → Démarrer uniquement PostgreSQL (avec confirmation si déjà existant)"
        echo "  clean-db  → Nettoyer complètement l'environnement Docker (volumes, réseaux)"
        echo "  start     → Démarrer Keycloak + PostgreSQL automatiquement"
        echo "  stop      → Arrêter Keycloak uniquement"
        echo "  restart   → Redémarrer Keycloak"
        echo "  logs      → Suivre les logs Keycloak"
        echo "  status    → Statut + health check"
        echo ""
        echo "  Workflow complet :"
        echo "    1. $0 build"
        echo "    2. $0 start"
        echo "    3. http://localhost:8080 → créer l'admin"
        echo ""
        echo "  Gestion DB séparée :"
        echo "    $0 start-db   → Démarrer PostgreSQL seul"
        echo "    $0 clean-db   → Nettoyer tout l'environnement Docker"
        echo ""
        ;;
esac
