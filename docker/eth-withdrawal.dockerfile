FROM openjdk:8-jre

WORKDIR /opt/notary

COPY eth-withdrawal/build/libs/eth-withdrawal-all.jar /opt/notary/eth-withdrawal.jar

# Please run `kscript --package grpc_healthcheck.kts` before building docker image
COPY grpc_healthcheck /opt/notary

COPY docker/entrypoint.sh /opt/notary/
RUN chmod +x /opt/notary/entrypoint.sh
ENTRYPOINT ["/opt/notary/entrypoint.sh"]
