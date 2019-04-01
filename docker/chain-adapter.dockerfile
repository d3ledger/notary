FROM openjdk:8-jre

WORKDIR /opt/notary

COPY chain-adapter/build/libs/chain-adapter-all.jar /opt/notary/chain-adapter.jar

# Please run `kscript --package grpc_healthcheck.kts` before building docker image
COPY grpc_healthcheck /opt/notary

## Wait for script (see https://github.com/ufoscout/docker-compose-wait/)
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.5.0/wait /opt/notary/wait
RUN chmod +x /opt/notary/wait

COPY docker/entrypoint.sh /opt/notary/
RUN chmod +x /opt/notary/entrypoint.sh
ENTRYPOINT /opt/notary/wait && /opt/notary/entrypoint.sh
