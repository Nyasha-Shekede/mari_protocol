# Android build container for Mari App
# Uses a prebuilt Android SDK + JDK image so you don't need Java/SDK on the host.

FROM ghcr.io/cirruslabs/android-sdk:34

# Set working directory inside the container
WORKDIR /workspace

# Gradle will use this directory for its cache (persists only per-container run)
ENV GRADLE_USER_HOME=/workspace/.gradle

# Ensure all Java processes (Gradle, Kotlin daemon, kapt workers) get the required
# module exports/opens so kapt can access javac internals on modular JDKs.
ENV JAVA_TOOL_OPTIONS="\
  --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED"

# Default command just prints help; actual build command is provided by Makefile
CMD ["./gradlew", "-p", ".", ":app:assembleDebug"]
