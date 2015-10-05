#!/bin/sh
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
VERSION=${1}
DOWNLOAD=${2:-/tmp/sling-staging}
mkdir ${DOWNLOAD} 2>/dev/null

if [ -z "${VERSION}" -o ! -d "${DOWNLOAD}" ]
then
 echo "Usage: check_staged_release.sh <version> [temp-directory]"
 exit
fi

if [ ! -e "${DOWNLOAD}/eclipse/${VERSION}" ]
then
 mkdir -p "${DOWNLOAD}/eclipse/${VERSION}"
 echo "################################################################################"
 echo "                           DOWNLOADING ARTIFACTS                                "
 echo "################################################################################"

 wget -e "robots=off" --wait 1 -nv -r -np "--reject=html,txt" "--follow-tags=" \
  -P "${DOWNLOAD}/eclipse/${VERSION}" -nH "--cut-dirs=3" \
  "https://dist.apache.org/repos/dist/dev/sling/ide-tooling-${VERSION}"

else
 echo "################################################################################"
 echo "                       USING EXISTING STAGED REPOSITORY                         "
 echo "################################################################################"
 echo "${DOWNLOAD}/eclipse/${VERSION}"
fi

echo "################################################################################"
echo "                          CHECK SIGNATURES AND DIGESTS                          "
echo "################################################################################"

for i in `find "${DOWNLOAD}/eclipse/${VERSION}" -type f | grep -v '\.\(asc\|sha1\|md5\)$'`
do
 f=`echo $i | sed 's/\.asc$//'`
 echo "$f"
 gpg --verify $f.asc 2>/dev/null
 if [ "$?" = "0" ]; then CHKSUM="GOOD"; else CHKSUM="BAD!!!!!!!!"; fi
 if [ ! -f "$f.asc" ]; then CHKSUM="----"; fi
 echo "gpg:  ${CHKSUM}"

 for tp in md5 sha1
 do
   if [ ! -f "$f.$tp" ]
   then
     CHKSUM="----"
   else
     A="`cat $f.$tp 2>/dev/null`"
     B="`openssl $tp < $f 2>/dev/null | sed 's/.*= *//' `"
     if [ "$A" = "$B" ]; then CHKSUM="GOOD (`cat $f.$tp`)"; else CHKSUM="BAD!! : $A not equal to $B"; fi
   fi
   echo "$tp : ${CHKSUM}"
 done

done

if [ -z "${CHKSUM}" ]; then echo "WARNING: no files found!"; fi

echo "################################################################################"


