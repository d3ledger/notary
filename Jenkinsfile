pipeline {
  options {
    buildDiscarder(logRotator(numToKeepStr: '20'))
    timestamps()
  }
  agent any
  stages {
    stage ('Stop same job builds') {
      agent { label 'master' }
      steps {
        script {
          // need this for develop->master PR cases
          // CHANGE_BRANCH is not defined if this is a branch build
          try {
            CHANGE_BRANCH_LOCAL = env.CHANGE_BRANCH
          }
          catch(MissingPropertyException e) { }
          if (GIT_LOCAL_BRANCH != "develop" && CHANGE_BRANCH_LOCAL != "develop") {
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
            iC = docker.image('openjdk:8-jdk')
            iC.inside('-e JVM_OPTS="-Xmx3200m" -e TERM="dumb"') {
              sh(script: "./gradlew dependencies")
              sh(script: ".circleci/copy_bindings.sh")
              sh(script: "./gradlew test --info")
              sh(script: "./gradlew compileIntegrationTestKotlin --info")

              // var = sh(returnStatus:true, script: "CYPRESS_baseUrl=http://d3-back-office:8080 CYPRESS_IROHA=http://grpcwebproxy:8080 cypress run")
              // if (var != 0) {
              //   echo '[FAILURE] E2E tests failed'
              //   currentBuild.result = 'FAILURE';
              //   return var
              // }
            }
        }
      }
      post {
        cleanup {
          // sh(script: "docker-compose -f docker/docker-compose.yaml down")
          cleanWs()
        }
      }
    }
  }
}
