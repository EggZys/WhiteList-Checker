#!/bin/sh
# Gradle wrapper script - download gradle-wrapper.jar if missing
GRADLE_APP_NAME="Gradle"
DIRNAME=$(dirname "$0")
APP_BASE_NAME=$(basename "$0")
APP_HOME=$(cd "$DIRNAME" && pwd -P)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ ! -f "$CLASSPATH" ]; then
    echo "Downloading gradle-wrapper.jar..."
    mkdir -p "$APP_HOME/gradle/wrapper"
    curl -sL "https://services.gradle.org/distributions/gradle-8.5-bin.zip" -o /tmp/gradle-8.5-bin.zip
    unzip -qo /tmp/gradle-8.5-bin.zip -d /tmp/gradle-temp
    cp /tmp/gradle-temp/gradle-8.5/lib/gradle-wrapper-8.5.jar "$CLASSPATH" 2>/dev/null || true
    rm -rf /tmp/gradle-temp /tmp/gradle-8.5-bin.zip
fi

exec java $JAVA_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
