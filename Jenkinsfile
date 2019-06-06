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
                    DOCKER_NETWORK = "${scmVars.CHANGE_ID}-${scmVars.GIT_COMMIT}-${BUILD_NUMBER}"
                    writeFile file: ".env", text: "SUBNET=${DOCKER_NETWORK}"
                    withCredentials([usernamePassword(credentialsId: 'nexus-d3-docker', usernameVariable: 'login', passwordVariable: 'password')]) {
                        sh "docker login nexus.iroha.tech:19002 -u ${login} -p '${password}'"
                        sh "docker-compose -f deploy/docker-compose.yml -f deploy/docker-compose.ci.yml pull"
                        sh(returnStdout: true, script: "docker-compose -f deploy/docker-compose.yml -f deploy/docker-compose.ci.yml up --build -d")
                    }

                    iC = docker.image("openjdk:8-jdk")
                    iC.inside("--network='d3-${DOCKER_NETWORK}' -e JVM_OPTS='-Xmx3200m' -e TERM='dumb'") {
                        sh "./gradlew test --info"
                        sh "./gradlew compileIntegrationTestKotlin --info"
                        sh "./gradlew integrationTest --info"
                    }
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
