
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
    stage('Stop same job builds') {
      agent { label 'master' }
      steps {
        script {
          def scmVars = checkout scm
          // need this for develop->master PR cases
          // CHANGE_BRANCH is not defined if this is a branch build
          try {
            scmVars.CHANGE_BRANCH_LOCAL = scmVars.CHANGE_BRANCH
          }
          catch (MissingPropertyException e) {
          }
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
          sh "docker cp d3-btc-node0-${DOCKER_NETWORK}:/usr/bin/bitcoin-cli deploy/bitcoin/"
          iC = docker.image("openjdk:8-jdk")
          iC.inside("--network='d3-${DOCKER_NETWORK}' -e JVM_OPTS='-Xmx3200m' -e TERM='dumb'") {
            sh "ln -s deploy/bitcoin/bitcoin-cli /usr/bin/bitcoin-cli"
            withCredentials([file(credentialsId: 'ethereum_password.properties', variable: 'ethereum_password')]) {
              sh "cp \$ethereum_password configs/eth/ethereum_password_local.properties"
              sh "cp \$ethereum_password configs/eth/ethereum_password_local.properties"
            }
            sh "./gradlew dependencies"
            sh "./gradlew dokka"
//            sh "./gradlew test --info"
//            sh "./gradlew compileIntegrationTestKotlin --info"
//            sh "./gradlew integrationTest --info"
//            sh "./gradlew codeCoverageReport --info"
            // sh "./gradlew pitest --info"
            withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
              sh(script: """./gradlew sonarqube --configure-on-demand \
                -Dsonar.host.url=https://sonar.soramitsu.co.jp \
                -Dsonar.login=${SONAR_TOKEN} \
              """)
            }
          }
          // scan smartcontracts only on pull requests to master
          try {
            if (env.CHANGE_TARGET == "master") {
              docker.image("mythril/myth").inside("--entrypoint=''") {
                sh "echo 'Smart contracts scan results' > mythril.txt"
                // using mythril to scan all solidity files
                sh "find . -name '*.sol' -exec myth --execution-timeout 900 --create-timeout 900 -x {} \\; | tee mythril.txt"
              }
              // save results as a build artifact
              zip archive: true, dir: '', glob: 'mythril.txt', zipFile: 'smartcontracts-scan-results.zip'
            }
          }
          catch(MissingPropertyException e) { }
          
        }
      }
      post {
        always {
          junit allowEmptyResults: true, keepLongStdio: true, testResults: 'build/test-results/**/*.xml'
          jacoco execPattern: 'build/jacoco/test.exec', sourcePattern: '.'
        }
        cleanup {
          sh "mkdir build-logs"
          sh """
            while read -r LINE; do \
              docker logs \$(echo \$LINE | cut -d ' ' -f1) | gzip -6 > build-logs/\$(echo \$LINE | cut -d ' ' -f2).log.gz; \
            done < <(docker ps --filter "network=d3-${DOCKER_NETWORK}" --format "{{.ID}} {{.Names}}")
          """
          
          sh "tar -zcvf build-logs/jacoco.log.gz build/reports/jacoco/*"
          archiveArtifacts artifacts: 'build-logs/*.log.gz'
          sh "docker-compose -f deploy/docker-compose.yml -f deploy/docker-compose.ci.yml down"
          cleanWs()
        }
      }
    }

    stage('Build and push docker images') {
      agent { label 'd3-build-agent' }
      steps {
        script {
          def scmVars = checkout scm
          if (env.BRANCH_NAME ==~ /(master|develop|reserved)/) {
            withCredentials([usernamePassword(credentialsId: 'nexus-d3-docker', usernameVariable: 'login', passwordVariable: 'password')]) {
              sh "docker login nexus.iroha.tech:19002 -u ${login} -p '${password}'"

              TAG = env.BRANCH_NAME
              sh "rm build/libs/notary-1.0-SNAPSHOT-all.jar || true"
              iC = docker.image("openjdk:8-jdk")
              iC.inside("-e JVM_OPTS='-Xmx3200m' -e TERM='dumb'") {
                sh "./gradlew shadowJar"
              }
              relay = docker.build("nexus.iroha.tech:19002/${login}/eth-relay:${TAG}", "-f docker/eth-relay.dockerfile .")
              registration = docker.build("nexus.iroha.tech:19002/${login}/registration:${TAG}", "-f docker/registration.dockerfile .")
              notary = docker.build("nexus.iroha.tech:19002/${login}/notary:${TAG}", "-f docker/notary.dockerfile .")
              withdrawal = docker.build("nexus.iroha.tech:19002/${login}/withdrawal:${TAG}", "-f docker/withdrawal.dockerfile .")

              relay.push("${TAG}")
              registration.push("${TAG}")
              notary.push("${TAG}")
              withdrawal.push("${TAG}")

            }
          }
        }
      }
    }
  }
}

