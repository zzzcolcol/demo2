pipeline {
    agent {
        kubernetes {
            yaml '''
apiVersion: v1
kind: Pod
spec:
  # ✅ 'volumes'와 'volumeMounts' 제거: 정적 Secret 대신 동적으로 토큰을 생성하므로 더 이상 필요 없습니다.
  containers:
  - name: gradle
    image: gradle:8.6.0-jdk17
    command: ['sleep']
    args: ['infinity']
  - name: kaniko
    # 💡 참고: kaniko 'debug' 이미지는 AWS CLI를 포함하고 있습니다. 일반 'executor' 이미지는 포함하지 않을 수 있습니다.
    image: gcr.io/kaniko-project/executor:debug
    command: ["sleep"]
    args: ["infinity"]
    env:
    - name: AWS_REGION
      value: ap-south-1
            '''
        }
    }

    environment {
        ECR_REPO = "120653558546.dkr.ecr.ap-south-1.amazonaws.com/my-app"
    }

    stages {
        stage('Gradle Build') {
            steps {
                container('gradle') {
                    // Git clone (주의: credentialsId는 Jenkins에 등록된 GitHub token ID여야 함)
                    git url: 'https://github.com/zzzcolcol/demo2.git',
                        branch: 'master',
                        credentialsId: "test" // 👈 실제 GitHub Credentials ID로 변경하세요.

                    script {
                        // Git ownership 오류 방지
                        sh 'git config --global --add safe.directory "${WORKSPACE}"'

                        // Git SHA 기반 태그 생성
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
                    // ✅ script 블록으로 감싸서 Groovy 변수와 쉘 스크립트를 함께 사용합니다.
                    script {
                        // ✅ withAWS를 사용하여 안전하게 AWS 자격 증명을 주입합니다.
                        withAWS(credentials: 'your-jenkins-aws-credentials-id', region: env.AWS_REGION) { // 👈 실제 AWS Credentials ID로 변경하세요.

                            echo "🔄 Getting a fresh ECR authentication token..."
                            // 1. 파이프라인 실행 시점에 새로운 ECR 인증 토큰을 동적으로 가져옵니다.
                            def ecrToken = sh(script: "aws ecr get-login-password --region ${env.AWS_REGION}", returnStdout: true).trim()
                            def ecrRegistry = "https://120653558546.dkr.ecr.ap-south-1.amazonaws.com"

                            echo "🔑 Creating Kaniko config.json for ECR..."
                            // 2. Kaniko가 사용할 인증 파일(config.json)을 동적으로 생성합니다.
                            sh """
                            mkdir -p /kaniko/.docker
                            echo '{ "auths": { "${ecrRegistry}": { "username": "AWS", "password": "${ecrToken}" } } }' > /kaniko/.docker/config.json
                            """

                            echo "🔨 Building & Pushing: ${env.IMAGE_FULL}"
                            // 3. 생성된 인증 정보로 Kaniko 빌드 및 푸시를 실행합니다.
                            sh """
                            /kaniko/executor \\
                                --dockerfile=Dockerfile \\
                                --context=dir://${WORKSPACE} \\
                                --destination=${env.IMAGE_FULL} \\
                                --destination=${env.ECR_REPO}:latest \\
                                --insecure \\
                                --skip-tls-verify
                            """
                        }
                    }
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