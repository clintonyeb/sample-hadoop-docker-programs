#!/bin/bash
name=hadoop
docker stop $name || true && docker rm $name || true
docker build -t $name .
docker run --name $name -it $name /etc/bootstrap.sh -bash