package com.fairpilot.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * 서버 실행 전 로컬 환경 점검.
 *
 * 실행:  ./gradlew preflight
 * (일반 test/build 태스크에서는 제외 — CI·팀원 빌드에 영향 없음)
 *
 * 점검 순서: MySQL 실행 → 계정 인증 → DB 존재 → Flyway 테이블 → Redis
 * 실패 단계에서 [진단]과 [조치]가 출력된다.
 */
@Tag("preflight")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServerStartupPreflightTest {

    private static final Map<String, String> dotenv = new HashMap<>();
    private static String dbAddress;
    private static String dbName;
    private static String dbUser;
    private static String dbPassword;

    @BeforeAll
    static void loadEnv() throws Exception {
        // 레포 루트의 .env 로드 (테스트 실행 위치가 모듈 폴더이므로 ../.env 우선)
        for (Path p : List.of(Path.of("..", ".env"), Path.of(".env"))) {
            if (Files.exists(p)) {
                for (String raw : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                    String line = raw.strip();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        dotenv.put(line.substring(0, eq).strip(), line.substring(eq + 1).strip());
                    }
                }
                System.out.println("[preflight] .env 로드: " + p.toAbsolutePath().normalize());
                break;
            }
        }
        dbAddress = prop("DB_ADDRESS", "localhost:3306");
        dbName = prop("DB_NAME", "fairpilot");
        dbUser = prop("DB_USERNAME", "root");
        dbPassword = prop("DB_PASSWORD", null);
        System.out.println("[preflight] 점검 대상: MySQL=" + dbAddress + "/" + dbName
                + " (계정: " + dbUser + "), Redis=localhost:6379");
    }

    /** OS 환경변수가 .env보다 우선 (Spring 프로퍼티 우선순위와 동일). */
    private static String prop(String key, String defaultValue) {
        String fromOs = System.getenv(key);
        if (fromOs != null && !fromOs.isBlank()) return fromOs;
        return dotenv.getOrDefault(key, defaultValue);
    }

    private static String host() {
        return dbAddress.contains(":") ? dbAddress.split(":")[0] : dbAddress;
    }

    private static int port() {
        return dbAddress.contains(":") ? Integer.parseInt(dbAddress.split(":")[1]) : 3306;
    }

    @Test
    @Order(1)
    void 단계1_MySQL_서버_실행_확인() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host(), port()), 3000);
            System.out.println("[통과] 1단계 — MySQL 실행 중 (" + dbAddress + ")");
        } catch (Exception e) {
            fail("""

                    [진단] MySQL 서버 미실행 — %s 연결 불가

                    [조치]
                      1. Win+R → services.msc 입력 → 'MySQL80' 서비스 → 우클릭 → 시작
                      2. 주소/포트가 다른 경우 → 레포 루트 .env 의 DB_ADDRESS 수정 (기본 localhost:3306)
                    """.formatted(dbAddress));
        }
    }

    @Test
    @Order(2)
    void 단계2_MySQL_계정_인증_확인() {
        if (dbPassword == null || dbPassword.isBlank() || dbPassword.contains("본인")) {
            fail("""

                    [진단] DB 비밀번호 미설정 — .env 의 DB_PASSWORD 없음

                    [조치]
                      1. 레포 루트에 .env 파일 생성 (이미 있으면 열기)
                      2. 아래 한 줄 입력:
                           DB_PASSWORD=<본인 MySQL root 비밀번호>
                      ※ .env 는 gitignore 처리됨 — 커밋되지 않음
                    """);
        }
        try (Connection ignored = DriverManager.getConnection(
                "jdbc:mysql://" + dbAddress + "/?serverTimezone=Asia/Seoul", dbUser, dbPassword)) {
            System.out.println("[통과] 2단계 — MySQL 인증 성공 (계정: " + dbUser + ")");
        } catch (SQLException e) {
            fail("""

                    [진단] MySQL 인증 실패 — 비밀번호 불일치 (%s)

                    [조치]
                      1. .env 의 DB_PASSWORD 값 확인
                      2. PowerShell 임시 변수가 .env 를 덮어쓰는 경우 → 입력: Remove-Item Env:DB_PASSWORD
                      3. Gradle 데몬 초기화 → 입력: ./gradlew --stop
                      4. 비밀번호 자체 검증 → 입력: mysql -u %s -p (직접 로그인 확인)
                    """.formatted(e.getMessage(), dbUser));
        }
    }

    @Test
    @Order(3)
    void 단계3_fairpilot_데이터베이스_존재_확인() {
        try (Connection ignored = DriverManager.getConnection(
                "jdbc:mysql://" + dbAddress + "/" + dbName + "?serverTimezone=Asia/Seoul", dbUser, dbPassword)) {
            System.out.println("[통과] 3단계 — 데이터베이스 '" + dbName + "' 존재");
        } catch (SQLException e) {
            fail("""

                    [진단] 데이터베이스 '%s' 없음

                    [조치]
                      1. PowerShell 에 입력: mysql -u %s -p → 비밀번호 입력
                      2. MySQL 프롬프트에 입력:
                           CREATE DATABASE %s CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
                      ※ 테이블은 만들지 말 것 — 4단계 Flyway 가 자동 생성
                    """.formatted(dbName, dbUser, dbName));
        }
    }

    @Test
    @Order(4)
    void 단계4_Flyway_마이그레이션_완료_확인() {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://" + dbAddress + "/" + dbName + "?serverTimezone=Asia/Seoul", dbUser, dbPassword);
             Statement st = conn.createStatement()) {

            boolean hasHistory;
            boolean hasExhibition;
            try (ResultSet rs = st.executeQuery("SHOW TABLES LIKE 'flyway_schema_history'")) {
                hasHistory = rs.next();
            }
            try (ResultSet rs = st.executeQuery("SHOW TABLES LIKE 'exhibition'")) {
                hasExhibition = rs.next();
            }

            if (!hasHistory) {
                fail("""

                        [진단] 테이블 없음 — Flyway 마이그레이션 미실행

                        [조치]
                          1. PowerShell 에 입력: ./gradlew :api-expo-admin:bootRun
                             (4개 앱 중 expo-admin 만 마이그레이션 수행)
                          2. 로그에 'Successfully applied ... migrations' 확인 → Ctrl+C 종료
                          ※ 나머지 앱은 테이블을 만들지 않음 — 반드시 expo-admin 먼저 실행
                        """);
            }
            if (!hasExhibition) {
                fail("""

                        [진단] 테이블 불완전 — 마이그레이션 중단 상태

                        [조치]
                          1. expo-admin 실행 로그의 Flyway 에러 확인
                          2. 개발 DB 초기화가 가장 빠름:
                             MySQL 프롬프트에 입력: DROP DATABASE %s;
                             → 3단계(CREATE DATABASE)부터 재수행
                        """.formatted(dbName));
            }
            System.out.println("[통과] 4단계 — Flyway 마이그레이션 완료");
        } catch (SQLException e) {
            fail("\n[진단] 테이블 조회 오류 — " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    void 단계5_Redis_실행_확인() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 6379), 3000);
            OutputStream out = socket.getOutputStream();
            out.write("PING\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String reply = reader.readLine();
            if (reply == null || !reply.contains("PONG")) {
                throw new IllegalStateException("PING 응답 이상: " + reply);
            }
            System.out.println("[통과] 5단계 — Redis 실행 중 (localhost:6379)");
            System.out.println();
            System.out.println("[완료] 전체 점검 통과 — 서버 실행 가능");
            System.out.println("  visitor        → ./gradlew :api-visitor:bootRun        (8080)");
            System.out.println("  exhibitor      → ./gradlew :api-exhibitor:bootRun      (8081)");
            System.out.println("  expo-admin     → ./gradlew :api-expo-admin:bootRun     (8082)");
            System.out.println("  platform-admin → ./gradlew :api-platform-admin:bootRun (8083)");
        } catch (Exception e) {
            fail("""

                    [진단] Redis 미실행 — localhost:6379 연결 불가
                    (모든 앱이 기동 시 Redis 에 연결 — Redis 없이는 서버 기동 불가)

                    [조치 — Docker 사용 시]
                      1. Docker Desktop 실행
                      2. PowerShell 에 입력: docker run -d --name redis -p 6379:6379 redis
                         (컨테이너가 이미 있는 경우 → 입력: docker start redis)
                      3. 확인 → 입력: docker exec redis redis-cli ping → 'PONG' 출력 시 정상

                    [조치 — Docker 미사용 시]
                      1. https://www.memurai.com 접속 → Memurai 설치
                      2. 설치 후 Windows 서비스로 자동 실행됨 (별도 명령 불필요)
                    """);
        }
    }
}
