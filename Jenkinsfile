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
                checkout scm
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
                    // withCredentials 블록 바깥에서 미리 env 값 할당해도 되지만
                    // withCredentials 내부에서 env.* 가 바인딩되므로 아래처럼 하세요.
                    withCredentials([
                        string(credentialsId: 'db_password', variable: 'DB_PASSWORD'),
                        string(credentialsId: 'db_url', variable: 'DB_URL'),
                        string(credentialsId: 'db_username', variable: 'DB_USERNAME'),
                        string(credentialsId: 'db_schema', variable: 'DB_SCHEMA'),
                        string(credentialsId: 'github_client_id', variable: 'GITHUB_CLIENT_ID'),
                        string(credentialsId: 'github_client_secret', variable: 'GITHUB_CLIENT_SECRET'),
                        string(credentialsId: 'jwt_secret', variable: 'JWT_SECRET'),
                        string(credentialsId: 'frontend_url', variable: 'FRONTEND_URL')
                    ]) {
                        // env 값을 로컬 변수로 복사 -> Groovy 파서 혼선을 피함
                        def vmUser = env.VM_USER
                        def vmHost = env.VM_HOST_IP
                        def imageTag = "${env.DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER}"
                        def dbUrl = env.DB_URL
                        def dbUsername = env.DB_USERNAME
                        def dbPassword = env.DB_PASSWORD
                        def dbSchema = env.DB_SCHEMA
                        def githubClientId = env.GITHUB_CLIENT_ID
                        def githubClientSecret = env.GITHUB_CLIENT_SECRET
                        def jwtSecret = env.JWT_SECRET
                        def frontendUrl = env.FRONTEND_URL
        
                        sshagent(credentials: [env.VM_SSH_CRED_ID]) {
                            sh """
ssh -o StrictHostKeyChecking=no ${vmUser}@${vmHost} /bin/bash <<'ENDSSH'
set -e    
# 네트워크 생성(이미 있으면 무시)
docker network create be4man-network || true      
# Redis 준비 (없으면 생성, 있으면 시작)
if ! docker inspect my-redis >/dev/null 2>&1; then
    echo "Redis container not found. Creating my-redis..."
    docker run -d --name my-redis --network be4man-network redis
else
    if [ "$(docker inspect -f '{{.State.Running}}' my-redis)" != "true" ]; then
    docker start my-redis
    fi
fi
        
# 기존 앱 컨테이너 중지 및 삭제
docker stop be4man_app || true
docker rm be4man_app || true

# 이미지 pull
docker pull ${imageTag}   

# 앱 컨테이너 실행 (옵션 -> IMAGE 순서)
docker run -d --name be4man_app -p 8080:8080 --network be4man-network \
    -e DB_URL="${dbUrl}" \
    -e DB_USERNAME="${dbUsername}" \
    -e DB_PASSWORD="${dbPassword}" \
    -e DB_SCHEMA="${dbSchema}" \
    -e GITHUB_CLIENT_ID="${githubClientId}" \
    -e GITHUB_CLIENT_SECRET="${githubClientSecret}" \
    -e JWT_SECRET="${jwtSecret}" \
    -e FRONTEND_URL="${frontendUrl}" \
    -e REDIS_HOST="my-redis" \
    -e REDIS_PORT="6379" \
    ${imageTag}
        
# 상태 확인 및 로그 일부 출력
docker ps -f name=be4man_app --format "table {{.ID}}\t{{.Image}}\t{{.Status}}"
sleep 5
docker logs --tail 50 be4man_app || true      
ENDSSH
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
