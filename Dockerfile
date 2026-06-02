# Dockerfile for Hybrid Selenium Framework
# Build: docker build -t hybrid-selenium:latest .
# Run: docker run -v $(pwd):/workspace hybrid-selenium:latest mvn test

FROM ubuntu:20.04

ARG JAVA_VERSION=8
ARG MAVEN_VERSION=3.8.1
ARG CHROME_VERSION=latest

# Set environment variables
ENV DEBIAN_FRONTEND=noninteractive \
    JAVA_HOME=/usr/lib/jvm/java-${JAVA_VERSION}-openjdk-amd64 \
    MAVEN_HOME=/opt/maven \
    PATH=$JAVA_HOME/bin:/opt/maven/bin:$PATH \
    MAVEN_CONFIG=/root/.m2

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    # Java
    openjdk-${JAVA_VERSION}-jdk \
    # Build tools
    build-essential \
    curl \
    wget \
    git \
    zip \
    unzip \
    # Browsers
    chromium-browser \
    chromium-chromedriver \
    firefox \
    firefox-geckodriver \
    # Required libraries
    libxss1 \
    libappindicator1 \
    libindicator7 \
    xdg-utils \
    fonts-liberation \
    libappindicator3-1 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libatspi2.0-0 \
    libcups2 \
    libdbus-1-3 \
    libgdk-pixbuf2.0-0 \
    libgtk-3-0 \
    libpango-1.0-0 \
    libpangocairo-1.0-0 \
    libx11-6 \
    libx11-xcb1 \
    libxcb1 \
    libxcomposite1 \
    libxcursor1 \
    libxdamage1 \
    libxext6 \
    libxfixes3 \
    libxrandr2 \
    libxrender1 \
    ca-certificates \
    fonts-dejavu-core \
    fontconfig \
    # Utilities
    locales \
    && rm -rf /var/lib/apt/lists/*

# Install Maven
RUN mkdir -p /opt/maven && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | \
    tar -xz --strip-components=1 -C /opt/maven && \
    ln -s /opt/maven/bin/mvn /usr/local/bin/mvn

# Set locale
RUN locale-gen en_US.UTF-8
ENV LANG=en_US.UTF-8 LANGUAGE=en_US:en LC_ALL=en_US.UTF-8

# Create workspace directory
WORKDIR /workspace

# Create Maven cache directory with proper permissions
RUN mkdir -p /root/.m2 && chmod -R 777 /root/.m2

# Create output directories
RUN mkdir -p /workspace/output/reports && \
    mkdir -p /workspace/target && \
    chmod -R 777 /workspace/output && \
    chmod -R 777 /workspace/target

# Install dependencies (preload common JARs)
RUN mvn dependency:resolve -q -DoutputFile=/dev/null || true

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD java -version 2>&1 | head -n 1

# Labels
LABEL maintainer="QA Team" \
      description="Docker image for Hybrid Selenium Framework test execution" \
      version="1.0"

# Default command
CMD ["/bin/bash"]
