#!/bin/sh

launcher_jar=bin/featurelauncher.jar

if [ ! -f ${launcher_jar} ]; then
    wget https://repository.apache.org/content/groups/snapshots/org/apache/sling/org.apache.sling.feature.launcher/0.1.0-SNAPSHOT/org.apache.sling.feature.launcher-0.1.0-20180906.104202-54.jar -O ${launcher_jar}
fi

java -jar ${launcher_jar} \
    -c $HOME/.m2/repository \
    -u file://$HOME/.m2/repository,https://repo.maven.apache.org/maven2,https://repository.apache.org/content/groups/snapshots \
    -f bin/app.json
