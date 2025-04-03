pipeline {
    agent {
        kubernetes {
            label 'kaniko-builder'
            defaultContainer 'jnlp'
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
    - name: gradle
      image: gradle:8-jdk17
      command: ['cat']
      tty: true
    - name: kaniko
      image: gcr.io/kaniko-project/executor:latest
      args:
        - --dockerfile=Dockerfile
        - --context=dir://\\$(pwd)
        - --destination=docker.io/zzzcolcol/demo2:\\${BUILD_NUMBER}
        - --verbosity=info
      volumeMounts:
        - name: kaniko-secret
          mountPath: /kaniko/.docker
    - name: kubectl
      image: bitnami/kubectl:1.27
      command: ['cat']
      tty: true
  volumes:
    - name: kaniko-secret
      secret:
        secretName: docker-hub-secret
"""
        }
    }

    environment {
        DOCKER_IMAGE = "zzzcolcol/demo2"
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        NAMESPACE = "jenkins"
    }

    stages {
        stage('Checkout') {
            steps {
                container('gradle') {
                    checkout scm
                }
            }
        }

        stage('Build') {
            steps {
                container('gradle') {
                    sh 'gradle clean build -x test'
                }
            }
        }

        stage('Docker Build and Push with Kaniko') {
            steps {
                container('kaniko') {
                    echo "Kaniko가 DockerHub로 이미지를 푸시 중입니다..."
                    // 실제 빌드는 container args로 실행됨
                }
            }
        }

        stage('Deploy to EKS') {
            steps {
                container('kubectl') {
                    sh """
                    sed -i.bak 's|IMAGE_PLACEHOLDER|docker.io/${DOCKER_IMAGE}:${IMAGE_TAG}|' ./k8s-deployment.yaml
                    kubectl apply -f ./k8s-deployment.yaml -n ${NAMESPACE}
                    kubectl apply -f ./k8s-service.yaml -n ${NAMESPACE}
                    """
                }
            }
        }
    }
}
