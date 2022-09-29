import org.apache.sling.jenkins.SlingJenkinsHelper;

def mvnVersion = 'Maven 3.3.9'
def javaVersion = 'JDK 17 (latest)'

node('ubuntu') {
    def helper = new SlingJenkinsHelper()
    helper.runWithErrorHandling({ jobConfig ->
        parallel 'linux': generateStages('linux', mvnVersion, javaVersion),
            'windows': generateStages('windows', mvnVersion, javaVersion)
    })
}

// generates os-specific stages
def generateStages(String os, def mvnVersion, def javaVersion) {
    def isWindows = os == "windows"
    def prefix = isWindows ? "win" : "linux"

    def stages = [
        "[$prefix] Build shared code": {
            withMaven(maven: mvnVersion, jdk: javaVersion, options: [artifactsPublisher(disabled: true)]) {
                timeout(10) {
                    runCmd "mvn -f shared clean install"
                }
            }
        }, "[$prefix] Build CLI bundles": {
            withMaven(maven: mvnVersion, jdk: javaVersion, options: [artifactsPublisher(disabled: true)]) {
                timeout(10) {
                    runCmd "mvn -f cli clean install"
                }
            }
        }, "[$prefix] Build Eclipse plug-ins": {
            withMaven(maven: mvnVersion, jdk: javaVersion, options: [artifactsPublisher(disabled: true)]) {
                timeout(20) {
                    // workaround for https://issues.jenkins-ci.org/browse/JENKINS-39415
                    wrap([$class: 'Xvfb', autoDisplayName: true]) {
                        runCmd 'mvn -f eclipse clean verify'
                    }
                    // workaround for https://issues.jenkins-ci.org/browse/JENKINS-55889
                    junit 'eclipse/**/surefire-reports/*.xml' 
                    archiveArtifacts artifacts: 'eclipse/**/logs/*.log'
                }
            }
        }
    ]

    // avoid wrapping Linux nodes again in node() context since that seems to make the 
    // SCM checkout unavailable
    if ( isWindows ) {
        return {
            node("Windows") {
                checkout scm
                stages.each { name, body ->
                    stage(name) {
                        body.call()
                    }
                }
            }
        }
    }

    return {
        stages.each { name, body ->
            stage(name) {
                body.call()
            }
        }
    }
}

def runCmd(def cmd) {
    if (isUnix() ) {
        sh cmd
    } else {
        bat cmd
    }
}
