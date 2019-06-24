package org

def getBranchName() {
    def parts = env.GIT_BRANCH.split("/", 2)
    return parts[1]
}

def forEachCommitSinceLastSuccessfulBuild(block) {
    if (env.GIT_PREVIOUS_SUCCESSFUL_COMMIT != null) {
        if (env.GIT_COMMIT == env.GIT_PREVIOUS_SUCCESSFUL_COMMIT) {
            // We are rebuilding a previously successful commit.
            return;
        }
        def commits = sh(
            script: "git log --format='%H' '${env.GIT_PREVIOUS_SUCCESSFUL_COMMIT}..${env.GIT_COMMIT}'",
            returnStdout: true
        ).split('\n')
        for (commit in commits) {
            block(commit)
        }
    }
}

/*
 * A 'version bump matrix' is a 3-element boolean array that
 * describes which components in a semantic version string
 * should be bumped.
 *
 * For example let's say the current version is '2.30.1'.
 * A version string matrix [false, true, true] means
 * that the version should be bumped to '2.31.2'.
 */

def boolean[] calcVersionBumpMatrixFromCommitMessage(message) {
    def sourceBranchMessage = null

    def match = message =~ /^Merge pull request .* from (.+?) to/
    if (match) {
        sourceBranchMessage = match.group(1)
    } else {
        sourceBranchMessage = message
    }

    if (sourceBranchMessage =~ /(?m)^major\//) {
        return [true,  false, false] as boolean[]
    } else if (sourceBranchMessage =~ /(?m)^minor\//) {
        return [false, true,  false] as boolean[]
    } else if (sourceBranchMessage =~ /(?m)^(tiny|patch)\//) {
        return [false, false, true] as boolean[]
    } else {
        return [false, false, false] as boolean[]
    }
}

def mergeVersionBumpMatrices(matrix1, matrix2) {
    return [
        matrix1[0] || matrix2[0],
        matrix1[1] || matrix2[1],
        matrix1[2] || matrix2[2]
    ] as boolean[]
}

def calcVersionBumpMatrixFromChangeset() {
    def boolean[] versionBumpMatrix = [false, false, false]
    forEachCommitSinceLastSuccessfulBuild() { commit ->
        def message = sh(
            script: "git log --format='%B' '$commit~1..$commit'",
            returnStdout: true
        )
        def thisVersionBumpMatrix = calcVersionBumpMatrixFromCommitMessage(message)
        versionBumpMatrix = mergeVersionBumpMatrices(versionBumpMatrix,
            thisVersionBumpMatrix)
    }
    return versionBumpMatrix
}

def int[] parseVersionString(versionFile, version) {
    def parts = version.trim().split("\\.")
    if (parts.length != 3) {
        error("Error parsing version number '$version' in file '$versionFile':"
            + " it does not consist of exactly 3 parts.")
    }
    return [
        parts[0] as int,
        parts[1] as int,
        parts[2] as int
    ] as int[]
}

def calculateBumpedVersion(version, versionBumpMatrix) {
    def result = version.clone()
    if (versionBumpMatrix[0]) {
        result[0]++
    }
    if (versionBumpMatrix[1]) {
        result[1]++
    }
    if (versionBumpMatrix[2]) {
        result[2]++
    }
    return result
}

def formatVersionAsMajorMinorOnly(version) {
    return "${version[0]}.${version[1]}"
}

return this
