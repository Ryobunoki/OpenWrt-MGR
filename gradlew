#!/usr/bin/env sh

DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

# Use the Gradle wrapper jar from the repo.
java -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
