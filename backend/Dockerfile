# EMS Spring Boot Backend Dockerfile - 在容器内编译并运行
FROM maven:3.8.5-openjdk-17

# 设置工作目录
WORKDIR /app

# 创建应用用户
RUN groupadd -r ems && useradd -r -g ems ems

# 复制整个项目源代码
COPY . .

# 编译项目
RUN mvn clean package -DskipTests

# 创建日志目录
RUN mkdir -p /app/logs && chown -R ems:ems /app

# 切换到应用用户
USER ems

# 暴露端口
EXPOSE 8081

# 启动应用
ENTRYPOINT ["java", "-jar", "target/ems-0.0.1-SNAPSHOT.jar"]