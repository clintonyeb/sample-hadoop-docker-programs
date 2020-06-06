#!/bin/bash
: ${HADOOP_PREFIX:=/usr/local/hadoop}
$HADOOP_PREFIX/etc/hadoop/hadoop-env.sh
rm /tmp/*.pid
service ssh start
$HADOOP_PREFIX/sbin/start-all.sh

java -jar $HADOOP_PREFIX/hadoop.jar /input /output

if [[ $1 == "-d" ]]; then
  while true; do sleep 1000; done
fi

if [[ $1 == "-bash" ]]; then
  /bin/bash
fi

