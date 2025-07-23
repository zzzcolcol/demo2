pipeline {
    agent {
        kubernetes {
            yaml '''
apiVersion: v1
kind: Pod
spec:
  # ✅ 'aws-cli' 컨테이너를 추가하여 AWS 관련 명령어를 전담시킵니다.
  containers:
  - name: gradle
    image: gradle:8.6.0-jdk17
    command: ['sleep']
    args: ['infinity']
  - name: kaniko
    image: gcr.io/kaniko-project/executor:debug
    command: ["sleep"]
    args: ["infinity"]
  - name: aws-cli
    image: amazon/aws-cli:latest
    command: ["sleep"]
    args: ["infinity"]
            '''
        }
    }

    environment {
        // ✅ 파이프라인 전역에서 사용할 수 있도록 환경 변수를 이곳에 정의합니다.
        AWS_REGION = 'ap-south-1'
        ECR_REPO = "120653558546.dkr.ecr.${AWS_REGION}.amazonaws.com/my-app"
    }

    stages {
        stage('Gradle Build') {
            steps {
                container('gradle') {
                    git url: 'https://github.com/zzzcolcol/demo2.git',
                        branch: 'master',
                        credentialsId: "test" // 👈 실제 GitHub Credentials ID로 변경

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

        stage('Docker Build & Push') {
            steps {
                script {
                    // ✅ 1. 'aws-cli' 컨테이너에서 ECR 인증 토큰을 생성합니다.
                    container('aws-cli') {
                        // 'withAWS'를 사용하여 AWS 자격 증명을 이 컨테이너에 주입합니다.
                        withAWS(credentials: 'test1', region: env.AWS_REGION) { // 👈 실제 AWS Credentials ID로 변경
                            echo "🔄 Getting a fresh ECR authentication token..."
                            def ecrToken = sh(script: "aws ecr get-login-password --region ${env.AWS_REGION}", returnStdout: true).trim()
                            def ecrRegistry = "https://120653558546.dkr.ecr.${env.AWS_REGION}.amazonaws.com"

                            echo "🔑 Creating Kaniko config.json in shared workspace..."
                            // ✅ Kaniko가 접근할 수 있도록 공유 워크스페이스에 config.json 파일을 생성합니다.
                            writeFile(
                                file: 'kaniko-config.json',
                                text: """{ "auths": { "${ecrRegistry}": { "username": "AWS", "password": "${ecrToken}" } } }"""
                            )
                        }
                    }

                    // ✅ 2. 'kaniko' 컨테이너에서 이미지를 빌드하고 푸시합니다.
                    container('kaniko') {
                        echo "🔨 Building & Pushing: ${env.IMAGE_FULL}"
                        // '--docker-config' 플래그를 사용하여 생성된 인증 파일의 위치를 알려줍니다.
                        sh """
                        /kaniko/executor \\
                           --dockerfile=Dockerfile \\
                           --context=dir://${WORKSPACE} \\
                           --destination=${env.IMAGE_FULL} \\
                           --destination=${env.ECR_REPO}:latest \\
                           --docker-config=dir://${WORKSPACE} \\
                           --custom-dir=kaniko-config.json
                        """
                         // --insecure, --skip-tls-verify 플래그는 제거하는 것을 권장합니다.
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