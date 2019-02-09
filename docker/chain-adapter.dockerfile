FROM openjdk:8-jre

WORKDIR /opt/notary

COPY chain-adapter/build/libs/chain-adapter-all.jar /opt/notary/chain-adapter.jar

# Please run `kscript --package grpc_healthcheck.kts` before building docker image
COPY grpc_healthcheck /opt/notary

COPY docker/entrypoint.sh /opt/notary/
RUN chmod +x /opt/notary/entrypoint.sh
ENTRYPOINT ["/opt/notary/entrypoint.sh"]
