#!/bin/bash
mvn exec:java -Dexec.mainClass="SimpleQuery" -Dexec.args="$1"
