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
                        withAWS(credentials: 'test1', region: env.AWS_REGION) { // 👈 실제 ID로 변경했는지 확인
                            echo "🔄 Getting a fresh ECR authentication token..."
                            def ecrToken = sh(script: "aws ecr get-login-password --region ${env.AWS_REGION}", returnStdout: true).trim()
                            def ecrRegistry = "https://120653558546.dkr.ecr.${env.AWS_REGION}.amazonaws.com"

                            echo "🔑 Creating config.json in shared volume..."
                            // 👇 3. 공유 볼륨 경로에 config.json 파일을 생성합니다.
                            writeFile(
                                file: '/shared-config/config.json',
                                text: """{ "auths": { "${ecrRegistry}": { "username": "AWS", "password": "${ecrToken}" } } }"""
                            )
                        }
                    }

                    container('kaniko') {
                        // 4. kaniko 컨테이너는 공유 볼륨을 통해 자동으로 config.json을 인식합니다.
                        echo "🔨 Building & Pushing: ${env.IMAGE_FULL} AND ${env.ECR_REPO}:latest"
                        
                        // 👇 5. 잘못된 플래그(--docker-config, --custom-dir)를 완전히 제거합니다.
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