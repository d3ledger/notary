FROM gradle

ADD --chown=gradle:gradle . /build/
RUN gradle -b /build/build.gradle assemble
# TODO: invoke runDeployRelay from the code, not there. This is definetely a hack
ENTRYPOINT ["gradle", "-b", "/build/build.gradle", "runDeployRelay", "runDeployRelay", "runRegistration"]
