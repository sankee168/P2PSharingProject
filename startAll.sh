#!/usr/bin/env bash

mvn exec:java -f pom.xml  -Dexec.mainClass="networks.peerProcess" -Dexec.args="1001" &
mvn exec:java -f pom.xml  -Dexec.mainClass="networks.peerProcess" -Dexec.args="1002" &
mvn exec:java -f pom.xml  -Dexec.mainClass="networks.peerProcess" -Dexec.args="1003" &
#mvn exec:java -f pom.xml  -Dexec.mainClass="networks.peerProcess" -Dexec.args="1004" &
#mvn exec:java -f pom.xml  -Dexec.mainClass="networks.peerProcess" -Dexec.args="1005" &
#mvn exec:java -f pom.xml  -Dexec.mainClass="networks.peerProcess" -Dexec.args="1006"
