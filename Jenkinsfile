pipeline {
  agent {
    label 'jenkins-jenkins-agent'  // 또는 사용하는 에이전트 라벨
  }

  environment {
    GRADLE_USER_HOME = "${WORKSPACE}/.gradle" // 캐시 디렉토리
  }

  tools {
    // Jenkins에 Gradle tool 설정해뒀다면 사용 가능
    gradle 'gradle-8'  // Jenkins 관리 > Global Tool Configuration
  }

  stages {
    stage('Checkout') {
      steps {
        git url: 'https://github.com/zzzcolcol/demo2.git', branch: 'master'
      }
    }

    stage('Build') {
      steps {
        sh './gradlew clean build'
      }
    }
  }

  post {
    success {
      echo '🎉 Build Success!'
    }
    failure {
      echo '❌ Build Failed!'
    }
  }
}
