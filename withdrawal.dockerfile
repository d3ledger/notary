FROM gradle

ADD --chown=gradle:gradle . /build/
RUN gradle -b /build/build.gradle assemble
ENTRYPOINT ["gradle", "-b", "/build/build.gradle", "runWithdrawal", "-Pprofile=deploy", "--stacktrace"]
