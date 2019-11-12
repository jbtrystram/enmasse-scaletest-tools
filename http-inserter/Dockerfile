FROM quay.io/enmasse/java-base:11-1

ARG version
ARG maven_version
ARG commit
ENV VERSION=${version} COMMIT=${commit} MAVEN_VERSION=${maven_version}

ADD target/${project.build.finalName}.jar /enmasse-scaletest-http-inserter.jar
RUN chgrp -R 0 /enmasse-scaletest-http-inserter.jar && \
    chmod -R g=u /enmasse-scaletest-http-inserter.jar


ENV JAVA_LAUNCH_PROFILE=openjdk-11
CMD ["/opt/run-java/launch_java.sh", "/enmasse-scaletest-http-inserter.jar", "/opt/scaletest/config.yml"]