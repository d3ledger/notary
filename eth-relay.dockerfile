FROM openjdk

WORKDIR /opt/notary

COPY build/libs/notary-1.0-SNAPSHOT-all.jar /opt/notary/notary.jar
COPY iroha_bindings/linux/* /opt/notary/
ENV LD_LIBRARY_PATH="/opt/notary:${LD_LIBRARY_PATH}"

ENTRYPOINT ["java", "-cp", "/opt/notary/notary.jar", "registration.eth.relay.DeployRelayMain"]
