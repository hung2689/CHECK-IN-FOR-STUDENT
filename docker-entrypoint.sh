#!/bin/sh
# Convert DATABASE_URL (standard cloud format) to JDBC format for Spring Boot
if [ -n "$DATABASE_URL" ]; then
  case "$DATABASE_URL" in
    postgres://*)     SPRING_DATASOURCE_URL="jdbc:postgresql://${DATABASE_URL#postgres://}" ;;
    postgresql://*)   SPRING_DATASOURCE_URL="jdbc:${DATABASE_URL}" ;;
    jdbc:*)           SPRING_DATASOURCE_URL="$DATABASE_URL" ;;
    *)                SPRING_DATASOURCE_URL="$DATABASE_URL" ;;
  esac
  export SPRING_DATASOURCE_URL
  echo "Converted DATABASE_URL -> SPRING_DATASOURCE_URL (jdbc:postgresql://...)"
fi

exec java -Djava.security.egd=file:/dev/./urandom -jar app.jar "$@"
