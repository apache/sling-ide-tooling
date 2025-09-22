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
    parallel([
    	'linux': generateStages('linux', mvnVersion, javaVersion),
        'windows': generateStages('windows', mvnVersion, javaVersion)
        ])
    if (shouldDeploy()) {
    	buildAndDeployP2Repository(mvnVersion, javaVersion)
    }
})

// generates os-specific stages
def generateStages(String os, def mvnVersion, def javaVersion) {
    def isWindows = os == "windows"
    def prefix = isWindows ? "win" : "linux"
    def nodeLabel = isWindows ? "Windows" : "ubuntu"
    
    boolean shouldDeployInThisBranch = (!isWindows && shouldDeploy())
    String goals = shouldDeployInThisBranch ? 'clean deploy' : 'clean install'

    def stages = [
        // use a local repository due to using version ranges in Tycho (https://github.com/eclipse-tycho/tycho/issues/1464)
        // otherwise resolving metadata might fail as the global repo seems to have invalid metadata
        "[$prefix] Build shared code": {
            withMaven(maven: mvnVersion, jdk: javaVersion, mavenLocalRepo: '.repository', options: [artifactsPublisher(disabled: true)]) {
                timeout(10) {
                    runCmd "mvn -f shared ${goals}"
                }
            }
        }, "[$prefix] Build CLI bundles": {
            withMaven(maven: mvnVersion, jdk: javaVersion, mavenLocalRepo: '.repository', options: [artifactsPublisher(disabled: true)]) {
                timeout(10) {
                    runCmd "mvn -f cli ${goals}"
                }
            }
        }, "[$prefix] Build Eclipse plug-ins": {
        	try {
	            withMaven(maven: mvnVersion, jdk: javaVersion, mavenLocalRepo: '.repository', options: [artifactsPublisher(disabled: true)]) {
	                timeout(60) {
	                    // workaround for https://issues.jenkins-ci.org/browse/JENKINS-39415
	                    wrap([$class: 'Xvfb', autoDisplayName: true]) {
	                        runCmd "mvn -f eclipse ${goals}"
	                    }
	                }

	                if (shouldDeployInThisBranch) {
	                    stash(name: 'p2-repository', includes: 'eclipse/p2update/target/repository/**')
	                }
	            }
	        } finally {
                // workaround for https://issues.jenkins-ci.org/browse/JENKINS-55889
                junit(testResults: 'eclipse/**/surefire-reports/*.xml', allowEmptyResults: true)
                archiveArtifacts(artifacts: 'eclipse/**/logs/*.log', allowEmptyArchive: true)
            }
        }
    ]

    return {
    	node(nodeLabel) {
    		stage("[$prefix] Clone") {
	    		echo "Running on node ${env.NODE_NAME}"
	    		checkout scm
	    	}
	        stages.each { name, body ->
	            stage(name) {
	                body.call()
	            }
	        }
        }
    }
}

def buildAndDeployP2Repository( def mvnVersion, def javaVersion ) {
	node('ubuntu') {
		stage('Deploy to ASF Nightlies') {
			echo "Running on node ${env.NODE_NAME}"
			unstash(name: 'p2-repository')
			sshPublisher(publishers: [
				sshPublisherDesc(configName: 'Nightlies', 
					transfers: [
						sshTransfer(
							cleanRemote: false,
							excludes: '',
							execCommand: '',
							execTimeout: 120000,
							flatten: false,
							makeEmptyDirs: false,
							noDefaultExcludes: false,
							patternSeparator: '[, ]+',
							remoteDirectory: '/sling/eclipse',
							remoteDirectorySDF: false,
							removePrefix: 'eclipse/p2update/target/repository/',
							sourceFiles: 'eclipse/p2update/target/repository/')
					], 
					usePromotionTimestamp: false,
					useWorkspaceInPromotion: false,
					verbose: true)
				])
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

boolean shouldDeploy() {
	return env.BRANCH_NAME == 'master'
}
