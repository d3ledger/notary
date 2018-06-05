FROM ubuntu:16.04

RUN apt-get update && \
    apt-get install -y wget default-jre

RUN mkdir /opt/waves

RUN wget https://github.com/wavesplatform/Waves/releases/download/v0.13.1/waves-all-0.13.1.jar -O /root/waves.jar

WORKDIR /opt/waves

ENTRYPOINT ["/bin/bash"]