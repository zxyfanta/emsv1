# 基础镜像：包含JDK17、Maven和curl
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /workspace

# 安装Maven和curl
RUN apt-get update && \
    apt-get install -y \
    maven \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 创建应用用户
RUN groupadd -r ems && useradd -r -g ems ems

# 设置Maven本地仓库目录
RUN mkdir -p /var/maven/.m2 && \
    chown -R ems:ems /var/maven

# 设置环境变量
ENV MAVEN_HOME=/usr/share/maven
ENV M2_HOME=/usr/share/maven
ENV PATH=$MAVEN_HOME/bin:$PATH
ENV MAVEN_OPTS="-Xmx512m"

# 创建应用工作目录
RUN mkdir -p /app && \
    chown -R ems:ems /app

# 切换到应用用户
USER ems

# 设置默认工作目录
WORKDIR /app

# 验证安装
RUN java -version && \
    mvn -version && \
    curl --version