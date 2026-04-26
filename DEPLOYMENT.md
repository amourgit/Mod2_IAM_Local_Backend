# Déploiement IAM Local

Guide de déploiement pour l'environnement IAM Local avec Keycloak personnalisé et PostgreSQL.

## 🚀 Déploiement rapide

### Option 1: Déploiement complet (recommandé)
```bash
./scripts/deploy-iam-local.sh deploy
```

Cette commande:
1. Construit le projet si nécessaire
2. Démarre PostgreSQL
3. Démarre Keycloak avec les champs IAM Local

### Option 2: Déploiement manuel
```bash
# 1. Démarrer PostgreSQL
./scripts/start-postgres.sh

# 2. Démarrer Keycloak
./scripts/start-keycloak.sh
```

## 📋 Commandes disponibles

### Script principal `deploy-iam-local.sh`
```bash
./scripts/deploy-iam-local.sh [commande]

Commandes:
  deploy    - Déploie l'environnement complet
  postgres  - Démarre uniquement PostgreSQL
  keycloak  - Démarre uniquement Keycloak
  build     - Construit les modules IAM Local
  stop      - Arrête tous les services
  restart   - Redémarre tous les services
  status    - Affiche le statut des services
  help      - Affiche l'aide
```

### Script PostgreSQL `start-postgres.sh`
```bash
./scripts/start-postgres.sh
```

### Script Keycloak `start-keycloak.sh`
```bash
./scripts/start-keycloak.sh [commande]

Commandes:
  (vide)    - Démarre Keycloak
  build     - Construit Keycloak avec Maven
  clean     - Nettoie l'installation
  env       - Crée/met à jour le fichier .env
  help      - Affiche l'aide
```

## ⚙️ Configuration

### Variables d'environnement
Le fichier `.env` à la racine contient la configuration:

```bash
# Base de données PostgreSQL
KC_DB=postgres
KC_DB_URL=jdbc:postgresql://localhost:5432/keycloak_iam_local
KC_DB_USERNAME=keycloak
KC_DB_PASSWORD=keycloak_secret_uob_2025

# Administrateur Keycloak
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin123
```

### Personnalisation
Pour modifier la configuration:
1. Éditer le fichier `.env` directement
2. Ou utiliser: `./scripts/start-keycloak.sh env`

## 🔗 Accès aux services

### Keycloak Admin Console
- **URL**: http://localhost:8080
- **Utilisateur**: admin
- **Mot de passe**: admin123

### Base de données PostgreSQL
- **Hôte**: localhost:5432
- **Base**: keycloak_iam_local
- **Utilisateur**: keycloak
- **Mot de passe**: keycloak_secret_uob_2025

### Commande de connexion PostgreSQL
```bash
docker exec -it iam-postgres-dev psql -U keycloak -d keycloak_iam_local
```

## 🏗️ Architecture

### Composants déployés
1. **PostgreSQL 16** (conteneur Docker)
   - Base de données `keycloak_iam_local`
   - Volume persistant pour les données

2. **Keycloak 26.6.0** (processus local)
   - Distribution personnalisée avec SPI IAM Local
   - Champs natifs: `id_compte_local`, `identifiant_national`, `statut_compte`
   - Table `compte_local` avec relation 1-N vers `user_entity`

### Fichiers créés
- `docker-compose.yml` - Configuration PostgreSQL
- `.env` - Variables d'environnement
- `quarkus/dist/target/keycloak-26.6.0/` - Distribution Keycloak
- `docker/volumes/` - Données PostgreSQL (si volumes locaux)

## 🔧 Dépannage

### Problèmes courants

1. **Port 5432 déjà utilisé**
   ```bash
   sudo lsof -i :5432
   # Arrêter l'autre service PostgreSQL
   ```

2. **Keycloak ne démarre pas**
   ```bash
   # Vérifier les logs
   cd quarkus/dist/target/keycloak-26.6.0/logs
   
   # Reconstruire
   ./scripts/deploy-iam-local.sh build
   ```

3. **PostgreSQL inaccessible**
   ```bash
   # Vérifier le conteneur
   docker ps | grep postgres
   
   # Vérifier les logs
   docker-compose logs iam-postgres-dev
   ```

### Nettoyage complet
```bash
# Arrêter tous les services
./scripts/deploy-iam-local.sh stop

# Supprimer les volumes Docker
docker-compose down -v

# Nettoyer Keycloak
./scripts/start-keycloak.sh clean

# Reconstruire depuis zéro
./scripts/deploy-iam-local.sh build
./scripts/deploy-iam-local.sh deploy
```

## 📊 Vérification du déploiement

### Vérifier les tables créées
```bash
docker exec -it iam-postgres-dev psql -U keycloak -d keycloak_iam_local -c "\dt"
```

### Vérifier les champs IAM Local
```bash
# Champs dans user_entity
docker exec -it iam-postgres-dev psql -U keycloak -d keycloak_iam_local -c "\d user_entity" | grep -E "(id_compte_local|identifiant_national|statut_compte)"

# Table compte_local
docker exec -it iam-postgres-dev psql -U keycloak -d keycloak_iam_local -c "\d compte_local"
```

### Vérifier les migrations Liquibase
```bash
docker exec -it iam-postgres-dev psql -U keycloak -d keycloak_iam_local -c "SELECT id, author, filename FROM databasechangelog WHERE filename LIKE '%iam-local%';"
```

## 🚀 En production

Pour un déploiement en production, adapter:
1. **Sécurité**: Changer les mots de passe par défaut
2. **Performance**: Configurer les ressources JVM et PostgreSQL
3. **Haute disponibilité**: Configurer le clustering Keycloak
4. **SSL/TLS**: Configurer HTTPS
5. **Monitoring**: Ajouter des métriques et logs

Exemple de configuration production dans `.env`:
```bash
# Production
KC_HOSTNAME=keycloak.entreprise.fr
KC_HTTP_ENABLED=false
KC_HTTPS_REQUIRED=true
KC_DB_URL_POOL_INITIAL_SIZE=5
KC_DB_URL_POOL_MAX_SIZE=20
KC_METRICS_ENABLED=true
KC_HEALTH_ENABLED=true
```
