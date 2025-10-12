// Jenkinsfile
pipeline {
    agent any

    // 전역 환경 변수 정의
    environment {
        // [VM 정보]
        VM_HOST_IP = '34.47.78.17'                // 배포 대상 VM의 공인 IP 주소
        VM_USER = 'yunsangjo59'                  // VM 접속 사용자 이름
        VM_SSH_CRED_ID = 'vm-ssh-credentials'    // 2단계 A. SSH Credentials ID

        // [Docker Hub 정보]
        DOCKER_CRED_ID = 'dockerhub-credentials'    // Docker Hub Username/Password Credential ID
        DOCKER_IMAGE_NAME = 'yoonyn/BE4MEN-server' // Docker Hub 경로로 변경

    }

    stages {
        stage('Checkout') { steps { echo "Checking out code..." } }

        stage('Build') {
            steps {
                echo "Starting Maven/Gradle Build..."
                sh './gradlew clean build -x test' // JAR 파일 생성
            }
        }

        stage('Test') {
            steps {
                echo "Running Unit Tests..."
                sh './gradlew test'
            }
        }

        stage('Package and Push to Docker Hub') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: env.DOCKER_CRED_ID,
                                                    passwordVariable: 'DOCKER_PASS',
                                                    usernameVariable: 'DOCKER_USER')]) {

                        def IMAGE_TAG = "${env.DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER}"

                        // Docker 이미지 빌드 및 태그 지정
                        sh "docker build -t ${IMAGE_TAG} ."

                        // Docker Hub 로그인 및 푸시
                        sh "echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin"
                        sh "docker push ${IMAGE_TAG}"
                    }
                }
            }
        }

        stage('Deploy to VM') {
            steps {
                script {
                    // SSH Private Key를 임시 파일로 가져옴
                    withCredentials([sshUserPrivateKey(credentialsId: env.VM_SSH_CRED_ID, keyFileVariable: 'SSH_KEY_PATH')]) {

                        def IMAGE_TAG = "${env.DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER}"

                        // ⭐️ SSH 접속 후 VM에서 Docker 명령 실행
                        sh """
                            ssh -i ${SSH_KEY_PATH} ${env.VM_USER}@${env.VM_HOST_IP} "
                                # 1. 기존 컨테이너 중지 및 삭제 (이전 버전 정리)
                                docker stop be4man_app || true
                                docker rm be4man_app || true

                                # 2. Docker Hub에서 새 이미지 Pull
                                docker pull ${IMAGE_TAG}

                                # 3. 새 컨테이너 실행 (VM의 메모리/세션 사용 가능)
                                docker run -d \
                                    --name be4man_app \
                                    -p 8080:8080 \
                                    ${IMAGE_TAG}
                            "
                        """
                        echo "Service deployed to VM ${env.VM_HOST_IP}."
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

