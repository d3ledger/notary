FROM python
RUN pip install requests
ENV NODE_0="d3-eth-node0"
ENV NODE_1="d3-eth-node1"
ENV PORT=8545

ENTRYPOINT ["python", "/eth/main.py"]
