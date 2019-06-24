import org.Helper

@groovy.transform.Field static final METADATA_FILE_BASE_NAME = 'metadata.rb'
@groovy.transform.Field HELPER = null
@groovy.transform.Field BUMPED_VERSION = null
@groovy.transform.Field BUMPED_MAJOR_VERSION = null
@groovy.transform.Field BUMPED_MAJOR_MINOR_VERSION = null

private def getMetadataFileFullPath() {
    return "${env.WORKSPACE}/${METADATA_FILE_BASE_NAME}"
}

private def extractVersionString(metadataFile) {
    def match = metadataFile =~ /(?m)^version ['"](.+)['"]/
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

                        checkout scm
                        // clean the workspace before we begin, if we don't do this it keep files
                        // like Berksfile.lock and that might give conflicts when running `berks install`
                        sh 'git clean -dXf'
                        if (fileExists('Gemfile')) {
                            sh 'bundle install -j 4 --path=$HOME/bundle'
                        }
                        //sh 'berks install'

                        def metadataFile = readFile(getMetadataFileFullPath())
                        def versionString = extractVersionString(metadataFile)
                        def versionArray = HELPER.parseVersionString(METADATA_FILE_BASE_NAME, versionString)
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
            stage('Tests') {
                steps {
                    script {
                        def usesBundler = fileExists('Gemfile')
                        def shRubyCommand = { command ->
                            if (usesBundler) {
                                sh "bundle exec ${command}"
                            } else {
                                sh command
                            }
                        }

                        def tests = {
                            foodcritic: {
                                shRubyCommand 'foodcritic .'
                            }
                        }

                        /* if (fileExists('spec')) {
                            tests['rspec'] = {
                                if (usesBundler) {
                                    sh 'bundle exec rspec -f d'
                                } else {
                                    sh 'chef exec rspec -f d'
                                }
                            }
                        }

                        if (fileExists('.kitchen.yml') || fileExists('kitchen.yml')) {
                            tests['kitchen'] = {
                                try {
                                    shRubyCommand 'kitchen converge'
                                    shRubyCommand 'kitchen verify'
                                } finally {
                                    shRubyCommand 'kitchen destroy'
                                }
                            }
                        } */

                        if (options.containsKey('tests')) {
                            tests += options['tests']
                        }

                        parallel(tests)
                    }
                }
            }
        }
    }
}
