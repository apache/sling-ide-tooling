import org.apache.sling.jenkins.SlingJenkinsHelper;

def mvnVersion = 'maven_3_latest' // https://cwiki.apache.org/confluence/x/cRTiAw
def javaVersion = 'jdk_17_latest' // https://cwiki.apache.org/confluence/x/kRLiAw

def helper = new SlingJenkinsHelper()
def jobConfig = [
    jdks: [8],
    upstreamProjects: [],
    archivePatterns: [],
    mavenGoal: '',
    additionalMavenParams: '',
    rebuildFrequency: '@weekly',
    enabled: true,
    emailRecipients: [],
    sonarQubeEnabled: true,
    sonarQubeUseAdditionalMavenParams: true,
    sonarQubeAdditionalParams: ''
]
helper.runWithErrorHandling(jobConfig, {
    parallel 'linux': generateStages('linux', mvnVersion, javaVersion),
        'windows': generateStages('windows', mvnVersion, javaVersion)
})

// generates os-specific stages
def generateStages(String os, def mvnVersion, def javaVersion) {
    def isWindows = os == "windows"
    def prefix = isWindows ? "win" : "linux"
    def nodeName = isWindows ? "Windows" : "ubuntu"

    def stages = [
        // use a local repository due to using version ranges in Tycho (https://github.com/eclipse-tycho/tycho/issues/1464)
        // otherwise resolving metadata might fail as the global repo seems to have invalid metadata
        "[$prefix] Build shared code": {
            withMaven(maven: mvnVersion, jdk: javaVersion, mavenLocalRepo: '.repository', options: [artifactsPublisher(disabled: true)]) {
                timeout(10) {
                    runCmd "mvn -f shared clean install"
                }
            }
        }, "[$prefix] Build CLI bundles": {
            withMaven(maven: mvnVersion, jdk: javaVersion, mavenLocalRepo: '.repository', options: [artifactsPublisher(disabled: true)]) {
                timeout(10) {
                    runCmd "mvn -f cli clean install"
                }
            }
        }, "[$prefix] Build Eclipse plug-ins": {
            withMaven(maven: mvnVersion, jdk: javaVersion, mavenLocalRepo: '.repository', options: [artifactsPublisher(disabled: true)]) {
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

    return {
    	node(nodeName) {
    		checkout scm
	        stages.each { name, body ->
	            stage(name) {
	                body.call()
	            }
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
