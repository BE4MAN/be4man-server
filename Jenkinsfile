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
                        string(credentialsId: 'frontend_url', variable: 'FRONTEND_URL'),
                        string(credentialsId: 'backend_url', variable: 'BACKEND_URL'),
                        string(credentialsId: 'github_redirect_url', variable: 'GITHUB_REDIRECT_URL'),
                        string(credentialsId: 'jenkins_url', variable: 'JENKINS_URL'),
                        string(credentialsId: 'jenkins_password', variable: 'JENKINS_PASSWORD'),
                        string(credentialsId: 'gemini_api_key', variable: 'GEMINI_API_KEY')

                    ]) {
                        // 로컬 변수로 복사
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
                        def backendUrl = env.BACKEND_URL
                        def githubRedirectUrl = env.GITHUB_REDIRECT_URL
                        def jenkinsUrl = env.JENKINS_URL
                        def jenkinsPassword = env.JENKINS_PASSWORD
                        def geminiApiKey = env.GEMINI_API_KEY




                        sshagent(credentials: [env.VM_SSH_CRED_ID]) {
                            // 원시(보간 없음) 문자열로 스크립트 작성 — ${} 사용 금지
                            def raw = '''ssh -o StrictHostKeyChecking=no __VMUSER__@__VMHOST__ /bin/bash <<'ENDSSH'
set -e

# 네트워크 생성 (있으면 무시)
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
docker pull __IMAGE_TAG__

# 앱 컨테이너 실행 (옵션 -> IMAGE 순서)
docker run -d --name be4man_app -p 8080:8080 --network be4man-network \
  -e DB_URL="__DB_URL__" \
  -e DB_USERNAME="__DB_USERNAME__" \
  -e DB_PASSWORD="__DB_PASSWORD__" \
  -e DB_SCHEMA="__DB_SCHEMA__" \
  -e GITHUB_CLIENT_ID="__GITHUB_CLIENT_ID__" \
  -e GITHUB_CLIENT_SECRET="__GITHUB_CLIENT_SECRET__" \
  -e JWT_SECRET="__JWT_SECRET__" \
  -e FRONTEND_URL="__FRONTEND_URL__" \
  -e REDIS_HOST="my-redis" \
  -e REDIS_PORT="6379" \
  -e BACKEND_URL="__BACKEND_URL__" \
  -e GITHUB_REDIRECT_URL="__GITHUB_REDIRECT_URL__" \
  -e JENKINS_URL="__JENKINS_URL__" \
  -e JENKINS_PASSWORD="__JENKINS_PASSWORD__" \
  -e GEMINI_API_KEY="__GEMINI_API_KEY__" \
  __IMAGE_TAG__

# 상태 확인 및 로그 일부 출력
docker ps -f name=be4man_app --format "table {{.ID}}\t{{.Image}}\t{{.Status}}"
sleep 5
docker logs --tail 50 be4man_app || true

ENDSSH
'''
                            // 필요한 값들만 치환 (안전하게)
                            def remoteScript = raw
                                .replace('__VMUSER__', vmUser)
                                .replace('__VMHOST__', vmHost)
                                .replace(/__IMAGE_TAG__/, imageTag)
                                .replace(/__DB_URL__/, dbUrl)
                                .replace(/__DB_USERNAME__/, dbUsername)
                                .replace(/__DB_PASSWORD__/, dbPassword)
                                .replace(/__DB_SCHEMA__/, dbSchema)
                                .replace(/__GITHUB_CLIENT_ID__/, githubClientId)
                                .replace(/__GITHUB_CLIENT_SECRET__/, githubClientSecret)
                                .replace(/__JWT_SECRET__/, jwtSecret)
                                .replace(/__FRONTEND_URL__/, frontendUrl)
                                .replace(/__BACKEND_URL__/, backendUrl)
                                .replace(/__GITHUB_REDIRECT_URL__/, githubRedirectUrl)
                                .replace(/__JENKINS_URL__/, jenkinsUrl)
                                .replace(/__JENKINS_PASSWORD__/, jenkinsPassword)
                                .replace(/__GEMINI_API_KEY__/, geminiApiKey)

                            // 실행
                            sh remoteScript
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
