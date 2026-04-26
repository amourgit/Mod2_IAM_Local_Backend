-- =============================================================================
-- Initialisation PostgreSQL pour IAM Local UOB v0.0.0 - Base unique
-- NOTE : On utilise la locale C (compatible alpine) au lieu de fr_FR.UTF-8
--        qui n'est pas disponible dans postgres:alpine sans build custom.
--        L'encodage UTF8 est conservé — suffisant pour les données françaises.
-- =============================================================================

-- ── Base unique Keycloak + IAM Local ───────────────────────────────────────
CREATE DATABASE keycloak_iam_local
    WITH ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TEMPLATE = template0;

CREATE USER keycloak WITH PASSWORD 'keycloak_secret_uob_2025';
GRANT ALL PRIVILEGES ON DATABASE keycloak_iam_local TO keycloak;

\connect keycloak_iam_local
GRANT ALL ON SCHEMA public TO keycloak;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO keycloak;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO keycloak;

-- Extension UUID pour les modules IAM
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\echo '=== Base unique keycloak_iam_local créée avec succès ==='
\echo '=== Prête pour Keycloak 26.6.0 + Modules IAM natifs ==='
\l
