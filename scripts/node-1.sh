#!/usr/bin/env bash

export SEEDNODES="akka.tcp://TestSTOCluster@127.0.0.1:6651,akka.tcp://TestSTOCluster@127.0.0.1:6652,akka.tcp://TestSTOCluster@127.0.0.1:6653"
export HTTPSERVICEPORT=9000
sbt "run \"akka.tcp://TestSTOCluster@127.0.0.1:6651\"" -Xmx1024m -Xms256m -Xss64M -Dcom.sun.management.jmxremote.port=9997 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.manament.jmxremote.ssl=false