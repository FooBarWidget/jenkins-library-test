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
	if (message.matches(/^major\//)) {
		return [true,  false, false] as boolean[]
	} else if (message.matches(/^minor\//)) {
		return [false, true,  false] as boolean[]
	} else if (message.matches(/^(tiny|patch)\//)) {
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
	def int[] versionBumpMatrix = [0, 0, 0]
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

def parseVersionString(versionFile, version) {
	def parts = version.split("\\.")
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

def getBranchName() {
	def parts = env.GIT_BRANCH.split("/", 2)
	return parts[1]
}

@NonCPS
def int[] readVersion() {
	return parseVersionString("version.txt",
		readFile("${env.WORKSPACE}/version.txt").trim())
}

pipeline {
	agent any
	stages {
		stage('Preparation') {
			steps {
				script {
					sh 'env | sort'
					if (getBranchName() == 'master') {
						echo "Current commit: ${env.GIT_COMMIT}\n"
							+ "Previous successful commit: ${env.GIT_PREVIOUS_SUCCESSFUL_COMMIT}"
						def version = readVersion()
						def versionBumpMatrix = calcVersionBumpMatrixFromChangeset()
						echo "version = ${version}"
						echo "versionBumpMatrix = ${versionBumpMatrix}"
					}
				}
			}
		}
	}
}
