#!/bin/sh

rm -rf felix-cache launcher

appbuilder_jar=bin/appbuilder.jar

# download application builder, if needed
if [ ! -f ${appbuilder_jar} ]; then
    wget https://repository.apache.org/content/groups/snapshots/org/apache/sling/org.apache.sling.feature.applicationbuilder/0.1.0-SNAPSHOT/org.apache.sling.feature.applicationbuilder-0.1.0-20180906.104203-63.jar -O ${appbuilder_jar}
fi

# assemble application
java -jar ${appbuilder_jar} \
    -u file://$HOME/.m2/repository,https://repo.maven.apache.org/maven2,https://repository.apache.org/content/groups/snapshots \
    -f features/clisync.json \
    -o bin/app.json
