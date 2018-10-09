FROM ubuntu:latest

RUN apt-get update && \
	apt-get install --yes software-properties-common && \
	add-apt-repository --yes ppa:bitcoin/bitcoin && \
	apt-get update && \
	apt-get install --yes bitcoind make

COPY . /root

CMD ["bitcoind", "--conf=/root/bitcoin.conf"]
