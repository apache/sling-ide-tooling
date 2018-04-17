#!/bin/sh

rm -rf felix-cache launcher

java -cp  ../../../whiteboard/featuremodel/feature-applicationbuilder/target/org.apache.sling.feature.applicationbuilder-0.0.1-SNAPSHOT.jar:${HOME}/.m2/repository/org/apache/felix/org.apache.felix.framework/5.6.8/org.apache.felix.framework-5.6.8.jar   org.apache.sling.feature.applicationbuilder.impl.Main   -d features/   -u file://${HOME}/.m2/repository   -o sling.json
