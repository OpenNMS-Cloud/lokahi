#!/bin/sh
########################################################################################################################
##
## FOR RELEASES
##
## RUN: ./release.sh v0.0.38-dev v0.0.37-dev
## - The format is 'release.sh <current_tag> <previous_tag>'
## - The <previous_tag> is used for 
##
########################################################################################################################

set -e

CURRENT_TAG=$1
PREVIOUS_TAG=$2

echo $CURRENT_TAG " - " $PREVIOUS_TAG

# Update Branch develop
git checkout develop
git pull -r origin develop

# Update Branch release
git checkout release
git pull -r origin release
git merge develop

exit 0

echo "release-"$CURRENT_TAG

echo "Changes: " > CHANGELOG/changelog-$CURRENT_TAG.md
git log $PREVIOUS_TAG..HEAD --oneline --graph --all | grep 'Merge pull request' | sed 's/| //g' >> CHANGELOG/changelog-$CURRENT_TAG.md

git add -A

git commit -m "RELEASE $CURRENT_TAG - updated"
git push origin release

# Release has the release tag history.
git tag $CURRENT_TAG
git push origin $CURRENT_TAG

# This is to create a PR to merge into develop.
git checkout -b release-$CURRENT_TAG
git push origin release-$CURRENT_TAG

echo "

########################################################################################################################
##
## TODO: Create a PR from branch created and pushed, and review and merge to develop.
##
########################################################################################################################

"
