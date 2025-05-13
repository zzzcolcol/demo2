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

  stages {
    stage('Gradle Build') {
      steps {
        container('gradle') {
          git url: 'https://github.com/zzzcolcol/demo2.git',
              branch: 'master',
              credentialsId: "github-token"

          sh 'gradle clean bootJar -x test'
          sh 'mv $(find build/libs -name "*.jar" | head -n 1) ./app.jar'
        }
      }
    }

    stage('Docker Build & Push') {
      steps {
        container('kaniko') {
          sh '''
            /kaniko/executor \
              --dockerfile=Dockerfile \
              --context=dir://${WORKSPACE} \
              --destination=120653558546.dkr.ecr.ap-south-1.amazonaws.com/my-app:latest \
              --insecure \
              --skip-tls-verify
          '''
        }
      }
      post {
        success {
          echo '✅ SUCCESS: Build & Push 완료!'
        }
        failure {
          echo '❌ FAILURE: Build & Push 실패'
        }
      }
    }
  }
}
