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
    //parallel([
    	//'linux': generateStages('linux', mvnVersion, javaVersion)
        //'windows': generateStages('windows', mvnVersion, javaVersion)
    //    ])
    if (shouldDeploy()) {
    	buildAndDeploySignedP2Repository(mvnVersion, javaVersion)
    }
})

// generates os-specific stages
def generateStages(String os, def mvnVersion, def javaVersion) {
    def isWindows = os == "windows"
    def prefix = isWindows ? "win" : "linux"
    def nodeLabel = isWindows ? "Windows" : "ubuntu"
    
    String goals = (!isWindows && shouldDeploy()) ? 'clean deploy' : 'clean install'

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
            withMaven(maven: mvnVersion, jdk: javaVersion, mavenLocalRepo: '.repository', options: [artifactsPublisher(disabled: true)]) {
                timeout(20) {
                    // workaround for https://issues.jenkins-ci.org/browse/JENKINS-39415
                    wrap([$class: 'Xvfb', autoDisplayName: true]) {
                        runCmd "mvn -f eclipse ${goals}"
                    }
                    // workaround for https://issues.jenkins-ci.org/browse/JENKINS-55889
                    junit(testResults: 'eclipse/**/surefire-reports/*.xml', allowEmptyResults: true)
                    archiveArtifacts(artifacts: 'eclipse/**/logs/*.log', allowEmptyArchive: true)
                }
            }
        }
    ]

    return {
    	node(nodeLabel) {
    		echo "Running on node ${env.NODE_NAME}"
    		checkout scm
	        stages.each { name, body ->
	            stage(name) {
	                body.call()
	            }
	        }
        }
    }
}

def buildAndDeploySignedP2Repository( def mvnVersion, def javaVersion ) {
	node('pkcs11') {
		stage('Build Signed P2 Repository') {
			echo "Running on node ${env.NODE_NAME} with PKCS#11 config at ${env.PKCS11_CONFIG}"
			checkout scm
			// set up environment variables according to https://docs.digicert.com/de/digicert-one/secure-software-manager/ci-cd-integrations/maven-integration-with-pkcs11.html
			withCredentials([
				file(credentialsId: 'sling-digicert-pkcs-certificate', variable: 'SM_CLIENT_CERT_FILE'), 
				string(credentialsId: 'sling-digicert-pkcs-cert-pw', variable: 'SM_CLIENT_CERT_PASSWORD'),
				string(credentialsId: 'sling-digicert-pkcs-api-key', variable: 'SM_API_KEY')]) {
				// https://docs.digicert.com/de/digicert-one/secure-software-manager/client-tools/configure-environment-variables.html
				// redirecting log to another file does not work for some reason
				withEnv(['SM_LOG_LEVEL=ERROR','SM_HOST=https://clientauth.one.digicert.com']) {
					try {
						withMaven(maven: mvnVersion, jdk: javaVersion, mavenLocalRepo: '.repository', options: [artifactsPublisher(disabled: true)]) {
			                timeout(20) {
			                	// build with profile "sign-with-jarsigner" for signing
			                    runCmd 'mvn -f eclipse/p2update clean verify -e'
			                }
			            }
			        } catch (e) {
			        	// reenable next line to expose further infos about signature errors
			        	//echo('smpkcs11.log: ' + readFile(file: "${env.HOME}/.signingmanager/logs/smpkcs11.log"))
			        	throw e
			        }
			    }
			}
		}
		stage('Deploy to ASF Nightlies') {
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
							removePrefix: 'eclipse/p2update/target/repository',
							sourceFiles: 'eclipse/p2update/target/repository')
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
	return env.CHANGE_BRANCH == 'feature/sign-jars'
}
