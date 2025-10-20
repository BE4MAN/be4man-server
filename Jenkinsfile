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
                        // env 값을 로컬 변수에 복사(파서 문제 방지)
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
                            // 원격에서 실행할 스크립트를 안전하게 문자열로 조합
                            def remoteScript =
                                "ssh -o StrictHostKeyChecking=no ${vmUser}@${vmHost} /bin/bash <<'ENDSSH'\n" +
                                "set -e\n" +
                                "\n" +
                                "# 네트워크 생성(이미 있으면 무시)\n" +
                                "docker network create be4man-network || true\n" +
                                "\n" +
                                "# Redis 준비 (없으면 생성, 있으면 시작)\n" +
                                "if ! docker inspect my-redis >/dev/null 2>&1; then\n" +
                                "  echo \"Redis container not found. Creating my-redis...\"\n" +
                                "  docker run -d --name my-redis --network be4man-network redis\n" +
                                "else\n" +
                                "  if [ \"$(docker inspect -f '{{.State.Running}}' my-redis)\" != \"true\" ]; then\n" +
                                "    docker start my-redis\n" +
                                "  fi\n" +
                                "fi\n" +
                                "\n" +
                                "# 기존 앱 컨테이너 중지 및 삭제\n" +
                                "docker stop be4man_app || true\n" +
                                "docker rm be4man_app || true\n" +
                                "\n" +
                                "# 이미지 pull\n" +
                                "docker pull ${imageTag}\n" +
                                "\n" +
                                "# 앱 컨테이너 실행 (옵션 -> IMAGE 순서)\n" +
                                "docker run -d --name be4man_app -p 8080:8080 --network be4man-network \\\n" +
                                "  -e DB_URL=\"${dbUrl}\" \\\n" +
                                "  -e DB_USERNAME=\"${dbUsername}\" \\\n" +
                                "  -e DB_PASSWORD=\"${dbPassword}\" \\\n" +
                                "  -e DB_SCHEMA=\"${dbSchema}\" \\\n" +
                                "  -e GITHUB_CLIENT_ID=\"${githubClientId}\" \\\n" +
                                "  -e GITHUB_CLIENT_SECRET=\"${githubClientSecret}\" \\\n" +
                                "  -e JWT_SECRET=\"${jwtSecret}\" \\\n" +
                                "  -e FRONTEND_URL=\"${frontendUrl}\" \\\n" +
                                "  -e REDIS_HOST=\"my-redis\" \\\n" +
                                "  -e REDIS_PORT=\"6379\" \\\n" +
                                "  ${imageTag}\n" +
                                "\n" +
                                "# 상태 확인 및 로그 일부 출력\n" +
                                "docker ps -f name=be4man_app --format \"table {{.ID}}\\t{{.Image}}\\t{{.Status}}\"\n" +
                                "sleep 5\n" +
                                "docker logs --tail 50 be4man_app || true\n" +
                                "ENDSSH\n"

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
