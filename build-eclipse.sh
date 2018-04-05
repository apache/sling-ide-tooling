#!/bin/sh -e

mvn -f shared/modules clean install
mvn -f shared/p2 clean package
mvn -f eclipse clean verify
