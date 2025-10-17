// Jenkinsfile (최종 실행 코드)
pipeline {
    agent any

    environment {
        VM_HOST_IP = '34.64.239.74'
        VM_USER = 'jenkins-gcp-key'
        VM_SSH_CRED_ID = 'vm-ssh-credentials'

        DOCKER_CRED_ID = 'dockerhub-credentials'
        DOCKER_IMAGE_NAME = 'yoonyn/be4man-server'
    }

    stages {
        stage('Checkout') {
            steps {
                echo "Checking out code..."
            }
        }

        stage('Build') {
            steps {
                echo "Starting Gradle Build..."
                sh 'chmod +x ./gradlew'
                sh './gradlew clean build -x test'
            }
        }

        stage('Package and Push to Docker Hub') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: env.DOCKER_CRED_ID,
                                                    passwordVariable: 'DOCKER_PASS',
                                                    usernameVariable: 'DOCKER_USER')]) {

                        def IMAGE_TAG = "${env.DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER}"

                        sh "docker build -t ${IMAGE_TAG} ."
                        sh "echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin"

                        // 네트워크 불안정 대비 재시도 로직 추가
                        retry(3) {
                            sh "docker push ${IMAGE_TAG}"
                        }
                    }
                }
            }
        }

        stage('Deploy to VM') {
            steps {
                script {
                  withCredentials([string(credentialsId: 'db_password', variable: 'DB_PASSWORD'),
                   string(credentialsId: 'db_url', variable: 'DB_URL'),
                   string(credentialsId: 'db_username', variable: 'DB_USERNAME'),
                   string(credentialsId: 'db_schema', variable: 'DB_SCHEMA')]
                   ) {
                    sshagent(credentials: [env.VM_SSH_CRED_ID]) {
                        sh """
                            # Host Key 검사를 무시하고 SSH 접속 (-o StrictHostKeyChecking=no)
                            ssh -o StrictHostKeyChecking=no ${env.VM_USER}@${env.VM_HOST_IP} '

                                # 1. 기존 컨테이너 중지 및 삭제 (오류 무시: || true)
                                docker stop be4man_app || true
                                docker rm be4man_app || true

                                # 2. Docker Hub에서 새 이미지 Pull
                                docker pull ${env.DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER}

                                # 3. 새 컨테이너 실행 (환경 변수 주입)
                                docker run -d \\
                                --name be4man_app \\
                                -p 8080:8080 \\
                                -e DB_URL="${DB_URL}" \\
                                -e DB_USERNAME="${DB_USERNAME}" \\
                                -e DB_PASSWORD="${DB_PASSWORD}" \\
                                -e DB_SCHEMA="${DB_SCHEMA}" \\
                                ${env.DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER}
                            '
                        """
                    }
                  }
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline finished with status: ${currentBuild.result}"
        }
    }
}