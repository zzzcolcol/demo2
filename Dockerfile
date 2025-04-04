# 1단계: 빌드 단계
FROM gradle:8-jdk17 AS build
WORKDIR /home/app

# 소스 복사 및 빌드
COPY . .
RUN gradle bootJar --no-daemon

# 2단계: 실행 단계
FROM eclipse-temurin:17-jre
WORKDIR /app

# 빌드된 jar 복사
COPY --from=build /home/app/build/libs/*.jar app.jar

# 포트 설정 (필요 시 수정)
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
