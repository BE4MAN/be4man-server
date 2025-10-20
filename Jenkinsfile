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
                        sshagent(credentials: [env.VM_SSH_CRED_ID]) {
                            def imageTag = "${env.DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER}"
                            sh """
ssh -o StrictHostKeyChecking=no ${env.VM_USER}@${env.VM_HOST_IP} /bin/bash <<'ENDSSH'
set -e

# 네트워크 생성(이미 있으면 무시)
docker network create be4man-network || true

# --- Redis 준비 (없으면 생성, 있으면 시작) ---
if ! docker inspect my-redis >/dev/null 2>&1; then
  echo "Redis container not found. Creating my-redis on be4man-network..."
  docker run -d --name my-redis --network be4man-network redis
else
  if [ "$(docker inspect -f '{{.State.Running}}' my-redis)" != "true" ]; then
    echo "Starting existing my-redis container..."
    docker start my-redis
  else
    echo "my-redis is already running."
  fi
fi

# 기존 앱 컨테이너 중지 및 삭제
docker stop be4man_app || true
docker rm be4man_app || true

# 이미지 풀
docker pull ${imageTag}

# 앱 컨테이너 실행 (옵션들이 IMAGE보다 앞에 오도록)
docker run -d --name be4man_app -p 8080:8080 --network be4man-network \
  -e DB_URL="${env.DB_URL}" \
  -e DB_USERNAME="${env.DB_USERNAME}" \
  -e DB_PASSWORD="${env.DB_PASSWORD}" \
  -e DB_SCHEMA="${env.DB_SCHEMA}" \
  -e GITHUB_CLIENT_ID="${env.GITHUB_CLIENT_ID}" \
  -e GITHUB_CLIENT_SECRET="${env.GITHUB_CLIENT_SECRET}" \
  -e JWT_SECRET="${env.JWT_SECRET}" \
  -e FRONTEND_URL="${env.FRONTEND_URL}" \
  -e REDIS_HOST="my-redis" \
  -e REDIS_PORT="6379" \
  -e REDIS_URL="redis://my-redis:6379" \
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
