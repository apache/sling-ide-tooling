import org.apache.sling.jenkins.SlingJenkinsHelper;

def mvnVersion = 'Maven 3.3.9'
def javaVersion = 'JDK 1.8 (latest)'

node('ubuntu') {

    def helper = new SlingJenkinsHelper()
    helper.runWithErrorHandling({ jobConfig ->

        stage('Build shared code') {
            withMaven(maven: mvnVersion, jdk: javaVersion, options: [artifactsPublisher(disabled: true)]) {
                timeout(10) {
                    sh "mvn -f shared/modules clean install"
                }
            }
        }

        stage('Build CLI bundles') {
            withMaven(maven: mvnVersion, jdk: javaVersion, options: [artifactsPublisher(disabled: true)]) {
                timeout(10) {
                    sh "mvn -f cli clean install"
                }
            }
        }

        stage ('Build shared code P2 repository') {
            withMaven(maven: mvnVersion, jdk: javaVersion, options: [artifactsPublisher(disabled: true)]) {
                timeout(10) {
                    sh 'mvn -f shared/p2 clean package'
                }
            }
        }

        stage ('Build Eclipse plug-ins') {
            withMaven(maven: mvnVersion, jdk: javaVersion, options: [artifactsPublisher(disabled: true)]) {
                timeout(20) {
                    wrap([$class: 'Xvfb']) {
                        sh 'mvn -f eclipse clean verify -Ddebug'
                    }
                    // workaround for https://issues.jenkins-ci.org/browse/JENKINS-55889
                    junit 'eclipse/**/surefire-reports/*.xml' 
                    archiveArtifacts artifacts: 'eclipse/**/logs/*.log'
                }
            }
        }
    });
}
