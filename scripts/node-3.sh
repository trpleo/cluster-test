#!/usr/bin/env bash

export SEEDNODES="akka.tcp://TestSTOCluster@127.0.0.1:6651,akka.tcp://TestSTOCluster@127.0.0.1:6652,akka.tcp://TestSTOCluster@127.0.0.1:6653"
export HTTPSERVICEPORT=9002
export CMHTTPSERVICEPORT=8502
sbt "run \"akka.tcp://TestSTOCluster@127.0.0.1:6653\"" -Xmx1024m -Xms256m -Xss64M -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.manament.jmxremote.ssl=false