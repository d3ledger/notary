pipeline {
  environment {
    DOCKER_NETWORK = ''
  }
  options {
    skipDefaultCheckout()
    buildDiscarder(logRotator(numToKeepStr: '20'))
    timestamps()
  }
  agent any
  stages {
    stage ('Docker login') {
      agent { label 'd3-build-agent'}
      steps {
        script {
          def scmVars = checkout scm
          withCredentials([usernamePassword(credentialsId: 'nexus-d3-docker', usernameVariable: 'login', passwordVariable: 'password')]) {
              sh "docker login nexus.iroha.tech:19002 -u ${login} -p '${password}'"
              sh "env"
              sh "echo ${env.BRANCH_NAME}"
              if(env.BRANCH_NAME ==~ /(master|develop|reserved)/){
                sh "rm build/libs/notary-1.0-SNAPSHOT-all.jar || true"
                iC = docker.image("openjdk:8-jdk")
                iC.inside("-e JVM_OPTS='-Xmx3200m' -e TERM='dumb'") {
                  sh "./gradlew shadowJar"
                }

                sh """
                    docker build -t nexus.iroha.tech:19002/d3-deploy/eth-relay:$TAG -f eth-relay.dockerfile . \
                    docker build -t nexus.iroha.tech:19002/d3-deploy/registration:$TAG  -f registration.dockerfile . \
                    docker build -t nexus.iroha.tech:19002/d3-deploy/notary:$TAG  -f notary.dockerfile . \
                    docker build -t nexus.iroha.tech:19002/d3-deploy/withdrawal:$TAG  -f withdrawal.dockerfile . \

                    docker push nexus.iroha.tech:19002/d3-deploy/eth-relay:$TAG \
                    docker push nexus.iroha.tech:19002/d3-deploy/registration:$TAG \
                    docker push nexus.iroha.tech:19002/d3-deploy/notary:$TAG \
                    docker push nexus.iroha.tech:19002/d3-deploy/withdrawal:$TAG \

                """
              }
          }
        }
      }

    }


    stage ('Stop same job builds') {
      agent { label 'master' }
      steps {
        script {
          def scmVars = checkout scm
          // need this for develop->master PR cases
          // CHANGE_BRANCH is not defined if this is a branch build
          try {
            scmVars.CHANGE_BRANCH_LOCAL = scmVars.CHANGE_BRANCH
          }
          catch(MissingPropertyException e) { }
          if (scmVars.GIT_LOCAL_BRANCH != "develop" && scmVars.CHANGE_BRANCH_LOCAL != "develop") {
            def builds = load ".jenkinsci/cancel-builds-same-job.groovy"
            builds.cancelSameJobBuilds()
          }
        }
      }
    }
    stage('Tests') {
      agent { label 'd3-build-agent' }
      steps {
        script {
            def scmVars = checkout scm
            DOCKER_NETWORK = "${scmVars.CHANGE_ID}-${scmVars.GIT_COMMIT}-${BUILD_NUMBER}"
            writeFile file: ".env", text: "SUBNET=${DOCKER_NETWORK}"
            sh "docker-compose -f deploy/docker-compose.yml -f deploy/docker-compose.ci.yml pull"
            sh(returnStdout: true, script: "docker-compose -f deploy/docker-compose.yml -f deploy/docker-compose.ci.yml up --build -d")
            iC = docker.image("openjdk:8-jdk")
            iC.inside("--network='d3-${DOCKER_NETWORK}' -e JVM_OPTS='-Xmx3200m' -e TERM='dumb'") {
              withCredentials([file(credentialsId: 'ethereum_password.properties', variable: 'ethereum_password')]) {
                  sh "cp \$ethereum_password configs/eth/ethereum_password_local.properties"
                  sh "cp \$ethereum_password configs/eth/ethereum_password_local.properties"
              }
              sh "./gradlew dependencies"
              sh "./gradlew test --info"
              sh "./gradlew compileIntegrationTestKotlin --info"
              sh "./gradlew integrationTest --info"

            }
        }
      }
      post {
        always {
          junit 'build/test-results/**/*.xml'
        }
        cleanup {
          sh "mkdir build-logs"
          sh """
            while read -r LINE; do \
              docker logs \$(echo \$LINE | cut -d ' ' -f1) | gzip -6 > build-logs/\$(echo \$LINE | cut -d ' ' -f2).log.gz; \
            done < <(docker ps --filter "network=d3-${DOCKER_NETWORK}" --format "{{.ID}} {{.Names}}")
          """
          archiveArtifacts artifacts: 'build-logs/*.log.gz'
          sh "docker-compose -f deploy/docker-compose.yml -f deploy/docker-compose.ci.yml down"
          cleanWs()
        }
      }
    }
  }
}
