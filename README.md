# DayMate

DayMate는 대학생을 위한 Java Swing 기반 개인 일정 관리 프로그램입니다. 수업, 시험, 회의, 개인 약속, 수강 과목 및 과제를 한곳에서 관리하고, 오늘의 일정과 과제 마감 정보를 쉽게 확인할 수 있도록 설계했습니다.

## 주요 기능

- 수업, 시험, 회의 및 개인 약속 등록·수정·삭제
- 반복 일정 설정
- 수강 과목 및 과제 관리
- 월간·주간 캘린더 조회
- 오늘의 일정 및 과제 마감 요약
- 일일 요약 알림 설정 및 확인

현재는 프로젝트 초기 단계이며 위 기능들은 이후 개발 과정에서 순차적으로 구현할 예정입니다.

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

저장 기능이 구현되면 데이터는 `${user.home}/.daymate/daymate.db` 경로의 내장 SQLite 데이터베이스에 저장됩니다. 별도의 데이터베이스 서버나 계정 설정은 필요하지 않습니다. 현재 SQLite 스키마와 저장 로직은 아직 구현되지 않았습니다.

## 예정된 패키지 구조

```text
kr.ac.jnu.daymate
├─ ui
├─ controller
├─ data
├─ model
└─ dto
```

## 현재 제한사항

- 프로젝트 초기 단계이므로 실제 기능은 아직 구현되지 않았습니다.
- 알림은 프로그램이 실행 중일 때 동작하는 Swing 알림으로 구현할 예정입니다.
- 단일 사용자의 로컬 환경을 대상으로 하며 계정 동기화나 서버 백업은 제공하지 않습니다.
