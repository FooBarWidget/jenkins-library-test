VERSION_FILE_BASE_NAME = "version.txt"
BUMPED_VERSION = null
COMMIT_AFTER_BUMP = null

def getVersionFileFullPath() {
	return "${env.WORKSPACE}/${VERSION_FILE_BASE_NAME}"
}

def getBranchName() {
	def parts = env.GIT_BRANCH.split("/", 2)
	return parts[1]
}

def forEachCommitSinceLastSuccessfulBuild(block) {
	if (env.GIT_PREVIOUS_SUCCESSFUL_COMMIT != null) {
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

	if (sourceBranchMessage.matches(/(?m)^major\//)) {
		return [true,  false, false] as boolean[]
	} else if (sourceBranchMessage.matches(/(?m)^minor\//)) {
		return [false, true,  false] as boolean[]
	} else if (sourceBranchMessage.matches(/(?m)^(tiny|patch)\//)) {
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

pipeline {
	agent any
	stages {
		stage('Preparation') {
			steps {
				script {
					sh 'env | sort'
					if (getBranchName() == 'master') {
						echo "Current commit: ${env.GIT_COMMIT}"
						echo "Previous successful commit: ${env.GIT_PREVIOUS_SUCCESSFUL_COMMIT}"

						def versionString = readFile(getVersionFileFullPath())
						def versionArray = parseVersionString(VERSION_FILE_BASE_NAME, versionString)
						def version = versionArray.join(".")
						echo "Detected current version: $version"

						def versionBumpMatrix = calcVersionBumpMatrixFromChangeset()
						echo "Calculated version bump plan: $versionBumpMatrix"

						def bumpedVersionArray = calculateBumpedVersion(versionArray, versionBumpMatrix)
						BUMPED_VERSION = bumpedVersionArray.join(".")
						echo "Will bump version to: $BUMPED_VERSION"
						BUMPED_MAJOR_VERSION = bumpedVersionArray[0] as String
						echo "Major version after bumping: $BUMPED_MAJOR_VERSION"
						BUMPED_MAJOR_MINOR_VERSION = formatVersionAsMajorMinorOnly(bumpedVersionArray)
						echo "Major+minor version after bumping: $BUMPED_MAJOR_MINOR_VERSION"
					}
				}
			}
		}
		stage('Bump version') {
			steps {
				script {
					echo "echo '$BUMPED_VERSION' > version.txt"
					echo "git commit -a -m 'v$BUMPED_VERSION'"
					echo "git push"
					echo "git tag v$BUMPED_VERSION"
					echo "git push v$BUMPED_VERSION"

					COMMIT_AFTER_BUMP = sh(
						script: "git log --format='%H' HEAD~1..HEAD",
						returnStdout: true
					).trim()
					echo "Commit after version bumping: $COMMIT_AFTER_BUMP"
				}
			}
		}
		stage('Update major branch') {
			steps {
				echo "if ! [ -f .git/refs/heads/v$BUMPED_MAJOR_VERSION ]; then git branch v$BUMPED_MAJOR_VERSION; fi"
				echo "git checkout v$BUMPED_MAJOR_VERSION"
				echo "git reset --hard $COMMIT_AFTER_BUMP"
				echo "git push origin v$BUMPED_MAJOR_VERSION"
			}
		}
		stage('Update major+minor branch') {
			steps {
				echo "if ! [ -f .git/refs/heads/v$BUMPED_MAJOR_MINOR_VERSION ]; then git branch v$BUMPED_MAJOR_MINOR_VERSION; fi"
				echo "git checkout v$BUMPED_MAJOR_MINOR_VERSION"
				echo "git reset --hard $COMMIT_AFTER_BUMP"
				echo "git push origin v$BUMPED_MAJOR_MINOR_VERSION"
			}
		}
	}
}
