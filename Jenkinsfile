def DOCKER_IMAGE_NAME = "zzzcolcol/demo2"
def IMAGE_TAG = "${env.BUILD_NUMBER}"
def NAMESPACE = "jenkins"

podTemplate(
  label: 'kaniko-builder',
  containers: [
    containerTemplate(name: 'gradle', image: 'gradle:8-jdk17', command: 'cat', ttyEnabled: true),
    containerTemplate(name: 'kaniko', image: 'gcr.io/kaniko-project/executor:latest', command: 'cat', ttyEnabled: true),
    containerTemplate(name: 'kubectl', image: 'bitnami/kubectl:1.27', command: 'cat', ttyEnabled: true)
  ],
  volumes: [
    secretVolume(secretName: 'docker-hub-secret', mountPath: '/kaniko/.docker')
  ]
) {
  node('kaniko-builder') {

    stage('Checkout') {
      container('gradle') {
        checkout scm
      }
    }

    stage('Build with Gradle') {
      container('gradle') {
        sh 'gradle clean build -x test'
      }
    }

    stage('Docker Build and Push with Kaniko') {
      container('kaniko') {
        sh '''
          /kaniko/executor \
            --dockerfile=Dockerfile \
            --context=dir://$(pwd) \
            --destination=docker.io/zzzcolcol/demo2:${BUILD_NUMBER} \
            --verbosity=info
        '''
      }
    }

    stage('Deploy to EKS') {
      container('kubectl') {
        sh """
          sed -i.bak 's|IMAGE_PLACEHOLDER|docker.io/${DOCKER_IMAGE_NAME}:${IMAGE_TAG}|' ./k8s-deployment.yaml
          kubectl apply -f ./k8s-deployment.yaml -n ${NAMESPACE}
          kubectl apply -f ./k8s-service.yaml -n ${NAMESPACE}
        """
      }
    }
  }
}
