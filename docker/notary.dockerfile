FROM openjdk:8-jre

WORKDIR /opt/notary

COPY eth/build/libs/eth-all.jar /opt/notary/notary.jar
COPY iroha_bindings/linux/* /opt/notary/
ENV LD_LIBRARY_PATH="/opt/notary:${LD_LIBRARY_PATH}"

# Please run `kscript --package grpc_healthcheck.kts` before building docker image
COPY grpc_healthcheck /opt/notary

COPY docker/entrypoint.sh /opt/notary/
RUN chmod +x /opt/notary/entrypoint.sh
ENTRYPOINT ["/opt/notary/entrypoint.sh"]
