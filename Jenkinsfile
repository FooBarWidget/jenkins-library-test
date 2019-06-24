final VERSION_FILE_BASE_NAME = "version.txt"
HELPER = null
BUMPED_VERSION = null
COMMIT_AFTER_BUMP = null

def getVersionFileFullPath() {
    return "${env.WORKSPACE}/${VERSION_FILE_BASE_NAME}"
}

pipeline {
    agent any
    stages {
        stage('Preparation') {
            steps {
                script {
                    HELPER = load 'src/org/Helper.groovy'
                    echo "Current commit: ${env.GIT_COMMIT}"
                    echo "Previous successful commit: ${env.GIT_PREVIOUS_SUCCESSFUL_COMMIT}"

                    def versionString = readFile(getVersionFileFullPath())
                    def versionArray = HELPER.parseVersionString(VERSION_FILE_BASE_NAME, versionString)
                    def version = versionArray.join(".")
                    echo "Detected current version: $version"

                    def versionBumpMatrix = HELPER.calcVersionBumpMatrixFromChangeset()
                    echo "Calculated version bump plan: $versionBumpMatrix"

                    def bumpedVersionArray = HELPER.calculateBumpedVersion(versionArray, versionBumpMatrix)
                    BUMPED_VERSION = bumpedVersionArray.join(".")
                    echo "Will bump version to: $BUMPED_VERSION"
                    BUMPED_MAJOR_VERSION = bumpedVersionArray[0] as String
                    echo "Major version after bumping: $BUMPED_MAJOR_VERSION"
                    BUMPED_MAJOR_MINOR_VERSION = HELPER.formatVersionAsMajorMinorOnly(bumpedVersionArray)
                    echo "Major+minor version after bumping: $BUMPED_MAJOR_MINOR_VERSION"
                }
            }
        }
        stage('Bump version') {
            steps {
                script {
                    if (HELPER.getBranchName() == 'master') {
                        echo "echo '$BUMPED_VERSION' > ${getVersionFileFullPath()}"
                        echo "git commit -a -m 'v$BUMPED_VERSION'"
                        echo "git push"
                        echo "git tag v$BUMPED_VERSION"
                        echo "git push origin v$BUMPED_VERSION"

                        COMMIT_AFTER_BUMP = sh(
                            script: "git log --format='%H' HEAD~1..HEAD",
                            returnStdout: true
                        ).trim()
                        echo "Commit after version bumping: $COMMIT_AFTER_BUMP"
                    } else {
                        echo 'Skipping bumping version because we are not on master branch.'
                    }
                }
            }
        }
        stage('Update major branch') {
            steps {
                script {
                    if (HELPER.getBranchName() == 'master') {
                        echo "if ! [ -f .git/refs/heads/v$BUMPED_MAJOR_VERSION ]; then git branch v$BUMPED_MAJOR_VERSION; fi"
                        echo "git checkout v$BUMPED_MAJOR_VERSION"
                        echo "git reset --hard $COMMIT_AFTER_BUMP"
                        echo "git push origin v$BUMPED_MAJOR_VERSION"
                    } else {
                        echo 'Skipping updating major branch because we are not on master branch.'
                    }
                }
            }
        }
        stage('Update major+minor branch') {
            steps {
                script {
                    if (HELPER.getBranchName() == 'master') {
                        echo "if ! [ -f .git/refs/heads/v$BUMPED_MAJOR_MINOR_VERSION ]; then git branch v$BUMPED_MAJOR_MINOR_VERSION; fi"
                        echo "git checkout v$BUMPED_MAJOR_MINOR_VERSION"
                        echo "git reset --hard $COMMIT_AFTER_BUMP"
                        echo "git push origin v$BUMPED_MAJOR_MINOR_VERSION"
                    } else {
                        echo 'Skipping updating major+minor branch because we are not on master branch.'
                    }
                }
            }
        }
    }
}
