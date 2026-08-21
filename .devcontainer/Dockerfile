FROM mcr.microsoft.com/devcontainers/typescript-node:22

# Install additional OS packages if needed.
# RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
#     && apt-get -y install --no-install-recommends <your-package-list-here>

# Install Junie CLI
USER node
RUN curl -fsSL https://junie.jetbrains.com/install.sh | bash

# Install OpenCode CLI
RUN curl -fsSL https://opencode.ai/install | bash

# Ensure Junie is in the PATH
ENV PATH="/home/node/.opencode/bin:/home/node/.junie/bin:${PATH}"
ENV JUNIE_EFFORT="Max"
