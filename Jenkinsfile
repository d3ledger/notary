// Overall pipeline looks like the following
//         
//   |--Linux-----|----Debug
//   |            |----Release 
//   |    OR
//   |           
//-- |--Linux ARM-|----Debug
//   |            |----Release
//   |    OR
//   |
//   |--MacOS-----|----Debug
//   |            |----Release
properties([parameters([
  choice(choices: 'Debug\nRelease', description: '', name: 'BUILD_TYPE'),
  booleanParam(defaultValue: true, description: '', name: 'Linux'),
  booleanParam(defaultValue: false, description: '', name: 'ARMv7'),
  booleanParam(defaultValue: false, description: '', name: 'ARMv8'),
  booleanParam(defaultValue: true, description: '', name: 'MacOS'),
  booleanParam(defaultValue: false, description: 'Whether build docs or not', name: 'Doxygen'),
  booleanParam(defaultValue: false, description: 'Whether build Java bindings', name: 'JavaBindings'),
  choice(choices: 'Release\nDebug', description: 'Java Bindings Build Type', name: 'JBBuildType'),
  booleanParam(defaultValue: false, description: 'Whether build Python bindings', name: 'PythonBindings'),
  choice(choices: 'Release\nDebug', description: 'Python Bindings Build Type', name: 'PBBuildType'),
  choice(choices: 'python3\npython2', description: 'Python Bindings Version', name: 'PBVersion'),
  booleanParam(defaultValue: false, description: 'Whether build Android bindings', name: 'AndroidBindings'),
  choice(choices: '26\n25\n24\n23\n22\n21\n20\n19\n18\n17\n16\n15\n14', description: 'Android Bindings ABI Version', name: 'ABABIVersion'),
  choice(choices: 'Release\nDebug', description: 'Android Bindings Build Type', name: 'ABBuildType'),
  choice(choices: 'arm64-v8a\narmeabi-v7a\narmeabi\nx86_64\nx86', description: 'Android Bindings Platform', name: 'ABPlatform'),
  string(defaultValue: '4', description: 'How much parallelism should we exploit. "4" is optimal for machines with modest amount of memory and at least 4 cores', name: 'PARALLELISM')])])


