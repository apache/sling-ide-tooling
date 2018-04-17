#!/bin/sh

rm -rf felix-cache launcher

java -jar ../../../whiteboard/featuremodel/feature-launcher/target/org.apache.sling.feature.launcher-0.0.1-SNAPSHOT.jar -a sling.json
