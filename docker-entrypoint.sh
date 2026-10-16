#!/bin/sh
set -e

# If DATABASE_URL is set, parse it into standard Spring Boot JDBC properties
if [ -n "$DATABASE_URL" ]; then
  # Strip jdbc: prefix if present
  case "$DATABASE_URL" in
    jdbc:*)
      CLEAN="${DATABASE_URL#jdbc:}"
      ;;
    *)
      CLEAN="$DATABASE_URL"
      ;;
  esac

  # Strip postgres:// or postgresql://
  STRIPPED=$(echo "$CLEAN" | sed 's|^postgres://||' | sed 's|^postgresql://||')

  case "$STRIPPED" in
    *@*)
      # Contains credentials: user:pass@host/path
      USERINFO=$(echo "$STRIPPED" | sed 's|@.*||')
      HOST_AND_PATH=$(echo "$STRIPPED" | sed 's|^[^@]*@||')
      DB_USER=$(echo "$USERINFO" | cut -d: -f1)
      DB_PASS=$(echo "$USERINFO" | cut -d: -f2-)

      export SPRING_DATASOURCE_URL="jdbc:postgresql://${HOST_AND_PATH}"
      export SPRING_DATASOURCE_USERNAME="$DB_USER"
      export SPRING_DATASOURCE_PASSWORD="$DB_PASS"
      ;;
    *)
      # No credentials in URL
      export SPRING_DATASOURCE_URL="jdbc:postgresql://${STRIPPED}"
      ;;
  esac

  echo "==> Converted database connection to: $SPRING_DATASOURCE_URL"
  if [ -n "$SPRING_DATASOURCE_USERNAME" ]; then
    echo "==> Converted database user: $SPRING_DATASOURCE_USERNAME"
  fi
fi

exec java -Djava.security.egd=file:/dev/./urandom -jar app.jar "$@"