pipeline {
  environment {
    CCACHE_DIR = '/opt/.ccache'
    CCACHE_RELEASE_DIR = '/opt/.ccache-release'
    SORABOT_TOKEN = credentials('SORABOT_TOKEN')
    SONAR_TOKEN = credentials('SONAR_TOKEN')
    GIT_RAW_BASE_URL = "https://raw.githubusercontent.com/hyperledger/iroha"

    IROHA_NETWORK = "iroha-0${CHANGE_ID}-${GIT_COMMIT}-${BUILD_NUMBER}"
    IROHA_POSTGRES_HOST = "pg-0${CHANGE_ID}-${GIT_COMMIT}-${BUILD_NUMBER}"
    IROHA_POSTGRES_USER = "pguser${GIT_COMMIT}"
    IROHA_POSTGRES_PASSWORD = "${GIT_COMMIT}"
    IROHA_POSTGRES_PORT = 5432
  }

  options {
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  agent any
  stages {
    stage ('Stop same job builds') {
      agent { label 'master' }
      steps {
        script {
          if (GIT_LOCAL_BRANCH != "develop") {
            def builds = load ".jenkinsci/cancel-builds-same-job.groovy"
            builds.cancelSameJobBuilds()
          }
        }
      }
    }
    stage('Build Debug') {
      when {
        expression { params.BUILD_TYPE == 'Debug' }
      }
      parallel {
        stage ('Linux') {
          when {
            beforeAgent true
            expression { return params.Linux }
          }
          agent { label 'x86_64' }
          steps {
            script {
              debugBuild = load ".jenkinsci/debug-build.groovy"
              coverage = load ".jenkinsci/selected-branches-coverage.groovy"
              if (coverage.selectedBranchesCoverage(['develop', 'master'])) {
                debugBuild.doDebugBuild(true)
              }
              else {
                debugBuild.doDebugBuild()
              }
              if (GIT_LOCAL_BRANCH ==~ /(master|develop)/) {
                releaseBuild = load ".jenkinsci/release-build.groovy"
                releaseBuild.doReleaseBuild()
              }
            }
          }
          post {
            always {
              script {
                post = load ".jenkinsci/linux-post-step.groovy"
                post.linuxPostStep()
              }
            }
          }
        }
        stage('ARMv7') {
          when {
            beforeAgent true
            expression { return params.ARMv7 } 
          }
          agent { label 'armv7' }
          steps {
            script {
              debugBuild = load ".jenkinsci/debug-build.groovy"
              coverage = load ".jenkinsci/selected-branches-coverage.groovy"
              if (!params.Linux && !params.ARMv8 && !params.MacOS && (coverage.selectedBranchesCoverage(['develop', 'master']))) {
                debugBuild.doDebugBuild(true)
              }              
              else {
                debugBuild.doDebugBuild()
              }
              if (GIT_LOCAL_BRANCH ==~ /(master|develop)/) {
                releaseBuild = load ".jenkinsci/release-build.groovy"
                releaseBuild.doReleaseBuild()
              }
            }
          }
          post {
            always {
              script {
                post = load ".jenkinsci/linux-post-step.groovy"
                post.linuxPostStep()
              }
            }
          }
        }
        stage('ARMv8') {
          when {
            beforeAgent true
            expression { return params.ARMv8 } 
          }
          agent { label 'armv8' }
          steps {
            script {
              debugBuild = load ".jenkinsci/debug-build.groovy"
              coverage = load ".jenkinsci/selected-branches-coverage.groovy"
              if (!params.Linux && !params.MacOS && (coverage.selectedBranchesCoverage(['develop', 'master']))) {
                debugBuild.doDebugBuild(true)
              }
              else {
                debugBuild.doDebugBuild()
              }
              if (GIT_LOCAL_BRANCH ==~ /(master|develop)/) {
                releaseBuild = load ".jenkinsci/release-build.groovy"
                releaseBuild.doReleaseBuild()
              }
            }
          }
          post {
            always {
              script {
                post = load ".jenkinsci/linux-post-step.groovy"
                post.linuxPostStep()
              }
            }
          }
        }
        stage('MacOS'){
          when {
            beforeAgent true
            expression { return params.MacOS }
          }
          agent { label 'mac' }
          steps {
            script {
              def coverageEnabled = false
              def cmakeOptions = ""
              coverage = load ".jenkinsci/selected-branches-coverage.groovy"
              if (!params.Linux && (coverage.selectedBranchesCoverage(['develop', 'master']))) {
                coverageEnabled = true
                cmakeOptions = " -DCOVERAGE=ON "
              }
              def scmVars = checkout scm
              env.IROHA_VERSION = "0x${scmVars.GIT_COMMIT}"
              env.IROHA_HOME = "/opt/iroha"
              env.IROHA_BUILD = "${env.IROHA_HOME}/build"

              sh """
                ccache --version
                ccache --show-stats
                ccache --zero-stats
                ccache --max-size=5G
              """
              sh """
                cmake \
                  -DTESTING=ON \
                  -H. \
                  -Bbuild \
                  -DCMAKE_BUILD_TYPE=${params.BUILD_TYPE} \
                  -DIROHA_VERSION=${env.IROHA_VERSION} \
                  ${cmakeOptions}
              """
              sh "cmake --build build -- -j${params.PARALLELISM}"
              sh "ccache --show-stats"
              if ( coverageEnabled ) {
                sh "cmake --build build --target coverage.init.info"
              }
              sh """
                export IROHA_POSTGRES_PASSWORD=${IROHA_POSTGRES_PASSWORD}; \
                export IROHA_POSTGRES_USER=${IROHA_POSTGRES_USER}; \
                mkdir -p /var/jenkins/${GIT_COMMIT}-${BUILD_NUMBER}; \
                initdb -D /var/jenkins/${GIT_COMMIT}-${BUILD_NUMBER}/ -U ${IROHA_POSTGRES_USER} --pwfile=<(echo ${IROHA_POSTGRES_PASSWORD}); \
                pg_ctl -D /var/jenkins/${GIT_COMMIT}-${BUILD_NUMBER}/ -o '-p 5433' -l /var/jenkins/${GIT_COMMIT}-${BUILD_NUMBER}/events.log start; \
                psql -h localhost -d postgres -p 5433 -U ${IROHA_POSTGRES_USER} --file=<(echo create database ${IROHA_POSTGRES_USER};)
              """
              def testExitCode = sh(script: 'IROHA_POSTGRES_HOST=localhost IROHA_POSTGRES_PORT=5433 cmake --build build --target test', returnStatus: true)
              if (testExitCode != 0) {
                currentBuild.currentResult = "UNSTABLE"
              }
              if ( coverageEnabled ) {
                sh "cmake --build build --target cppcheck"
                // Sonar
                if (env.CHANGE_ID != null) {
                  sh """
                    sonar-scanner \
                      -Dsonar.github.disableInlineComments \
                      -Dsonar.github.repository='hyperledger/iroha' \
                      -Dsonar.analysis.mode=preview \
                      -Dsonar.login=${SONAR_TOKEN} \
                      -Dsonar.projectVersion=${BUILD_TAG} \
                      -Dsonar.github.oauth=${SORABOT_TOKEN}
                  """
                }
                sh "cmake --build build --target coverage.info"
                sh "python /usr/local/bin/lcov_cobertura.py build/reports/coverage.info -o build/reports/coverage.xml"
                cobertura autoUpdateHealth: false, autoUpdateStability: false, coberturaReportFile: '**/build/reports/coverage.xml', conditionalCoverageTargets: '75, 50, 0', failUnhealthy: false, failUnstable: false, lineCoverageTargets: '75, 50, 0', maxNumberOfBuilds: 50, methodCoverageTargets: '75, 50, 0', onlyStable: false, zoomCoverageChart: false
              }
              if (GIT_LOCAL_BRANCH ==~ /(master|develop)/) {
                releaseBuild = load ".jenkinsci/mac-release-build.groovy"
                releaseBuild.doReleaseBuild()
              }
            }
          }
          post {
            always {
              script {
                timeout(time: 600, unit: "SECONDS") {
                  try {
                    if (currentBuild.currentResult == "SUCCESS" && GIT_LOCAL_BRANCH ==~ /(master|develop)/) {
                      def artifacts = load ".jenkinsci/artifacts.groovy"
                      def commit = env.GIT_COMMIT
                      filePaths = [ '\$(pwd)/build/*.tar.gz' ]
                      artifacts.uploadArtifacts(filePaths, sprintf('/iroha/macos/%1$s-%2$s-%3$s', [GIT_LOCAL_BRANCH, sh(script: 'date "+%Y%m%d"', returnStdout: true).trim(), commit.substring(0,6)]))                        
                    }
                  }
                  finally {
                    cleanWs()
                    sh """
                      pg_ctl -D /var/jenkins/${GIT_COMMIT}-${BUILD_NUMBER}/ stop && \
                      rm -rf /var/jenkins/${GIT_COMMIT}-${BUILD_NUMBER}/
                    """
                  }
                }
              }
            }
          }
        }
      }
    }
    stage('Build Release') {
      when {
        expression { params.BUILD_TYPE == 'Release' }      
      }
      parallel {
        stage('Linux') {
          when {
            beforeAgent true
            expression { return params.Linux }
          }
          agent { label 'x86_64' }
          steps {
            script {
              def releaseBuild = load ".jenkinsci/release-build.groovy"
              releaseBuild.doReleaseBuild()
            }
          }
          post {
            always {
              script {
                post = load ".jenkinsci/linux-post-step.groovy"
                post.linuxPostStep()
              }
            }
          }
        }
        stage('ARMv7') {
          when {
            beforeAgent true
            expression { return params.ARMv7 }
          }
          agent { label 'armv7' }
          steps {
            script {
              def releaseBuild = load ".jenkinsci/release-build.groovy"
              releaseBuild.doReleaseBuild()
            }
          }
          post {
            always {
              script {
                post = load ".jenkinsci/linux-post-step.groovy"
                post.linuxPostStep()
              }
            }
          }           
        }
        stage('ARMv8') {
          when {
            beforeAgent true
            expression { return params.ARMv8 }
          }
          agent { label 'armv8' }
          steps {
            script {
              def releaseBuild = load ".jenkinsci/release-build.groovy"
              releaseBuild.doReleaseBuild()
            }
          }
          post {
            always {
              script {
                post = load ".jenkinsci/linux-post-step.groovy"
                post.linuxPostStep()
              }
            }
          }          
        }
        stage('MacOS') {
          when {
            beforeAgent true
            expression { return params.MacOS }
          }
          agent { label 'mac' }
          steps {
            script {
              def releaseBuild = load ".jenkinsci/mac-release-build.groovy"
              releaseBuild.doReleaseBuild()
            }
          }
          post {
            always {
              script {
                timeout(time: 600, unit: "SECONDS") {
                  try {
                    if (currentBuild.currentResult == "SUCCESS" && GIT_LOCAL_BRANCH ==~ /(master|develop)/) {
                      def artifacts = load ".jenkinsci/artifacts.groovy"
                      def commit = env.GIT_COMMIT
                      filePaths = [ '\$(pwd)/build/*.tar.gz' ]
                      artifacts.uploadArtifacts(filePaths, sprintf('/iroha/macos/%1$s-%2$s-%3$s', [GIT_LOCAL_BRANCH, sh(script: 'date "+%Y%m%d"', returnStdout: true).trim(), commit.substring(0,6)]))
                    }
                  }
                  finally {
                    cleanWs()
                  }
                }
              }
            }
          }
        }
      }
    }
    stage('Build docs') {
      when {
        beforeAgent true
        allOf {
          expression { return params.Doxygen }
          expression { GIT_LOCAL_BRANCH ==~ /(master|develop)/ }
        }
      }
      // build docs on any vacant node. Prefer `x86_64` over
      // others as nodes are more powerful
      agent { label 'x86_64 || arm' }
      steps {
        script {
          def doxygen = load ".jenkinsci/doxygen.groovy"
          docker.image("${env.DOCKER_IMAGE}").inside {
            def scmVars = checkout scm
            doxygen.doDoxygen()
          }
        }
      }
    }
    stage('Build bindings') {
      when {
        beforeAgent true
        anyOf {
          expression { return params.PythonBindings }
          expression { return params.JavaBindings }
          expression { return params.AndroidBindings }
        }
      }
      agent { label 'x86_64' }
      environment {
        JAVA_HOME = '/usr/lib/jvm/java-8-oracle'
      }
      steps {
        script {
          def bindings = load ".jenkinsci/bindings.groovy"
          def dPullOrBuild = load ".jenkinsci/docker-pull-or-build.groovy"
          def pCommit = load ".jenkinsci/previous-commit.groovy"
          def platform = sh(script: 'uname -m', returnStdout: true).trim()
          def previousCommit = pCommit.previousCommitOrCurrent()
          if (params.JavaBindings) {
            iC = dPullOrBuild.dockerPullOrUpdate("$platform-develop-build",
                                                 "${env.GIT_RAW_BASE_URL}/${env.GIT_COMMIT}/docker/develop/Dockerfile",
                                                 "${env.GIT_RAW_BASE_URL}/${previousCommit}/docker/develop/Dockerfile",
                                                 "${env.GIT_RAW_BASE_URL}/develop/docker/develop/Dockerfile",
                                                 ['PARALLELISM': params.PARALLELISM])
            iC.inside("-v /tmp/${env.GIT_COMMIT}/bindings-artifact:/tmp/bindings-artifact") {
              bindings.doJavaBindings(params.JBBuildType)
            }
          }
          if (params.PythonBindings) {
            iC = dPullOrBuild.dockerPullOrUpdate("$platform-develop-build",
                                                 "${env.GIT_RAW_BASE_URL}/${env.GIT_COMMIT}/docker/develop/Dockerfile",
                                                 "${env.GIT_RAW_BASE_URL}/${previousCommit}/docker/develop/Dockerfile",
                                                 "${env.GIT_RAW_BASE_URL}/develop/docker/develop/Dockerfile",
                                                 ['PARALLELISM': params.PARALLELISM])
            iC.inside("-v /tmp/${env.GIT_COMMIT}/bindings-artifact:/tmp/bindings-artifact") {
              bindings.doPythonBindings(params.PBBuildType)
            }
          }
          if (params.AndroidBindings) {
            iC = dPullOrBuild.dockerPullOrUpdate("android-${params.ABPlatform}-${params.ABBuildType}",
                                                 "${env.GIT_RAW_BASE_URL}/${env.GIT_COMMIT}/docker/android/Dockerfile",
                                                 "${env.GIT_RAW_BASE_URL}/${previousCommit}/docker/android/Dockerfile",
                                                 "${env.GIT_RAW_BASE_URL}/develop/docker/android/Dockerfile",
                                                 ['PARALLELISM': params.PARALLELISM, 'PLATFORM': params.ABPlatform, 'BUILD_TYPE': params.ABBuildType])
            sh "curl -L -o /tmp/${env.GIT_COMMIT}/entrypoint.sh ${env.GIT_RAW_BASE_URL}/${env.GIT_COMMIT}/docker/android/entrypoint.sh"
            sh "chmod +x /tmp/${env.GIT_COMMIT}/entrypoint.sh"
            iC.inside("-v /tmp/${env.GIT_COMMIT}/entrypoint.sh:/entrypoint.sh:ro -v /tmp/${env.GIT_COMMIT}/bindings-artifact:/tmp/bindings-artifact") {
              bindings.doAndroidBindings(params.ABABIVersion)
            }
          }          
        }
      }
      post {
        always {
          timeout(time: 600, unit: "SECONDS") {
            script {
              try {
                if (currentBuild.currentResult == "SUCCESS") {
                  def artifacts = load ".jenkinsci/artifacts.groovy"
                  def commit = env.GIT_COMMIT
                  if (params.JavaBindings) {
                    javaBindingsFilePaths = [ '/tmp/${GIT_COMMIT}/bindings-artifact/java-bindings-*.zip' ]
                    artifacts.uploadArtifacts(javaBindingsFilePaths, '/iroha/bindings/java')
                  }
                  if (params.PythonBindings) {
                    pythonBindingsFilePaths = [ '/tmp/${GIT_COMMIT}/bindings-artifact/python-bindings-*.zip' ]
                    artifacts.uploadArtifacts(pythonBindingsFilePaths, '/iroha/bindings/python')
                  }
                  if (params.AndroidBindings) {
                    androidBindingsFilePaths = [ '/tmp/${GIT_COMMIT}/bindings-artifact/android-bindings-*.zip' ]
                    artifacts.uploadArtifacts(androidBindingsFilePaths, '/iroha/bindings/android')
                  }
                }
              }
              finally {
                sh "rm -rf /tmp/${env.GIT_COMMIT}"
                cleanWs()
              }
            }
          }
        }
      }
    }
  }
}
