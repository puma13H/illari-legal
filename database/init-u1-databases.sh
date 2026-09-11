#!/bin/sh
set -eu

for db in illari_servicio illari_orden illari_pago; do
  exists="$(psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -tAc "SELECT 1 FROM pg_database WHERE datname='$db'")"
  if [ "$exists" != "1" ]; then
    echo "Creando base $db"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -c "CREATE DATABASE \"$db\""
  fi
done
