#!/bin/bash

# =============================================
# Script de démarrage PostgreSQL pour IAM Local
# =============================================

set -e

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Fonction pour afficher les messages
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

# Vérification de Docker et Docker Compose
check_dependencies() {
    log_info "Vérification des dépendances..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker n'est pas installé ou pas dans le PATH"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose n'est pas installé ou pas dans le PATH"
        exit 1
    fi
    
    log_success "Dépendances vérifiées"
}

# Aucun répertoire local nécessaire
setup_directories() {
    log_info "Configuration PostgreSQL sans volumes locaux..."
    log_success "Configuration terminée"
}

# Démarrage des services
start_services() {
    log_info "Démarrage des services PostgreSQL..."
    
    # Arrêt des services existants
    docker-compose down 2>/dev/null || true
    
    # Démarrage
    docker-compose up -d
    
    log_success "Services démarrés"
}

# Attente de la disponibilité de PostgreSQL
wait_for_postgres() {
    log_info "Attente de la disponibilité de PostgreSQL..."
    
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if docker exec iam-postgres-dev pg_isready -U keycloak -d keycloak_iam_local &>/dev/null; then
            log_success "PostgreSQL est prêt !"
            return 0
        fi
        
        log_info "Tentative $attempt/$max_attempts..."
        sleep 2
        ((attempt++))
    done
    
    log_error "PostgreSQL n'est pas disponible après $max_attempts tentatives"
    return 1
}

# Affichage des informations de connexion
show_connection_info() {
    log_success "=== INFORMATIONS DE CONNEXION ==="
    echo
    echo -e "${BLUE}Hôte:${NC} localhost"
    echo -e "${BLUE}Port:${NC} 5432"
    echo -e "${BLUE}Base de données:${NC} keycloak_iam_local"
    echo -e "${BLUE}Utilisateur:${NC} keycloak"
    echo -e "${BLUE}Mot de passe:${NC} keycloak_secret_uob_2025"
    echo
    echo -e "${BLUE}Commande de connexion:${NC}"
    echo "docker exec -it iam-postgres-dev psql -U keycloak -d keycloak_iam_local"
    echo
    echo -e "${BLUE}URL JDBC:${NC}"
    echo "jdbc:postgresql://localhost:5432/keycloak_iam_local"
    echo
}

# Fonction principale
main() {
    echo "============================================"
    echo "Démarrage PostgreSQL pour IAM Local"
    echo "============================================"
    echo
    
    check_dependencies
    setup_directories
    start_services
    
    if wait_for_postgres; then
        show_connection_info
        log_success "PostgreSQL est prêt à être utilisé !"
    else
        log_error "Échec du démarrage de PostgreSQL"
        echo
        echo "Pour vérifier les logs :"
        echo "docker-compose logs iam-postgres-dev"
        exit 1
    fi
}

# Gestion des signaux
trap 'log_warning "Interruption reçue"; docker-compose down; exit 1' INT TERM

# Exécution
main "$@"
