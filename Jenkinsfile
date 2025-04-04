pipeline{
    agent {
        kubernetes{
            yaml '''
               apiVersoin: v1
               kind: Pod
               spec:
                 containers:
                 - name: gradle
                   image: gradle:8.6.0-jdk17
                   command: ['sleep']
                   args: ['infinity']
                 - name: kaniko
                   image: gcr.io/kaniko-project/executor:debug
                   command:
                   - sleep
                   args:
                   - infinity
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
            '''
        }
    }
    stages{
        stage('gradle'){
            steps{
                container('gradle'){
                    git url: 'https://github.com/zzzcolcol/demo2.git',
                    branch: 'master',
                    credentialsId: "github-token"
                    
                    sh 'gradle bootJar'
                    sh 'ls -al'
                    sh 'mv ./build/libs/xxx.jar ./'
                }
            }
        }
        stage('docker'){
            steps{
                container('kaniko'){
                    sh "executor --dockerfile=Dockerfile --context=dir://${env.WORKSPACE} --destination=120653558546.dkr.ecr.ap-northeast-2.amazonaws.com/zzzcolcol/test:latest"
                }
            }
            post{
                success{
                    echo 'success Build & Push'
                }
                failure{
                    echo 'failure Build & Push'
                }
            }
        }
    }
}