def call(block) {
    pipeline {
        agent 'any'
        stages {
            stage('Stage 1') {
                steps {
                    echo "Hello world"
                }
            }
            if (block != null) {
                block()
            }
        }
    }
}
