#!/bin/sh
set -eu

MAX_RETRIES=10
RETRY_COUNT=0
DB_HOST="${PAYMENTS_POSTGRES_HOST:?PAYMENTS_POSTGRES_HOST must be set}"
DB_PORT="${PAYMENTS_POSTGRES_PORT_INNER:-5432}"
DB_NAME="${PAYMENTS_POSTGRES_DB_NAME:?PAYMENTS_POSTGRES_DB_NAME must be set}"
DB_USER="${SPRING_DATASOURCE_USERNAME:?SPRING_DATASOURCE_USERNAME must be set}"
DB_PASSWORD="${SPRING_DATASOURCE_PASSWORD:?SPRING_DATASOURCE_PASSWORD must be set}"
POSTGRES_ADMIN="${POSTGRES_USER:-postgres}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD must be set}"

echo "Waiting for PostgreSQL at ${DB_HOST}:${DB_PORT}..."
until pg_isready --host="$DB_HOST" --port="$DB_PORT" --username="$POSTGRES_ADMIN" >/dev/null 2>&1; do
  RETRY_COUNT=$((RETRY_COUNT + 1))
  if [ "$RETRY_COUNT" -ge "$MAX_RETRIES" ]; then
    echo "PostgreSQL is unavailable after ${MAX_RETRIES} attempts."
    exit 1
  fi
  sleep 1
done

echo "PostgreSQL is available. Checking database and service role..."
psql_admin() {
  PGPASSWORD="$POSTGRES_PASSWORD" psql \
    --host="$DB_HOST" --port="$DB_PORT" --username="$POSTGRES_ADMIN" \
    --dbname=postgres --no-password "$@"
}

ROLE_EXISTS=$(psql_admin --tuples-only --no-align \
  --command="SELECT 1 FROM pg_roles WHERE rolname = '${DB_USER}';")
if [ "$ROLE_EXISTS" != "1" ]; then
  psql_admin --command="CREATE ROLE \"${DB_USER}\" LOGIN PASSWORD '${DB_PASSWORD}';"
fi

DATABASE_EXISTS=$(psql_admin --tuples-only --no-align \
  --command="SELECT 1 FROM pg_database WHERE datname = '${DB_NAME}';")
if [ "$DATABASE_EXISTS" != "1" ]; then
  psql_admin --command="CREATE DATABASE \"${DB_NAME}\" OWNER \"${DB_USER}\";"
fi

echo "Verifying service database connection..."
PGPASSWORD="$DB_PASSWORD" psql --host="$DB_HOST" --port="$DB_PORT" \
  --username="$DB_USER" --dbname="$DB_NAME" --no-password --command="SELECT 1;" >/dev/null

run_liquibase() {
  liquibase \
    --search-path=/app/src/main/resources \
    --classpath=/opt/payments/libs/postgresql-42.7.5.jar \
    --driver=org.postgresql.Driver \
    --url="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
    --username="$DB_USER" \
    --password="$DB_PASSWORD" \
    --changeLogFile=db/changelog/db.changelog.yaml \
    "$@"
}

echo "Validating Liquibase changelog..."
run_liquibase validate

echo "Applying Liquibase migrations..."
run_liquibase update

echo "Starting payments-service..."
exec "$@"
