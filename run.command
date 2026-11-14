#!/bin/sh
set -e

cd "$(dirname "$0")"
sh ./mvnw -q -DskipTests package
exec java --enable-native-access=ALL-UNNAMED -jar target/five-nights-at-gudos.jar "$@"
