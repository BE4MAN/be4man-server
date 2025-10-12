# Dockerfile

# 베이스 이미지: OpenJDK 17이 설치된 경량 리눅스 이미지로 변경
FROM openjdk:17-jdk-slim

# 빌드 시스템이 생성하는 최종 JAR 파일의 경로 및 이름을 ARG로 정의
ARG JAR_FILE=build/libs/be4man-server.jar

# 컨테이너 내 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일을 컨테이너의 /app/app.jar로 복사
COPY ${JAR_FILE} app.jar

# 애플리케이션 실행 명령어 (컨테이너 시작 시 실행됨)
ENTRYPOINT ["java","-jar","/app/app.jar"]