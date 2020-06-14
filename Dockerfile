# Creates pseudo distributed hadoop
#
# docker build -t hadoop .


FROM maven:3.5.2-jdk-8-alpine AS BUILD_ENV
MAINTAINER Clinton Yeboah

ENV HOME=/tmp
WORKDIR $HOME
ADD pom.xml $HOME
RUN mvn dependency:go-offline -B
ADD src $HOME/src
RUN mvn package

FROM bde2020/hadoop-base:2.0.0-hadoop3.2.1-java8

ADD bootstrap.sh /etc/bootstrap.sh
RUN chown root:root /etc/bootstrap.sh
RUN chmod 700 /etc/bootstrap.sh

ENV BOOTSTRAP /etc/bootstrap.sh

COPY --from=BUILD_ENV /tmp/target/hadoop-1.0-jar-with-dependencies.jar $HADOOP_HOME/hadoop.jar

CMD ["/etc/bootstrap.sh", "-d"]