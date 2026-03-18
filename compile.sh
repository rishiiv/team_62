#!/bin/bash

# Compile script for JavaFX application
# Usage: ./compile.sh [javafx-path]
# If javafx-path is not provided, you'll need to set JAVA_FX_PATH environment variable
# or update the path in this script

JAVAFX_PATH=${1:-${JAVA_FX_PATH:-/Users/adavi/development/javafx-sdk-25.0.1/lib}}

echo "Compiling JavaFX application with MVC structure..."
echo "Using JavaFX path: $JAVAFX_PATH"

if [ ! -d "$JAVAFX_PATH" ]; then
    echo "Warning: JavaFX path not found at $JAVAFX_PATH"
    echo "Please download JavaFX SDK from https://openjfx.io/ or set JAVA_FX_PATH environment variable"
    echo "Example: export JAVA_FX_PATH=/path/to/javafx-sdk/lib"
    exit 1
fi

javac --module-path "$JAVAFX_PATH" \
      --add-modules javafx.controls,javafx.fxml \
      -d build/classes \
      src/main/java/com/team62/model/*.java \
      src/main/java/com/team62/db/*.java \
      src/main/java/com/team62/view/*.java \
      src/main/java/com/team62/controller/*.java \
      src/main/java/com/team62/*.java

if [ $? -eq 0 ]; then
    echo "Compilation complete. Run with: ./run.sh"
else
    echo "Compilation failed!"
    exit 1
fi