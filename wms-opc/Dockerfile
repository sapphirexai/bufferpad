FROM bellsoft/liberica-openjdk-rocky:17.0.15-cds

LABEL maintainer="buffer-pad"

RUN mkdir -p /bufferpad/wms-opc/logs /bufferpad/wms-opc/temp

WORKDIR /bufferpad/wms-opc

ENV SERVER_PORT=9001 \
    SPRING_PROFILES_ACTIVE=prod \
    LOG_FILE=logs/wms-opc-prod.log \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_OPTS=""

EXPOSE ${SERVER_PORT}

COPY target/opc-*.jar app.jar

SHELL ["/bin/bash", "-c"]

ENTRYPOINT exec java \
    -Djava.security.egd=file:/dev/./urandom \
    -Dserver.port=${SERVER_PORT} \
    -XX:+HeapDumpOnOutOfMemoryError \
    ${JAVA_OPTS} \
    -jar app.jar \
    --spring.profiles.active=${SPRING_PROFILES_ACTIVE}
