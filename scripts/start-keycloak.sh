#!/bin/bash

# =============================================
# Script de déploiement Keycloak pour IAM Local
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

# Configuration par défaut
DEFAULT_KC_DB="postgres"
DEFAULT_KC_DB_URL="jdbc:postgresql://localhost:5432/keycloak_iam_local"
DEFAULT_KC_DB_USERNAME="keycloak"
DEFAULT_KC_DB_PASSWORD="keycloak_secret_uob_2025"
DEFAULT_KEYCLOAK_ADMIN="admin"
DEFAULT_KEYCLOAK_ADMIN_PASSWORD="admin123"

# Fichier .env
ENV_FILE=".env"

# Chargement des variables depuis .env
load_env() {
    if [ -f "$ENV_FILE" ]; then
        log_info "Chargement des variables depuis $ENV_FILE"
        export $(grep -v '^#' $ENV_FILE | xargs)
    else
        log_warning "Fichier $ENV_FILE non trouvé, utilisation des valeurs par défaut"
        create_default_env
    fi
}

# Création du fichier .env par défaut
create_default_env() {
    log_info "Création du fichier $ENV_FILE avec les valeurs par défaut..."
    
    cat > "$ENV_FILE" << EOF
# =============================================
# Configuration Keycloak IAM Local
# =============================================

# Base de données PostgreSQL
KC_DB=${DEFAULT_KC_DB}
KC_DB_URL=${DEFAULT_KC_DB_URL}
KC_DB_USERNAME=${DEFAULT_KC_DB_USERNAME}
KC_DB_PASSWORD=${DEFAULT_KC_DB_PASSWORD}

# Administrateur Keycloak
KEYCLOAK_ADMIN=${DEFAULT_KEYCLOAK_ADMIN}
KEYCLOAK_ADMIN_PASSWORD=${DEFAULT_KEYCLOAK_ADMIN_PASSWORD}

# Configuration supplémentaire (optionnel)
# KC_HOSTNAME=localhost
# KC_HTTP_ENABLED=true
# KC_METRICS_ENABLED=true
EOF
    
    log_success "Fichier $ENV_FILE créé"
}

# Vérification des dépendances
check_dependencies() {
    log_info "Vérification des dépendances..."
    
    if ! command -v java &> /dev/null; then
        log_error "Java n'est pas installé ou pas dans le PATH"
        exit 1
    fi
    
    if [ ! -f "quarkus/dist/target/keycloak-26.6.0.tar.gz" ]; then
        log_error "Archive Keycloak non trouvée: quarkus/dist/target/keycloak-26.6.0.tar.gz"
        log_error "Veuillez d'abord construire Keycloak avec: ./mvnw package -am -DskipTests"
        exit 1
    fi
    
    log_success "Dépendances vérifiées"
}

# Préparation de l'environnement Keycloak
prepare_keycloak() {
    log_info "Préparation de l'environnement Keycloak..."
    
    # Nettoyage de l'ancienne installation
    if [ -d "quarkus/dist/target/keycloak-26.6.0" ]; then
        log_info "Suppression de l'ancienne installation..."
        rm -rf quarkus/dist/target/keycloak-26.6.0
    fi
    
    # Extraction de l'archive
    log_info "Extraction de l'archive Keycloak..."
    cd quarkus/dist/target
    tar -xzf keycloak-26.6.0.tar.gz
    cd ../../..
    
    # Copie du SPI si disponible
    if [ -f "iam-compte-keycloak-spi/target/iam-compte-keycloak-spi.jar" ]; then
        log_info "Installation du SPI IAM Local..."
        cp iam-compte-keycloak-spi/target/iam-compte-keycloak-spi.jar quarkus/dist/target/keycloak-26.6.0/providers/
        log_success "SPI IAM Local installé"
    else
        log_warning "SPI IAM Local non trouvé, utilisation de Keycloak standard"
    fi
    
    log_success "Préparation terminée"
}

