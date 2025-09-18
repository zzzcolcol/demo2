pipeline {
    agent {
        kubernetes {
            yaml '''
apiVersion: v1
kind: Pod
spec:
  # ✅ 1. Pod Identity(IRSA)와 연결된 서비스 계정을 지정합니다.
  serviceAccountName: jenkins-agent 
  containers:
  - name: gradle
    image: gradle:8.6.0-jdk17
    command: ['sleep']
    args: ['infinity']
  - name: kaniko
    image: gcr.io/kaniko-project/executor:debug
    command: ["sleep"]
    args: ["infinity"]
# ❌ 2. aws-cli 컨테이너와 공유 볼륨이 더 이상 필요 없으므로 제거했습니다.
'''
        }
    }

    environment {
        AWS_REGION = 'ap-south-1'
        ECR_REPO = "120653558546.dkr.ecr.${AWS_REGION}.amazonaws.com/my-app"
    }

    stages {
        stage('Gradle Build') {
            steps {
                container('gradle') {
                    git url: 'https://github.com/zzzcolcol/demo2.git',
                        branch: 'master',
                        credentialsId: "test"

                    script {
                        sh 'git config --global --add safe.directory "${WORKSPACE}"'
                        def gitSha = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        env.IMAGE_TAG = "v-${gitSha}"
                        env.IMAGE_FULL = "${env.ECR_REPO}:${env.IMAGE_TAG}"
                    }

                    sh 'gradle clean bootJar -x test'
                    sh 'mv $(find build/libs -name "*.jar" | head -n 1) ./app.jar'
                }
            }
        }
        
        // ✅ 3. Docker Build & Push 단계를 대폭 간소화했습니다.
        stage('Docker Build & Push') {
            steps {
                container('kaniko') {
                    echo "🔨 Building & Pushing image with Pod Identity..."
                    sh """
                    /kaniko/executor \\
                      --dockerfile=Dockerfile \\
                      --context=dir://${WORKSPACE} \\
                      --destination=${env.IMAGE_FULL}
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