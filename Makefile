# fail if RELEASE_VERSION or NEXT_VERSION is not set
ifndef RELEASE_VERSION
$(error RELEASE_VERSION is not set)
endif

ifndef NEXT_VERSION
$(error NEXT_VERSION is not set)
endif

SVN_DIST_ROOT=https://dist.apache.org/repos/dist/dev/sling/ide-tooling
M2_REPO=$(HOME)/.m2/repository
P2UPDATE_PATH=$(M2_REPO)/org/apache/sling/ide/org.apache.sling.ide.p2update/$(RELEASE_VERSION)
SOURCE_BUNDLE_PATH=$(M2_REPO)/org/apache/sling/ide/org.apache.sling.ide.source-bundle/$(RELEASE_VERSION)
P2UPDATE_FILE=org.apache.sling.ide.p2update-$(RELEASE_VERSION).zip
SOURCE_BUNDLE_FILE=org.apache.sling.ide.source-bundle-$(RELEASE_VERSION).zip
STAGING_DIR=dist-staging/$(RELEASE_VERSION)

release: check-gpg release-shared release-eclipse prepare-dist-dir upload-to-dist-dev print-email-draft
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

prepare-dist-dir:
	rm -rf $(STAGING_DIR)
	mkdir -p $(STAGING_DIR)
	cp $(P2UPDATE_PATH)/$(P2UPDATE_FILE) $(STAGING_DIR)/
	cp $(SOURCE_BUNDLE_PATH)/$(SOURCE_BUNDLE_FILE) $(STAGING_DIR)/
	for ext in asc; do \
		if [ -f $(P2UPDATE_PATH)/$(P2UPDATE_FILE).$$ext ]; then cp $(P2UPDATE_PATH)/$(P2UPDATE_FILE).$$ext $(STAGING_DIR)/; fi; \
		if [ -f $(SOURCE_BUNDLE_PATH)/$(SOURCE_BUNDLE_FILE).$$ext ]; then cp $(SOURCE_BUNDLE_PATH)/$(SOURCE_BUNDLE_FILE).$$ext $(STAGING_DIR)/; fi; \
	done
	cd $(STAGING_DIR) && for f in $(P2UPDATE_FILE) $(SOURCE_BUNDLE_FILE); do \
		openssl dgst -sha512 -r $$f | awk '{print $$1"  "$$2}' > $$f.sha512; \
		openssl dgst -sha1 -r $$f | awk '{print $$1"  "$$2}' > $$f.sha1; \
		openssl dgst -md5 -r $$f | awk '{print $$1"  "$$2}' > $$f.md5; \
	done

.PHONE=prepare-dist-dir

upload-to-dist-dev:
	@if svn ls $(SVN_DIST_ROOT)/$(RELEASE_VERSION) >/dev/null 2>&1; then \
		echo "ERROR: Release version $(RELEASE_VERSION) already exists at $(SVN_DIST_ROOT)/$(RELEASE_VERSION)"; exit 1; \
	else \
		echo "SVN destination does not exist yet, proceeding with import"; \
	fi
	svn import -m "Uploading Sling IDE Tooling $(RELEASE_VERSION) release artifacts" $(STAGING_DIR) $(SVN_DIST_ROOT)/$(RELEASE_VERSION)

.PHONY=upload-to-dist-dev

clean-dist-staging:
	rm -rf dist-staging

.PHONY=clean-dist-staging

print-email-draft:

.PHONY=print-email-draft
