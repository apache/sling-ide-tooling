# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

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

release: check-gpg release-shared release-eclipse prepare-dist-dir upload-to-dist-dev
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
	cd eclipse && mvn --batch-mode versions:set-property -DgenerateBackupPoms=false -Dproperty=sling-ide.shared-deps.version -DnewVersion=$(RELEASE_VERSION) && git add pom.xml && git commit -m 'chore(deps): set shared-deps version to $(RELEASE_VERSION) for release'
	cd eclipse && mvn --batch-mode release:prepare release:perform -DreleaseVersion=$(RELEASE_VERSION) -DdevelopmentVersion=$(NEXT_VERSION) -Dtag=sling-ide-tooling-eclipse-$(RELEASE_VERSION)
	cd eclipse && mvn --batch-mode versions:set-property -DgenerateBackupPoms=false -Dproperty=sling-ide.shared-deps.version -DnewVersion=$(NEXT_VERSION) && git add pom.xml && git commit -m 'chore(deps): set shared-deps version to $(NEXT_VERSION) after the release'

.PHONY=release-eclipse

prepare-dist-dir:
	rm -rf $(STAGING_DIR)
	mkdir -p $(STAGING_DIR)
	cp $(P2UPDATE_PATH)/$(P2UPDATE_FILE) $(STAGING_DIR)/
	cp $(SOURCE_BUNDLE_PATH)/$(SOURCE_BUNDLE_FILE) $(STAGING_DIR)/
	cp $(P2UPDATE_PATH)/$(P2UPDATE_FILE).asc $(STAGING_DIR)/
	cp $(SOURCE_BUNDLE_PATH)/$(SOURCE_BUNDLE_FILE).asc $(STAGING_DIR)/
	cd $(STAGING_DIR) && for f in $(P2UPDATE_FILE) $(SOURCE_BUNDLE_FILE); do \
		openssl dgst -sha512 -r $$f | awk '{print $$1}' > $$f.sha512; \
		openssl dgst -sha1 -r $$f | awk '{print $$1}' > $$f.sha1; \
		openssl dgst -md5 -r $$f | awk '{print $$1}' > $$f.md5; \
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
