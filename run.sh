#!/bin/bash

# Run script for JavaFX application
# Usage: ./run.sh [javafx-path]
# If javafx-path is not provided, uses JAVA_FX_PATH environment variable
# or defaults to /path/to/javafx-sdk/lib (update this if needed)

JAVAFX_PATH=${1:-${JAVA_FX_PATH:-/Users/adavi/development/javafx-sdk-25.0.1/lib}}

# Path to the PostgreSQL JDBC driver JAR. Prefer lib/postgresql-*.jar if present.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -z "${PG_JDBC_JAR}" ] && [ -d "$SCRIPT_DIR/lib" ]; then
  PG_JDBC_JAR="$(find "$SCRIPT_DIR/lib" -maxdepth 1 -name 'postgresql-*.jar' 2>/dev/null | head -1)"
fi
PG_JDBC_JAR=${PG_JDBC_JAR:-/Users/adavi/development/postgresql-42.7.10.jar}

echo "Running JavaFX application..."
echo "Using JavaFX path: $JAVAFX_PATH"

if [ ! -d "$JAVAFX_PATH" ]; then
    echo "Error: JavaFX path not found at $JAVAFX_PATH"
    echo "Please download JavaFX SDK from https://openjfx.io/ or set JAVA_FX_PATH environment variable"
    echo "Example: export JAVA_FX_PATH=/path/to/javafx-sdk/lib"
    exit 1
fi

java --module-path "$JAVAFX_PATH" \
     --add-modules javafx.controls,javafx.fxml \
     --enable-native-access=javafx.graphics \
     -cp "build/classes:$PG_JDBC_JAR" \
     com.team62.Main
