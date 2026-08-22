FROM mcr.microsoft.com/devcontainers/java:1-21-bookworm

USER root
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
RUN mkdir -p "${ANDROID_SDK_ROOT}/cmdline-tools" \
    && curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" \
        -o /tmp/android-command-line-tools.zip \
    && unzip -q /tmp/android-command-line-tools.zip -d "${ANDROID_SDK_ROOT}/cmdline-tools" \
    && mv "${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools" "${ANDROID_SDK_ROOT}/cmdline-tools/latest" \
    && yes | "${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager" \
        --sdk_root="${ANDROID_SDK_ROOT}" --licenses >/dev/null \
    && "${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager" \
        --sdk_root="${ANDROID_SDK_ROOT}" \
        "platform-tools" "platforms;android-35" "build-tools;34.0.0" "build-tools;35.0.0" \
    && rm -f /tmp/android-command-line-tools.zip \
    && chown -R vscode:vscode "${ANDROID_SDK_ROOT}"

# Install Junie CLI
USER vscode
RUN curl -fsSL https://junie.jetbrains.com/install.sh | bash

# Install OpenCode CLI
RUN curl -fsSL https://opencode.ai/install | bash

# Ensure Gradle, Junie, and OpenCode are in the PATH
ENV PATH="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:/workspaces/snake/gradle/bin:/home/vscode/.opencode/bin:/home/vscode/.junie/bin:${PATH}"
ENV JUNIE_EFFORT="Max"
