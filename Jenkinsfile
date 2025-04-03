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
      command: ['cat']
      tty: true
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
        IMAGE_TAG = "${BUILD_NUMBER}"
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
                    sh """
                      /kaniko/executor \
                        --dockerfile=Dockerfile \
                        --context=dir://\$(pwd) \
                        --destination=docker.io/${DOCKER_IMAGE}:\${BUILD_NUMBER} \
                        --verbosity=info
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
