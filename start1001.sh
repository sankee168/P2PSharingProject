#!/usr/bin/env bash

mvn exec:java -f pom.xml  -Dexec.mainClass="networks.peerProcess" -Dexec.args="1001"
