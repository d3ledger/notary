FROM openjdk:8-jre


WORKDIR /opt/healthcheck

# Please run `kscript --package grpc_healthcheck.kts` before building docker image
COPY grpc_healthcheck /opt/healthcheck

ENTRYPOINT ["/bin/bash","/opt/healthcheck/grpc_healthcheck"]
