pipeline {
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
      agent { label 'x86_64' }
      steps {
        script {
            def scmVars = checkout scm
            writeFile file: ".env", text: "SUBNET=-${scmVars.GIT_COMMIT}-${BUILD_NUMBER}"
            sh(returnStdout: true, script: "docker-compose -f deploy/docker-compose-dev.yml up --build -d")
            iC = docker.image("openjdk:8-jdk")
            iC.inside("--network='d3-${scmVars.GIT_COMMIT}-${BUILD_NUMBER}' -e JVM_OPTS='-Xmx3200m' -e TERM='dumb'") {
              withCredentials([file(credentialsId: 'ethereum_password.properties', variable: 'ethereum_password')]) {
                  sh "cp \$ethereum_password src/main/resources/eth/ethereum_password.properties"
                  sh "cp \$ethereum_password src/integration-test/resources/eth/ethereum_password.properties"
              }
              sh(script: "./gradlew dependencies")
              sh(script: "./gradlew test --info")
              sh(script: "./gradlew compileIntegrationTestKotlin --info")
              sh(script: "./gradlew integrationTest --info")
            }
        }
      }
      post {
        cleanup {
          sh(script: "docker-compose -f deploy/docker-compose-dev.yml down")
          cleanWs()
        }
      }
    }
  }
}
