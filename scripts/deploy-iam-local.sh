#!/bin/bash

# =============================================
# Script de déploiement complet IAM Local
# =============================================

set -e

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

show_usage() {
    echo "Usage: $0 [postgres|keycloak|build|stop|restart|status|help]"
    echo
    echo "Commandes:"
    echo "  postgres  - Démarre uniquement PostgreSQL"
    echo "  keycloak  - Démarre uniquement Keycloak"
    echo "  build     - Construit les modules IAM Local"
    echo "  deploy    - Déploie l'environnement complet (PostgreSQL + Keycloak)"
    echo "  stop      - Arrête tous les services"
    echo "  restart   - Redémarre tous les services"
    echo "  status    - Affiche le statut des services"
    echo "  help      - Affiche cette aide"
    echo
    echo "Par défaut: Déploie l'environnement complet"
}

build_project() {
    log_info "Construction du projet IAM Local..."
    
    if [ ! -f "mvnw" ]; then
        log_error "Maven wrapper non trouvé. Veuillez exécuter depuis la racine du projet."
        exit 1
    fi
    
    log_info "Build complet du projet..."
    ./mvnw clean package -am -DskipTests
    
    log_info "Build de la distribution Keycloak..."
    ./mvnw clean package -pl quarkus/dist -am -DskipTests
    
    log_success "Construction terminée"
}

start_postgres() {
    log_info "Démarrage de PostgreSQL..."
    
    if [ -f "scripts/start-postgres.sh" ]; then
        ./scripts/start-postgres.sh
    else
        log_error "Script start-postgres.sh non trouvé"
        exit 1
    fi
}

start_keycloak() {
    log_info "Démarrage de Keycloak..."
    
    if [ -f "scripts/start-keycloak.sh" ]; then
        ./scripts/start-keycloak.sh
    else
        log_error "Script start-keycloak.sh non trouvé"
        exit 1
    fi
}

stop_services() {
    log_info "Arrêt des services..."
    
    # Arrêt de Keycloak
    if pgrep -f "kc.sh" > /dev/null; then
        log_info "Arrêt de Keycloak..."
        pkill -f "kc.sh" || true
    fi
    
    # Arrêt de PostgreSQL via Docker Compose
    if [ -f "docker-compose.yml" ]; then
        log_info "Arrêt de PostgreSQL..."
        docker-compose down
    fi
    
    log_success "Services arrêtés"
}

restart_services() {
    log_info "Redémarrage des services..."
    stop_services
    sleep 2
    deploy_full
}

deploy_full() {
    log_info "Déploiement de l'environnement IAM Local..."
    
    # 1. Construction si nécessaire
    if [ ! -f "quarkus/dist/target/keycloak-26.6.0.tar.gz" ]; then
        log_info "Construction requise..."
        build_project
    fi
    
    # 2. Démarrage PostgreSQL
    start_postgres
    
    # 3. Attente de PostgreSQL
    log_info "Attente de PostgreSQL..."
    sleep 5
    
    # 4. Démarrage Keycloak
    start_keycloak
}

show_status() {
    echo "============================================"
    echo "Statut des services IAM Local"
    echo "============================================"
    echo
    
    # Statut PostgreSQL
    echo -e "${BLUE}PostgreSQL:${NC}"
    if docker ps --format "table {{.Names}}\t{{.Status}}" | grep -q "iam-postgres-dev"; then
        docker ps --format "table {{.Names}}\t{{.Status}}" | grep "iam-postgres-dev"
    else
        echo -e "${RED}Non démarré${NC}"
    fi
    echo
    
    # Statut Keycloak
    echo -e "${BLUE}Keycloak:${NC}"
    if pgrep -f "kc.sh" > /dev/null; then
        echo -e "${GREEN}En cours d'exécution${NC}"
        if command -v curl &> /dev/null; then
            if curl -s http://localhost:8080 > /dev/null; then
                echo -e "${GREEN}Accessible sur http://localhost:8080${NC}"
            else
                echo -e "${YELLOW}En cours de démarrage...${NC}"
            fi
        fi
    else
        echo -e "${RED}Non démarré${NC}"
    fi
    echo
    
    # Informations de connexion
    echo -e "${BLUE}Informations de connexion:${NC}"
    if [ -f ".env" ]; then
        source .env
        echo -e "Base de données: ${KC_DB_URL:-N/A}"
        echo -e "Admin Keycloak: ${KEYCLOAK_ADMIN:-admin}"
    else
        echo -e "${YELLOW}Fichier .env non trouvé${NC}"
    fi
}

# Gestion des arguments
case "${1:-deploy}" in
    "postgres")
        start_postgres
        ;;
    "keycloak")
        start_keycloak
        ;;
    "build")
        build_project
        ;;
    "deploy")
        deploy_full
        ;;
    "stop")
        stop_services
        ;;
    "restart")
        restart_services
        ;;
    "status")
        show_status
        ;;
    "help"|"-h"|"--help")
        show_usage
        ;;
    *)
        log_error "Commande inconnue: $1"
        show_usage
        exit 1
        ;;
esac
