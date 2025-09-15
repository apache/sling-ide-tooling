# fail if RELEASE_VERSION or NEXT_VERSION is not set
ifndef RELEASE_VERSION
$(error RELEASE_VERSION is not set)
endif

ifndef NEXT_VERSION
$(error NEXT_VERSION is not set)
endif

release: check-gpg pre-release-shared pre-release-eclipse push post-release-shared post-release-eclipse
.PHONY=release

# ensure that GPG signing will work in batch mode
check-gpg:
	gpg --sign README.md
	rm -f README.md.gpg
.PHONY=check-gpg

pre-release-shared:
	cd shared && mvn --batch-mode release:prepare -DdryRun=true -DreleaseVersion=$(RELEASE_VERSION) -DdevelopmentVersion=$(NEXT_VERSION)
	cd shared && mvn --batch-mode versions:set -DnewVersion=$(RELEASE_VERSION) -DprocessAllModules=true -DgenerateBackupPoms=false
	cd shared && git add pom.xml '**/pom.xml' && git commit -m 'chore(shared): prepare release $(RELEASE_VERSION)'
	cd shared && mvn --batch-mode clean install -DskipTests

.PHONY=pre-release-shared

pre-release-eclipse:
.PHONY=pre-release-eclipse

push:
.PHONY=push

post-release-shared:
	cd shared && mvn --batch-mode versions:set -DnewVersion=$(NEXT_VERSION) -DprocessAllModules=true -DgenerateBackupPoms=false
	cd shared && git add pom.xml '**/pom.xml' && git commit -m 'chore(shared): bump version to $(NEXT_VERSION)'
.PHONY=post-release-shared

post-release-eclipse:
.PHONY=post-release-eclipse
