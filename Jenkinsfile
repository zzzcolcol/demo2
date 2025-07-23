pipeline {
  agent {
    kubernetes {
      yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: gradle
    image: gradle:8.6.0-jdk17
    command: ['sleep']
    args: ['infinity']
  - name: kaniko
    image: gcr.io/kaniko-project/executor:debug
    command: ["sleep"]
    args: ["infinity"]
    env:
    - name: AWS_REGION
      value: ap-south-1
    volumeMounts:
    - name: registry-credentials
      mountPath: /kaniko/.docker
  volumes:
  - name: registry-credentials
    secret:
      secretName: my-reg
      items:
      - key: .dockerconfigjson
        path: config.json
      optional: false
      '''
    }
  }

  environment {
    ECR_REPO = '120653558546.dkr.ecr.ap-south-1.amazonaws.com/my-app'
  }

  stages {
    stage('Gradle Build') {
      steps {
        container('gradle') {
          // Git clone (주의: credentialsId는 Jenkins에 등록된 GitHub token ID여야 함)
          git url: 'https://github.com/zzzcolcol/demo2.git',
              branch: 'master',
              credentialsId: "test"

          script {
            // ✅ Git ownership 오류 방지
            sh 'git config --global --add safe.directory "${WORKSPACE}"'

            // ✅ Git SHA 기반 태그 생성
            def gitSha = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
            env.IMAGE_TAG = "v-${gitSha}"
            env.IMAGE_FULL = "${env.ECR_REPO}:${env.IMAGE_TAG}"
          }

          // Gradle 빌드
          sh 'gradle clean bootJar -x test'
          sh 'mv $(find build/libs -name "*.jar" | head -n 1) ./app.jar'
        }
      }
    }

    stage('Docker Build & Push') {
      steps {
        container('kaniko') {
          echo "🔨 Building & Pushing: ${env.IMAGE_FULL}"
          sh """
            /kaniko/executor \
              --dockerfile=Dockerfile \
              --context=dir://${WORKSPACE} \
              --destination=${env.IMAGE_FULL} \
              --destination=${env.ECR_REPO}:latest \
              --insecure \
              --skip-tls-verify
          """
        }
      }
      post {
        success {
          echo "✅ SUCCESS: Image pushed -> ${env.IMAGE_FULL}"
        }
        failure {
          echo '❌ FAILURE: Docker Build & Push 실패'
        }
      }
    }
  }
}
