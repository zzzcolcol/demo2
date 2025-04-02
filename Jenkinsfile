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
    command:
    - /busybox/sh
    args:
    - -c
    - sleep 3600
    volumeMounts:
      - name: kaniko-secret
        mountPath: /kaniko/.docker
  volumes:
    - name: kaniko-secret
      secret:
        secretName: docker-hub-secret
"""
        }
    }

    environment {
        DOCKER_IMAGE = "zzzcolcol/demo2"         // DockerHub 이미지 경로
        IMAGE_TAG = "${env.BUILD_NUMBER}"        // 이미지 태그
        NAMESPACE = "jenkins"                    // 배포할 K8s 네임스페이스
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

        stage('Build and Push Docker Image') {
            steps {
                container('kaniko') {
                    sh """
                    /kaniko/executor \
                      --dockerfile=Dockerfile \
                      --context=dir://$(pwd) \
                      --destination=docker.io/${DOCKER_IMAGE}:${IMAGE_TAG}
                    """
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
