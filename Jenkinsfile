pipeline {
    agent {
        label 'ubuntu'
    }

    tools {
        maven 'Maven 3.3.9'
        jdk 'JDK 1.8 (latest)'
    }

    stages {
        stage ('Build shared code') {
            steps {
                timeout(10) {
                    sh 'mvn -f shared/modules clean install'
                }
                junit 'shared/modules/**/surefire-reports/*.xml'
            }
        }

        stage ('Build shared code P2 repository') {
            steps {
                timeout(10) {
                    sh 'mvn -f shared/p2 clean package'
                }
            }
        }

        stage ('Build Eclipse plug-ins') {
            steps {
                wrap([$class: 'Xvfb']) {
                    timeout(20) {
                        sh 'mvn -f eclipse clean verify'
                    }
                }
                junit 'eclipse/**/surefire-reports/*.xml'
                archiveArtifacts artifacts: 'eclipse/**/logs/*.log'
            }
        }
    }

    post {
        failure {
            mail to: 'dev@sling.apache.org',
            subject: "Failed Pipeline: ${currentBuild.fullDisplayName}",
            body: "See ${env.BUILD_URL}"
        }

        unstable {
            mail to: 'dev@sling.apache.org',
            subject: "Failed Pipeline: ${currentBuild.fullDisplayName}",
            body: "See ${env.BUILD_URL}"
        }

    }

}

