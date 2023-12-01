#!/usr/bin/env bash
########################################################################################################################
##
## FOR RELEASES
##
## RUN: ./release.sh v0.0.38-dev v0.0.37-dev
## - The format is 'release.sh <current_tag> <previous_tag>'
## - The <previous_tag> is used for 
##
########################################################################################################################

# https://github.com/olivergondza/bash-strict-mode
set -eEuo pipefail
trap 's=$?; echo >&2 "$0: Error on line "$LINENO": $BASH_COMMAND"; exit $s' ERR

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

PULL_FLAGS="--ff-only"

DRYRUN=0

if [ $# -gt 0 ] && [ "$1" == "-n" ]; then
    DRYRUN=1
    shift
fi

if [ $# -eq 2 ]; then
    PREVIOUS_TAG="$1"; shift
    CURRENT_TAG="$1"; shift
elif [ $# -eq 1 ]; then
    PREVIOUS_TAG=""
    CURRENT_TAG="$1"; shift
elif [ $# -eq 0 ]; then
    PREVIOUS_TAG=""
    CURRENT_TAG=""
else
    echo "$(basename $0): error: incorrect number of command-line arguments" >&2
    echo "usage: $(basename $0) [-n] [[<PREVIOUS_TAG>] <CURRENT_TAG>]" >&2
    echo "options:" >&2
    echo "    -n  Dry-run mode; don't do 'git push'es. Will still locally create commit, tag, and merge to release." >&2
    exit 1
fi

if [ -z "${PREVIOUS_TAG}" ]; then
    PREVIOUS_TAG=$(git tag --sort=creatordate | tail -1)
    if [ -z "${PREVIOUS_TAG}" ]; then
        echo "$(basename $0): error: could not get previous tag from 'git tag --sort=creatordate | tail -1'" >&2
        exit 1
    fi
fi

if [ -z "${CURRENT_TAG}" ]; then
    VERSION_SUFFIX=$(sed 's/^v[0-9.]*//' <<<"${PREVIOUS_TAG}")
    VERSION_WITHOUT_SUFFIX=$(sed 's/^\(v[0-9.]*\).*/\1/' <<<"${PREVIOUS_TAG}")
    VERSION_WITHOUT_PATCH=$(sed 's/^\(v[0-9.]*\)\.\([0-9]*\)$/\1/' <<<"${VERSION_WITHOUT_SUFFIX}")
    VERSION_PATCH=$(sed 's/^\(v[0-9.]*\)\.\([0-9]*\)$/\2/' <<<"${VERSION_WITHOUT_SUFFIX}")
    NEW_PATCH=$((${VERSION_PATCH} + 1))
    CURRENT_TAG="${VERSION_WITHOUT_PATCH}.${NEW_PATCH}${VERSION_SUFFIX}"

    echo $PREVIOUS_TAG $VERSION_SUFFIX $VERSION_WITHOUT_SUFFIX $VERSION_WITHOUT_PATCH $VERSION_PATCH $NEW_PATCH $CURRENT_TAG
fi

# go to the top-level of the repo
cd ${DIR}/../..

echo $PREVIOUS_TAG " - " $CURRENT_TAG

# Update Branch develop
git checkout develop
git pull $PULL_FLAGS origin develop

# Update Branch release
git checkout release
git pull $PULL_FLAGS origin release

git merge --no-edit develop

CHANGELOG="CHANGELOG/changelog-$CURRENT_TAG.md"

echo "Changes:" > $CHANGELOG
# 2023-11-03: I removed --all because it kept on bringing in old merges
git log $PREVIOUS_TAG..HEAD --oneline --graph | grep 'Merge pull request' | sed 's/| //g' >> $CHANGELOG

git add $CHANGELOG
git commit -m "RELEASE $CURRENT_TAG - updated" $CHANGELOG

if [ $DRYRUN -eq 0 ]; then
    git push origin release
fi

# Release has the release tag history.
git tag $CURRENT_TAG
if [ $DRYRUN -eq 0 ]; then
    git push origin $CURRENT_TAG
fi
