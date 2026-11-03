#!/bin/sh
# ChatL gradlew: gradle-wrapper.jar yoksa resmi Gradle deposundan indirir, sonra Gradle 8.8'i calistirir.
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
URL="https://raw.githubusercontent.com/gradle/gradle/v8.8.0/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$JAR" ]; then
  echo "gradle-wrapper.jar indiriliyor..." >&2
  mkdir -p "$APP_HOME/gradle/wrapper"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$URL" -o "$JAR" || { echo "Indirme basarisiz" >&2; exit 1; }
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$URL" -O "$JAR" || { echo "Indirme basarisiz" >&2; exit 1; }
  else
    echo "curl veya wget gerekli" >&2; exit 1
  fi
fi

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then JAVACMD="$JAVA_HOME/bin/java"; else JAVACMD=java; fi
exec "$JAVACMD" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew \
  -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
