pipeline {
    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timestamps()
    }
    agent {
        docker {
            label 'd3-build-agent'
            image 'openjdk:8-jdk-alpine'
            args '-v /var/run/docker.sock:/var/run/docker.sock -v /tmp:/tmp'
        }
    }
    stages {
        stage('Build') {
            steps {
                script {
                    sh "./gradlew composeUp --info --stacktrace"
                    sh "./gradlew build --info"
                    sh "./gradlew composeDown --info --stacktrace"
                }
            }
        }
        stage('Test') {
            steps {
                script {
                    sh "./gradlew integrationTest --info --stacktrace"
                }
            }
        }
        stage('Build artifacts') {
            steps {
                script {
                    if (env.BRANCH_NAME ==~ /(master|develop)/ || env.TAG_NAME) {
                        DOCKER_TAGS = ['master': 'latest', 'develop': 'develop']
                        withCredentials([usernamePassword(credentialsId: 'nexus-d3-docker', usernameVariable: 'login', passwordVariable: 'password')]) {
                          env.DOCKER_REGISTRY_URL = "https://nexus.iroha.tech:19002"
                          env.DOCKER_REGISTRY_USERNAME = "${login}"
                          env.DOCKER_REGISTRY_PASSWORD = "${password}"
                          env.TAG = env.TAG_NAME ? env.TAG_NAME : DOCKER_TAGS[env.BRANCH_NAME]
                          sh "./gradlew dockerPush"
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            publishHTML (target: [
                allowMissing: false,
                alwaysLinkToLastBuild: false,
                keepAll: true,
                reportDir: 'build/reports',
                reportFiles: 'd3-test-report.html',
                reportName: "D3 test report"
            ])
        }
        cleanup {
            cleanWs()
        }
    }
}