# Vérification de la connexion PostgreSQL
check_postgres() {
    log_info "Vérification de la connexion PostgreSQL..."
    
    # Extraction des infos de connexion depuis KC_DB_URL
    DB_HOST=$(echo $KC_DB_URL | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
    DB_PORT=$(echo $KC_DB_URL | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
    DB_NAME=$(echo $KC_DB_URL | sed -n 's/.*\/\([^?]*\).*/\1/p')
    
    if command -v pg_isready &> /dev/null; then
        if pg_isready -h $DB_HOST -p $DB_PORT -d $DB_NAME &> /dev/null; then
            log_success "PostgreSQL est accessible"
        else
            log_error "PostgreSQL n'est pas accessible sur $DB_HOST:$DB_PORT/$DB_NAME"
            log_error "Vérifiez que PostgreSQL est démarré avec: docker-compose up -d"
            exit 1
        fi
    else
        log_warning "pg_isready non disponible, saut de la vérification PostgreSQL"
    fi
}

# Démarrage de Keycloak
start_keycloak() {
    log_info "Démarrage de Keycloak..."
    
    cd quarkus/dist/target/keycloak-26.6.0
    
    # Configuration de l'environnement
    export KC_DB="${KC_DB:-$DEFAULT_KC_DB}"
    export KC_DB_URL="${KC_DB_URL:-$DEFAULT_KC_DB_URL}"
    export KC_DB_USERNAME="${KC_DB_USERNAME:-$DEFAULT_KC_DB_USERNAME}"
    export KC_DB_PASSWORD="${KC_DB_PASSWORD:-$DEFAULT_KC_DB_PASSWORD}"
    export KEYCLOAK_ADMIN="${KEYCLOAK_ADMIN:-$DEFAULT_KEYCLOAK_ADMIN}"
    export KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-$DEFAULT_KEYCLOAK_ADMIN_PASSWORD}"
    
    # Affichage de la configuration
    log_success "=== CONFIGURATION KEYCLOAK ==="
    echo -e "${BLUE}Base de données:${NC} $KC_DB"
    echo -e "${BLUE}URL:${NC} $KC_DB_URL"
    echo -e "${BLUE}Utilisateur:${NC} $KC_DB_USERNAME"
    echo -e "${BLUE}Admin:${NC} $KEYCLOAK_ADMIN"
    echo -e "${BLUE}Admin Password:${NC} $KEYCLOAK_ADMIN_PASSWORD"
    echo
    
    # Démarrage
    log_info "Lancement de Keycloak en mode développement..."
    ./bin/kc.sh start-dev
}

# Affichage des informations d'accès
show_access_info() {
    echo
    log_success "=== ACCÈS KEYCLOAK ==="
    echo -e "${BLUE}Console Admin:${NC} http://localhost:8080"
    echo -e "${BLUE}Utilisateur:${NC} ${KEYCLOAK_ADMIN:-$DEFAULT_KEYCLOAK_ADMIN}"
    echo -e "${BLUE}Mot de passe:${NC} ${KEYCLOAK_ADMIN_PASSWORD:-$DEFAULT_KEYCLOAK_ADMIN_PASSWORD}"
    echo
    echo -e "${BLUE}API REST:${NC} http://localhost:8080/realms/master/protocol/openid-connect/token"
    echo
}

# Fonction principale
main() {
    echo "============================================"
    echo "Déploiement Keycloak pour IAM Local"
    echo "============================================"
    echo
    
    load_env
    check_dependencies
    prepare_keycloak
    check_postgres
    
    # Gestion du signal SIGINT pour arrêter proprement
    trap 'log_warning "Interruption reçue"; exit 0' INT TERM
    
    if start_keycloak; then
        show_access_info
    else
        log_error "Échec du démarrage de Keycloak"
        exit 1
    fi
}

# Gestion des arguments
case "${1:-}" in
    "build")
        log_info "Construction de Keycloak..."
        ./mvnw package -am -DskipTests
        ;;
    "clean")
        log_info "Nettoyage de l'installation..."
        rm -rf quarkus/dist/target/keycloak-26.6.0
        log_success "Nettoyage terminé"
        ;;
    "env")
        create_default_env
        log_info "Fichier .env créé/ mis à jour"
        ;;
    "help"|"-h"|"--help")
        echo "Usage: $0 [build|clean|env|help]"
        echo
        echo "Commandes:"
        echo "  build  - Construit Keycloak avec Maven"
        echo "  clean  - Nettoie l'installation Keycloak"
        echo "  env    - Crée/met à jour le fichier .env"
        echo "  help   - Affiche cette aide"
        echo
        echo "Par défaut: Démarre Keycloak"
        ;;
    "")
        # Comportement par défaut
        main
        ;;
    *)
        log_error "Commande inconnue: $1"
        echo "Utilisez '$0 help' pour voir les commandes disponibles"
        exit 1
        ;;
esac
