# DayMate

DayMate는 대학생을 위한 Java Swing 기반 개인 일정 관리 프로그램입니다. 수업, 시험, 회의, 개인 약속, 수강 과목 및 과제를 한곳에서 관리하고, 오늘의 일정과 과제 마감 정보를 쉽게 확인할 수 있도록 설계했습니다.

## 주요 기능

- 수업, 시험, 회의 및 개인 약속 등록·수정·삭제
- 반복 일정 설정
- 수강 과목 및 과제 관리
- 월간·주간 캘린더 조회
- 오늘의 일정 및 과제 마감 요약
- 일일 요약 알림 설정 및 확인

현재 SQLite 기반 로컬 저장소, 일정 등록·수정·삭제 및 반복 일정 설정, 수강 과목·과제 관리와 제출 상태 변경 화면까지 구현되어 있습니다. 캘린더와 오늘의 요약 등 나머지 화면은 이후 개발 과정에서 순차적으로 연결할 예정입니다.

## 기술 구성

- Java 17
- Java Swing
- Gradle Wrapper (Groovy DSL)
- JUnit 5
- SQLite + Xerial SQLite JDBC
- Javadoc API 매뉴얼

## 실행 및 검증

Windows PowerShell에서 다음 명령을 사용합니다.

```powershell
.\gradlew.bat run
.\gradlew.bat test
.\gradlew.bat javadoc
.\gradlew.bat verifyDayMate
```

`verifyDayMate`는 테스트 실행, Javadoc 생성 및 런타임 의존성을 포함한 runnable JAR 빌드를 함께 수행합니다.

## 데이터 저장

데이터는 `${user.home}/.daymate/daymate.db` 경로의 내장 SQLite 데이터베이스에 저장됩니다. 프로그램이 데이터베이스와 스키마를 자동으로 생성하므로 별도의 데이터베이스 서버나 계정 설정은 필요하지 않습니다.

## 패키지 구조

```text
kr.ac.jnu.daymate
├─ ui          Swing 화면과 입력 대화상자
├─ controller  입력 검증과 사용 사례 제어
├─ data        SQLite 저장 및 메모리 캐시 관리
├─ model       일정, 과목, 과제, 알림 도메인 모델
└─ dto         처리 결과와 화면 전달 데이터
```

## 현재 제한사항

- 메인 화면과 일정 및 과목·과제 관리 화면의 연결은 아직 구현되지 않았습니다.
- 캘린더, 오늘의 요약 및 알림 화면은 아직 구현되지 않았습니다.
- 알림은 프로그램이 실행 중일 때 동작하는 Swing 알림으로 구현할 예정입니다.
- 단일 사용자의 로컬 환경을 대상으로 하며 계정 동기화나 서버 백업은 제공하지 않습니다.
