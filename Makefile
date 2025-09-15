# fail if RELEASE_VERSION or NEXT_VERSION is not set
ifndef RELEASE_VERSION
$(error RELEASE_VERSION is not set)
endif

ifndef NEXT_VERSION
$(error NEXT_VERSION is not set)
endif

release: check-gpg release-shared release-eclipse
.PHONY=release

# ensure that GPG signing will work in batch mode
check-gpg:
	gpg --sign README.md
	rm -f README.md.gpg
.PHONY=check-gpg

release-shared:
	cd shared && mvn --batch-mode release:prepare release:perform -DreleaseVersion=$(RELEASE_VERSION) -DdevelopmentVersion=$(NEXT_VERSION) -Dtag=sling-ide-tooling-shared-$(RELEASE_VERSION)

.PHONY=release-shared

release-eclipse:
	cd eclipse && mvn --batch-mode release:prepare release:perform -DreleaseVersion=$(RELEASE_VERSION) -DdevelopmentVersion=$(NEXT_VERSION) -Dtag=sling-ide-tooling-eclipse-$(RELEASE_VERSION)

.PHONY=release-eclipse

