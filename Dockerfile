# Creates pseudo distributed hadoop
#
# docker build -t sequenceiq/hadoop .
FROM maven:3.5.2-jdk-8-alpine AS BUILD_ENV
COPY pom.xml /tmp/
COPY src /tmp/src/
WORKDIR /tmp/
RUN mvn package

FROM debian
MAINTAINER Clinton Yeboah
COPY --from=BUILD_ENV /tmp/target/hadoop-1.0-jar-with-dependencies.jar /bin/
USER root

#RUN apt-get update \
# && apt-get install -y locales \
# && dpkg-reconfigure -f noninteractive locales \
# && locale-gen C.UTF-8 \
# && /usr/sbin/update-locale LANG=C.UTF-8 \
# && echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen \
# && locale-gen \
# && apt-get clean \
# && rm -rf /var/lib/apt/lists/*
#
## Users with other locales should set this in their derivative image
#ENV LANG en_US.UTF-8
#ENV LANGUAGE en_US:en
#ENV LC_ALL en_US.UTF-8


RUN apt-get update \
 && apt-get install -y curl unzip tar sudo openssh-server openssh-client rsync apt-utils wget gnupg software-properties-common
#    build-essential python-dev python3 python3-setuptools python3-pip \
# && ln -fs /usr/bin/python3 /usr/bin/python \
# && pip3 install py4j \

# passwordless ssh
RUN yes 'y' | ssh-keygen -q -N "" -t dsa -f /etc/ssh/ssh_host_dsa_key
RUN yes 'y' | ssh-keygen -q -N "" -t rsa -f /etc/ssh/ssh_host_rsa_key
RUN yes 'y' | ssh-keygen -q -N "" -t rsa -f /root/.ssh/id_rsa
RUN cp /root/.ssh/id_rsa.pub /root/.ssh/authorized_keys


# JAVA
RUN wget -qO - https://adoptopenjdk.jfrog.io/adoptopenjdk/api/gpg/key/public | sudo apt-key add -
RUN add-apt-repository --yes https://adoptopenjdk.jfrog.io/adoptopenjdk/deb/
RUN apt-get update -y \
 && apt-get install -y adoptopenjdk-8-hotspot \
 && update-alternatives --config java \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME /usr/lib/jvm/adoptopenjdk-8-hotspot-amd64
ENV PATH $PATH:$JAVA_HOME/bin

ENV HADOOP_VERSION 3.2.1
ENV HADOOP_HOME /usr/local/hadoop
ENV HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
ENV PATH $PATH:$HADOOP_HOME/bin


ENV HADOOP_HDFS_HOME $HADOOP_HOME
ENV HADOOP_MAPRED_HOME $HADOOP_HOME
ENV HADOOP_YARN_HOME $HADOOP_HOME
ENV HADOOP_CONF_DIR $HADOOP_HOME/etc/hadoop
ENV YARN_CONF_DIR $HADOOP_HOME/etc/hadoop


RUN curl -# -L --retry 3 "http://archive.apache.org/dist/hadoop/common/hadoop-$HADOOP_VERSION/hadoop-$HADOOP_VERSION.tar.gz" | tar -xz -C /usr/local/ 
RUN cd /usr/local && ln -s ./hadoop-${HADOOP_VERSION} hadoop
RUN rm -rf $HADOOP_HOME/share/doc && chown -R root:root $HADOOP_HOME

# download native support
# RUN mkdir -p /tmp/native
# RUN curl -L https://github.com/sequenceiq/docker-hadoop-build/releases/download/v2.7.1/hadoop-native-64-2.7.1.tgz | tar -xz -C /tmp/native

# hadoop
# RUN curl -s http://apache.spinellicreations.com/hadoop/common/hadoop-2.10.0/hadoop-2.10.0.tar.gz | tar -xz -C /usr/local/
# RUN cd /usr/local && ln -s ./hadoop-2.7.1 hadoop

