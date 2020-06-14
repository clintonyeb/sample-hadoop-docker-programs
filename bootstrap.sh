#!/bin/bash

rm -rf /output
$HADOOP_HOME/bin/hadoop jar $HADOOP_HOME/hadoop.jar Main /input /output

if [[ $1 == "-d" ]]; then
  while true; do sleep 1000; done
fi

if [[ $1 == "-bash" ]]; then
  /bin/bash
fi
