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
                    sh "#!/bin/sh\n./gradlew build --info"
                }
            }
        }
        stage('Test') {
            steps {
                script {
                    sh "#!/bin/sh\n./gradlew test --info"
                    sh "./gradlew compileIntegrationTestKotlin --info"
                    sh "./gradlew integrationTest --info"
                }
            }
        }
        stage('Build artifacts') {
            steps {
                script {
                    if (env.BRANCH_NAME ==~ /(master|develop|reserved)/ || env.TAG_NAME) {
                        TAG = env.TAG_NAME ? env.TAG_NAME : env.BRANCH_NAME
                        withCredentials([usernamePassword(credentialsId: 'nexus-d3-docker', usernameVariable: 'login', passwordVariable: 'password')]) {
                            iC = docker.image("gradle:4.10.2-jdk8-slim")
                            iC.inside(" -e JVM_OPTS='-Xmx3200m' -e TERM='dumb'"+
                                " -v /var/run/docker.sock:/var/run/docker.sock -v /tmp:/tmp"+
                                " -e DOCKER_REGISTRY_URL='https://nexus.iroha.tech:19002'"+
                                " -e DOCKER_REGISTRY_USERNAME='${login}'"+
                                " -e DOCKER_REGISTRY_PASSWORD='${password}'"+
                                " -e TAG='${TAG}'") {
                                sh "gradle shadowJar"
                                sh "gradle dockerPush"
                            }
                        }
                    }
                }
            }
        }
    }
    post {
        cleanup {
            cleanWs()
        }
    }
}