RUN sed -i '/.*export JAVA_HOME/ s:.*:export JAVA_HOME=/usr/lib/jvm/adoptopenjdk-8-hotspot-amd64\nexport HADOOP_PREFIX=/usr/local/hadoop\nexport HADOOP_HOME=/usr/local/hadoop\n:' $HADOOP_HOME/etc/hadoop/hadoop-env.sh
RUN sed -i '/.*export HADOOP_CONF_DIR/ s:.*:export HADOOP_CONF_DIR=/usr/local/hadoop/etc/hadoop/:' $HADOOP_HOME/etc/hadoop/hadoop-env.sh
# RUN cat $HADOOP_HOME/etc/hadoop/hadoop-env.sh

RUN mkdir $HADOOP_HOME/input
RUN cp $HADOOP_HOME/etc/hadoop/*.xml $HADOOP_HOME/input

# pseudo distributed
ADD config/core-site.xml.template $HADOOP_HOME/etc/hadoop/core-site.xml.template
RUN sed s/HOSTNAME/localhost/ $HADOOP_HOME/etc/hadoop/core-site.xml.template > $HADOOP_HOME/etc/hadoop/core-site.xml
ADD config/hdfs-site.xml $HADOOP_HOME/etc/hadoop/hdfs-site.xml

ADD config/mapred-site.xml $HADOOP_HOME/etc/hadoop/mapred-site.xml
ADD config/yarn-site.xml $HADOOP_HOME/etc/hadoop/yarn-site.xml

RUN $HADOOP_HOME/bin/hdfs namenode -format

# fixing the libhadoop.so like a boss
# RUN rm -rf $HADOOP_HOME/lib/native
# RUN mv /tmp/native $HADOOP_HOME/lib

ADD config/ssh_config /root/.ssh/config
RUN chmod 600 /root/.ssh/config
RUN chown root:root /root/.ssh/config

# # installing supervisord
# RUN yum install -y python-setuptools
# RUN easy_install pip
# RUN curl https://bitbucket.org/pypa/setuptools/raw/bootstrap/ez_setup.py -o - | python
# RUN pip install supervisor
#
# ADD supervisord.conf /etc/supervisord.conf

ADD bootstrap.sh /etc/bootstrap.sh
RUN chown root:root /etc/bootstrap.sh
RUN chmod 700 /etc/bootstrap.sh

ENV BOOTSTRAP /etc/bootstrap.sh

# workingaround docker.io build error
#RUN ls -la $HADOOP_HOME/etc/hadoop/*-env.sh
RUN chmod +x $HADOOP_HOME/etc/hadoop/*-env.sh
#RUN ls -la $HADOOP_HOME/etc/hadoop/*-env.sh

# fix the 254 error code
# RUN sed  -i "/^[^#]*UsePAM/ s/.*/#&/"  /etc/ssh/sshd_config
# RUN echo "UsePAM no" >> /etc/ssh/sshd_config
# RUN echo "Port 2122" >> /etc/ssh/sshd_config

ENV HDFS_NAMENODE_USER root
ENV HDFS_DATANODE_USER root
ENV HDFS_SECONDARYNAMENODE_USER root
ENV YARN_RESOURCEMANAGER_USER root
ENV YARN_NODEMANAGER_USER root

RUN service ssh start && $HADOOP_HOME/etc/hadoop/hadoop-env.sh && $HADOOP_HOME/sbin/start-all.sh dfs -mkdir -p /user/root
RUN service ssh start && $HADOOP_HOME/etc/hadoop/hadoop-env.sh && $HADOOP_HOME/sbin/start-all.sh dfs -put $HADOOP_HOME/etc/hadoop/ input

# Clean up
RUN mkdir -p /home/input
RUN rm -rf /home/input/* && rm -rf /home/output
ADD resources /home/input/

CMD ["/etc/bootstrap.sh", "-d"]

# Hdfs ports
EXPOSE 50010 50020 50070 50075 50090 8020 9000
# Mapred ports
EXPOSE 10020 19888
#Yarn ports
EXPOSE 8030 8031 8032 8033 8040 8042 8088
#Other ports
EXPOSE 49707 2122
