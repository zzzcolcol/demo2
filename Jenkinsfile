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
    # 👇 2. kaniko 컨테이너에 공유 볼륨을 마운트합니다.
    # Kaniko가 기본적으로 config.json을 찾는 경로입니다.
    volumeMounts:
    - name: kaniko-docker-config
      mountPath: /kaniko/.docker
  - name: aws-cli
    image: amazon/aws-cli:latest
    command: ["sleep"]
    args: ["infinity"]
    # 👇 1. aws-cli 컨테이너에 공유 볼륨을 마운트합니다.
    volumeMounts:
    - name: kaniko-docker-config
      mountPath: /shared-config
  # ✅ Pod 내에서 컨테이너들이 공유할 수 있는 임시 볼륨을 정의합니다.
  volumes:
  - name: kaniko-docker-config
    emptyDir: {}
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
        stage('Docker Build & Push') {
            steps {
                script {
                    container('aws-cli') {
                        withAWS(credentials: 'test1', region: env.AWS_REGION) {
                            echo "🔄 Getting a fresh ECR authentication token and writing config..."
                            
                            // 👇 writeFile 대신 sh 명령어로 직접 파일을 생성합니다.
                            // 이 스크립트 전체가 aws-cli 컨테이너 안에서 실행됩니다.
                            sh """
                            # 1. ECR 토큰을 쉘 변수에 저장합니다.
                            TOKEN=\$(aws ecr get-login-password --region ${env.AWS_REGION})
                            ECR_REGISTRY="https://120653558546.dkr.ecr.${env.AWS_REGION}.amazonaws.com"
                            
                            # 2. echo와 heredoc(EOF)을 사용해 공유 볼륨에 config.json 파일을 직접 씁니다.
                            echo "Writing config to /shared-config/config.json"
                            cat > /shared-config/config.json <<EOF
                            {
                              "auths": {
                                "\${ECR_REGISTRY}": {
                                  "username": "AWS",
                                  "password": "\${TOKEN}"
                                }
                              }
                            }
                            EOF
                            """
                        }
                    }

                    container('kaniko') {
                        echo "🔨 Building & Pushing image..."
                        sh """
                        /kaniko/executor \\
                          --dockerfile=Dockerfile \\
                          --context=dir://${WORKSPACE} \\
                          --destination=${env.IMAGE_FULL} \\
                          --destination=${env.ECR_REPO}:latest
                        """
                    }
                }
            }
            post {
                success {
                    echo "✅ SUCCESS: Image pushed -> ${env.IMAGE_FULL}, ${env.ECR_REPO}:latest"
                }
                failure {
                    echo '❌ FAILURE: Docker Build & Push 실패'
                }
            }
        }
    }
  
}

