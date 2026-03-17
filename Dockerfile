# 使用 DaoCloud 镜像源的 OpenJDK 镜像
FROM docker.m.daocloud.io/library/eclipse-temurin:17-jre

# 设置工作目录
WORKDIR /app

# 复制构建好的 JAR 包（需要在本地先执行 mvn clean package）
COPY target/seckill-system-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8083

# JVM 参数优化
ENV JAVA_OPTS="-Xms512m -Xmx512m"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]