FROM openjdk:8-jre

WORKDIR /opt/notary

COPY btc-registration/build/libs/btc-registration-all.jar /opt/notary/btc-registration.jar

# Please run `kscript --package grpc_healthcheck.kts` before building docker image
COPY grpc_healthcheck /opt/notary

COPY docker/entrypoint.sh /opt/notary/
RUN chmod +x /opt/notary/entrypoint.sh
ENTRYPOINT ["/opt/notary/entrypoint.sh"]
