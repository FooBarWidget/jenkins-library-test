def call(options) {
    pipeline {
        agent any
        // Stages are pre-defined, but within a step you can call arbitrary code.
        stages {
            stage('Stage 1') {
                steps {
                    script {
                        echo "Hello world"
                        if (options.containsKey('stage_1_block')) {
                            options['stage_1_block']()
                        }
                    }
                }
                post {
                    script {
                        echo "Hello world post"
                        if (options.containsKey('stage_1_post_block')) {
                            options['stage_1_post_block']()
                        }
                    }
                }
            }
        }
    }
}
