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
                  sh "cp \$ethereum_password src/main/resources/eth/ethereum_password.properties"
                  sh "cp \$ethereum_password notary-integration-test/src/integration-test/resources/eth/ethereum_password.properties"
              }
              withCredentials([file(credentialsId: 'ethereum_password.properties', variable: 'bitcoin_wallet')]) {
                  sh "cp \$bitcoin_wallet deploy/bitcoin/test-btc.wallet"
              }
              sh "./gradlew dependencies"
              // sh "./gradlew test --info"
              // sh "./gradlew compileIntegrationTestKotlin --info"
              sh "./gradlew integrationTest --info"
            }
        }
      }
      post {
        always {
          junit 'build/test-results/test/*.xml'
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
