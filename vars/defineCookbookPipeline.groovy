import org.Helper

@groovy.transform.Field
def METADATA_FILE_BASE_NAME = 'metadata.rb'
HELPER = null
BUMPED_VERSION = null
BUMPED_MAJOR_VERSION = null
BUMPED_MAJOR_MINOR_VERSION = null

def getMetadataFileFullPath() {
    println(this.getClass())
    return "${env.WORKSPACE}/${METADATA_FILE_BASE_NAME}"
}

def extractVersionString(metadataFile) {
    def match = metadataFile =~ /(?m)^metadata ['"](.+)['"]/
    if (match) {
        return match.group(1)
    }
}

def call(options) {
    pipeline {
        agent any
        stages {
            stage('Preparation') {
                steps {
                    script {
                        HELPER = new org.Helper()
                        echo "Current commit: ${env.GIT_COMMIT}"
                        echo "Previous successful commit: ${env.GIT_PREVIOUS_SUCCESSFUL_COMMIT}"

                        def metadataFile = readFile(getMetadataFileFullPath())
                        def versionString = extractVersionString(metadataFile)
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

                        if (options.containsKey('prepare')) {
                            options['prepare']()
                        }
                    }
                }
            }
        }
    }
}
