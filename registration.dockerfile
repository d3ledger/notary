FROM openjdk:8-jre

WORKDIR /opt/notary

COPY build/libs/notary-1.0-SNAPSHOT-all.jar /opt/notary/notary.jar
COPY iroha_bindings/linux/* /opt/notary/
ENV LD_LIBRARY_PATH="/opt/notary:${LD_LIBRARY_PATH}"

ENTRYPOINT ["/bin/bash", "-c" ,"sleep 10 && java -cp /opt/notary/notary.jar registration.eth.EthRegistrationMain"]
