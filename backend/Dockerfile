# FlashTicket 后端运行镜像（生产）
# 设计前提：构建产物由本地 `mvn clean package -DskipTests` 生成后上传，
# 服务器上只做「拷贝 + 启动」，不在容器里跑 Maven —— 1G 内存编译必 OOM。
#
# 构建：docker build -t flashticket-backend:1.0.0 ./backend
# 前提：backend/target/flashticket-backend-*.jar 已上传

FROM eclipse-temurin:21-jre-jammy

# 项目全链路按 UTC 存储与比较（DATETIME(3) UTC），容器时区必须与之一致，
# 否则日志时间、限流窗口、订单超时判定全部偏 8 小时。
ENV TZ=UTC \
    LANG=C.UTF-8 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms256m -Xmx288m -Xss512k -XX:+UseSerialGC -XX:MaxMetaspaceSize=96m -XX:MaxDirectMemorySize=32m -XX:+ExitOnOutOfMemoryError -Duser.timezone=UTC -Dfile.encoding=UTF-8"

WORKDIR /app
RUN mkdir -p /app/logs

COPY target/flashticket-backend-*.jar /app/app.jar

EXPOSE 8080

# 用 bash 的 /dev/tcp 探测端口，基础镜像里没有 curl/wget，不额外装包。
HEALTHCHECK --interval=20s --timeout=5s --start-period=90s --retries=5 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
