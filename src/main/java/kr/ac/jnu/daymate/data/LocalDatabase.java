package kr.ac.jnu.daymate.data;

import kr.ac.jnu.daymate.model.AssignmentData;
import kr.ac.jnu.daymate.model.CourseData;
import kr.ac.jnu.daymate.model.NotificationSetting;
import kr.ac.jnu.daymate.model.ScheduleData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/**
 * SQLite 연결, 스키마 초기화 및 DayMate 데이터의 영속화를 담당한다.
 */
public final class LocalDatabase {
    private static final String DEFAULT_DATABASE_DIRECTORY = ".daymate";
    private static final String DEFAULT_DATABASE_FILE = "daymate.db";

    private final String databasePath;

    /**
     * 사용자 홈 디렉터리 아래의 기본 경로를 사용하는 데이터베이스를 생성한다.
     */
    public LocalDatabase() {
        this(Path.of(
                System.getProperty("user.home"),
                DEFAULT_DATABASE_DIRECTORY,
                DEFAULT_DATABASE_FILE
        ).toString());
    }

    /**
     * 지정한 파일 경로를 사용하는 데이터베이스를 생성한다.
     * 테스트에서는 실제 사용자 데이터와 분리된 임시 파일 경로를 전달해야 한다.
     *
     * @param databasePath SQLite 데이터베이스 파일 경로
     * @throws NullPointerException {@code databasePath}가 {@code null}인 경우
     * @throws IllegalArgumentException 경로가 빈 문자열인 경우
     */
    public LocalDatabase(String databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "데이터베이스 경로는 null일 수 없습니다.");
        if (databasePath.isBlank()) {
            throw new IllegalArgumentException("데이터베이스 경로는 비어 있을 수 없습니다.");
        }
    }

    /**
     * 현재 데이터베이스 파일 경로를 반환한다.
     *
     * @return 데이터베이스 파일 경로
     */
    public String getDatabasePath() {
        return databasePath;
    }

    /**
     * 데이터베이스 상위 디렉터리와 필요한 테이블을 생성한다.
     * 이미 존재하는 테이블은 유지한다.
     *
     * @throws IllegalStateException 디렉터리 또는 스키마를 초기화할 수 없는 경우
     */
    public void initialize() {
        createParentDirectory();
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS schedules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        type TEXT NOT NULL,
                        schedule_date TEXT NOT NULL,
                        start_time TEXT NOT NULL,
                        end_time TEXT NOT NULL,
                        location TEXT,
                        memo TEXT,
                        repeat_end_date TEXT
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS schedule_repeat_days (
                        schedule_id INTEGER NOT NULL,
                        day_of_week TEXT NOT NULL,
                        PRIMARY KEY (schedule_id, day_of_week),
                        FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS courses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        professor TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS assignments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        course_id INTEGER NOT NULL,
                        due_date_time TEXT NOT NULL,
                        memo TEXT,
                        submitted INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE RESTRICT
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS notification_settings (
                        id INTEGER PRIMARY KEY CHECK (id = 1),
                        enabled INTEGER NOT NULL,
                        alert_time TEXT,
                        include_schedule INTEGER NOT NULL,
                        include_assignment INTEGER NOT NULL
                    )
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("데이터베이스 스키마를 초기화할 수 없습니다.", exception);
        }
    }

    /**
     * 저장된 일정을 식별자 순서로 불러온다.
     *
     * @return 저장된 일정 목록
     * @throws IllegalStateException 일정 데이터를 조회할 수 없는 경우
     */
    public List<ScheduleData> loadSchedules() {
        try (Connection connection = openConnection()) {
            return loadSchedules(connection);
        } catch (SQLException | RuntimeException exception) {
            throw new IllegalStateException("일정 데이터를 불러올 수 없습니다.", exception);
        }
    }

    /**
     * 저장된 수강 과목을 식별자 순서로 불러온다.
     *
     * @return 저장된 과목 목록
     * @throws IllegalStateException 과목 데이터를 조회할 수 없는 경우
     */
    public List<CourseData> loadCourses() {
        try (Connection connection = openConnection()) {
            return loadCourses(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException("과목 데이터를 불러올 수 없습니다.", exception);
        }
    }

    /**
     * 저장된 과제를 식별자 순서로 불러온다.
     *
     * @return 저장된 과제 목록
     * @throws IllegalStateException 과제 데이터를 조회할 수 없는 경우
     */
    public List<AssignmentData> loadAssignments() {
        try (Connection connection = openConnection()) {
            return loadAssignments(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException("과제 데이터를 불러올 수 없습니다.", exception);
        }
    }

    /**
     * 저장된 알림 설정을 불러온다.
     * 저장된 값이 없다면 비활성화된 기본 설정을 반환한다.
     *
     * @return 알림 설정
     * @throws IllegalStateException 알림 설정을 조회할 수 없는 경우
     */
    public NotificationSetting loadNotificationSetting() {
        try (Connection connection = openConnection()) {
            return loadNotificationSetting(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException("알림 설정을 불러올 수 없습니다.", exception);
        }
    }

    /**
     * 전달받은 목록과 데이터베이스의 일정 데이터를 트랜잭션으로 동기화한다.
     * 식별자가 {@code 0}인 일정에는 SQLite가 새로운 식별자를 부여한다.
     *
     * @param schedules 저장할 전체 일정 목록
     * @return 저장에 성공하면 {@code true}
     */
    public boolean saveSchedules(List<ScheduleData> schedules) {
        return saveAndLoadSchedules(schedules) != null;
    }

    /**
     * 전달받은 목록과 데이터베이스의 과목 데이터를 트랜잭션으로 동기화한다.
     * 연결된 과제가 남아 있는 과목을 제거하려 하면 저장에 실패한다.
     *
     * @param courses 저장할 전체 과목 목록
     * @return 저장에 성공하면 {@code true}
     */
    public boolean saveCourses(List<CourseData> courses) {
        return saveAndLoadCourses(courses) != null;
    }

    /**
     * 전달받은 목록과 데이터베이스의 과제 데이터를 트랜잭션으로 동기화한다.
     *
     * @param assignments 저장할 전체 과제 목록
     * @return 저장에 성공하면 {@code true}
     */
    public boolean saveAssignments(List<AssignmentData> assignments) {
        return saveAndLoadAssignments(assignments) != null;
    }

    /**
     * 알림 설정 한 건을 저장하거나 기존 값을 갱신한다.
     *
     * @param setting 저장할 알림 설정
     * @return 저장에 성공하면 {@code true}
     */
    public boolean saveNotificationSetting(NotificationSetting setting) {
        return saveAndLoadNotificationSetting(setting) != null;
    }

    List<ScheduleData> saveAndLoadSchedules(List<ScheduleData> schedules) {
        Objects.requireNonNull(schedules, "일정 목록은 null일 수 없습니다.");
        return executeTransaction(connection -> {
            synchronizeSchedules(connection, schedules);
            return loadSchedules(connection);
        });
    }

    List<CourseData> saveAndLoadCourses(List<CourseData> courses) {
        Objects.requireNonNull(courses, "과목 목록은 null일 수 없습니다.");
        return executeTransaction(connection -> {
            synchronizeCourses(connection, courses);
            return loadCourses(connection);
        });
    }

    List<AssignmentData> saveAndLoadAssignments(List<AssignmentData> assignments) {
        Objects.requireNonNull(assignments, "과제 목록은 null일 수 없습니다.");
        return executeTransaction(connection -> {
            synchronizeAssignments(connection, assignments);
            return loadAssignments(connection);
        });
    }

    NotificationSetting saveAndLoadNotificationSetting(NotificationSetting setting) {
        Objects.requireNonNull(setting, "알림 설정은 null일 수 없습니다.");
        return executeTransaction(connection -> {
            upsertNotificationSetting(connection, setting);
            return loadNotificationSetting(connection);
        });
    }

    private void upsertNotificationSetting(Connection connection, NotificationSetting setting) throws SQLException {
        String sql = """
                INSERT INTO notification_settings (
                    id, enabled, alert_time, include_schedule, include_assignment
                ) VALUES (1, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    enabled = excluded.enabled,
                    alert_time = excluded.alert_time,
                    include_schedule = excluded.include_schedule,
                    include_assignment = excluded.include_assignment
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, setting.isEnabled() ? 1 : 0);
            statement.setString(2, setting.getAlertTime() == null ? null : setting.getAlertTime().toString());
            statement.setInt(3, setting.isIncludeSchedule() ? 1 : 0);
            statement.setInt(4, setting.isIncludeAssignment() ? 1 : 0);
            statement.executeUpdate();
        }
    }

    private void createParentDirectory() {
        try {
            Path absolutePath = Path.of(databasePath).toAbsolutePath();
            Path parent = absolutePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("데이터베이스 디렉터리를 생성할 수 없습니다.", exception);
        }
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
        return connection;
    }

    private List<ScheduleData> loadSchedules(Connection connection) throws SQLException {
        String repeatSql = "SELECT schedule_id, day_of_week FROM schedule_repeat_days ORDER BY schedule_id";
        String scheduleSql = """
                SELECT id, title, type, schedule_date, start_time, end_time,
                       location, memo, repeat_end_date
                FROM schedules
                ORDER BY id
                """;
        Map<Integer, Set<DayOfWeek>> repeatDaysBySchedule = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(repeatSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int scheduleId = resultSet.getInt("schedule_id");
                repeatDaysBySchedule
                        .computeIfAbsent(scheduleId, ignored -> EnumSet.noneOf(DayOfWeek.class))
                        .add(DayOfWeek.valueOf(resultSet.getString("day_of_week")));
            }
        }

        List<ScheduleData> loadedSchedules = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(scheduleSql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int scheduleId = resultSet.getInt("id");
                String repeatEndDate = resultSet.getString("repeat_end_date");
                loadedSchedules.add(new ScheduleData(
                        scheduleId,
                        resultSet.getString("title"),
                        resultSet.getString("type"),
                        LocalDate.parse(resultSet.getString("schedule_date")),
                        LocalTime.parse(resultSet.getString("start_time")),
                        LocalTime.parse(resultSet.getString("end_time")),
                        resultSet.getString("location"),
                        resultSet.getString("memo"),
                        repeatDaysBySchedule.getOrDefault(scheduleId, Set.of()),
                        repeatEndDate == null ? null : LocalDate.parse(repeatEndDate)
                ));
            }
        }
        return List.copyOf(loadedSchedules);
    }

    private List<CourseData> loadCourses(Connection connection) throws SQLException {
        String sql = "SELECT id, name, professor FROM courses ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<CourseData> loadedCourses = new ArrayList<>();
            while (resultSet.next()) {
                loadedCourses.add(new CourseData(
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getString("professor")
                ));
            }
            return List.copyOf(loadedCourses);
        }
    }

    private List<AssignmentData> loadAssignments(Connection connection) throws SQLException {
        String sql = """
                SELECT id, title, course_id, due_date_time, memo, submitted
                FROM assignments
                ORDER BY id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<AssignmentData> loadedAssignments = new ArrayList<>();
            while (resultSet.next()) {
                loadedAssignments.add(new AssignmentData(
                        resultSet.getInt("id"),
                        resultSet.getString("title"),
                        resultSet.getInt("course_id"),
                        LocalDateTime.parse(resultSet.getString("due_date_time")),
                        resultSet.getString("memo"),
                        resultSet.getInt("submitted") == 1
                ));
            }
            return List.copyOf(loadedAssignments);
        }
    }

    private NotificationSetting loadNotificationSetting(Connection connection) throws SQLException {
        String sql = """
                SELECT enabled, alert_time, include_schedule, include_assignment
                FROM notification_settings
                WHERE id = 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return defaultNotificationSetting();
            }
            String alertTime = resultSet.getString("alert_time");
            return new NotificationSetting(
                    resultSet.getInt("enabled") == 1,
                    alertTime == null ? null : LocalTime.parse(alertTime),
                    resultSet.getInt("include_schedule") == 1,
                    resultSet.getInt("include_assignment") == 1
            );
        }
    }

    private <T> T executeTransaction(SqlTransaction<T> transaction) {
        Connection connection;
        try {
            connection = openConnection();
        } catch (SQLException exception) {
            return null;
        }

        try {
            connection.setAutoCommit(false);
            T result = transaction.execute(connection);
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException exception) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            return null;
        } finally {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // 트랜잭션 결과는 commit 시점에 확정되므로 연결 종료 오류가 결과를 변경하지 않는다.
            }
        }
    }

    private void synchronizeSchedules(Connection connection, List<ScheduleData> schedules) throws SQLException {
        Set<Integer> persistedIds = new LinkedHashSet<>();
        for (ScheduleData schedule : schedules) {
            int persistedId = upsertSchedule(connection, schedule);
            persistedIds.add(persistedId);
            replaceRepeatDays(connection, persistedId, schedule.getRepeatDays());
        }
        deleteRowsMissingFrom(connection, "schedules", persistedIds);
    }

    private int upsertSchedule(Connection connection, ScheduleData schedule) throws SQLException {
        if (schedule.getId() <= 0) {
            String insertSql = """
                    INSERT INTO schedules (
                        title, type, schedule_date, start_time, end_time,
                        location, memo, repeat_end_date
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                setScheduleValues(statement, schedule, 1);
                statement.executeUpdate();
            }
            return lastInsertedId(connection);
        }

        String upsertSql = """
                INSERT INTO schedules (
                    id, title, type, schedule_date, start_time, end_time,
                    location, memo, repeat_end_date
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    title = excluded.title,
                    type = excluded.type,
                    schedule_date = excluded.schedule_date,
                    start_time = excluded.start_time,
                    end_time = excluded.end_time,
                    location = excluded.location,
                    memo = excluded.memo,
                    repeat_end_date = excluded.repeat_end_date
                """;
        try (PreparedStatement statement = connection.prepareStatement(upsertSql)) {
            statement.setInt(1, schedule.getId());
            setScheduleValues(statement, schedule, 2);
            statement.executeUpdate();
        }
        return schedule.getId();
    }

    private void setScheduleValues(PreparedStatement statement, ScheduleData schedule, int startIndex)
            throws SQLException {
        statement.setString(startIndex, schedule.getTitle());
        statement.setString(startIndex + 1, schedule.getType());
        statement.setString(startIndex + 2, schedule.getDate() == null ? null : schedule.getDate().toString());
        statement.setString(startIndex + 3,
                schedule.getStartTime() == null ? null : schedule.getStartTime().toString());
        statement.setString(startIndex + 4,
                schedule.getEndTime() == null ? null : schedule.getEndTime().toString());
        statement.setString(startIndex + 5, schedule.getLocation());
        statement.setString(startIndex + 6, schedule.getMemo());
        statement.setString(startIndex + 7,
                schedule.getRepeatEndDate() == null ? null : schedule.getRepeatEndDate().toString());
    }

    private void replaceRepeatDays(
            Connection connection,
            int scheduleId,
            Set<DayOfWeek> repeatDays
    ) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM schedule_repeat_days WHERE schedule_id = ?")) {
            delete.setInt(1, scheduleId);
            delete.executeUpdate();
        }
        String insertSql = "INSERT INTO schedule_repeat_days (schedule_id, day_of_week) VALUES (?, ?)";
        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
            for (DayOfWeek repeatDay : repeatDays) {
                insert.setInt(1, scheduleId);
                insert.setString(2, repeatDay.name());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void synchronizeCourses(Connection connection, List<CourseData> courses) throws SQLException {
        Set<Integer> persistedIds = new LinkedHashSet<>();
        for (CourseData course : courses) {
            persistedIds.add(upsertCourse(connection, course));
        }
        deleteRowsMissingFrom(connection, "courses", persistedIds);
    }

    private int upsertCourse(Connection connection, CourseData course) throws SQLException {
        if (course.getId() <= 0) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO courses (name, professor) VALUES (?, ?)")) {
                statement.setString(1, course.getName());
                statement.setString(2, course.getProfessor());
                statement.executeUpdate();
            }
            return lastInsertedId(connection);
        }
        String sql = """
                INSERT INTO courses (id, name, professor) VALUES (?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    name = excluded.name,
                    professor = excluded.professor
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, course.getId());
            statement.setString(2, course.getName());
            statement.setString(3, course.getProfessor());
            statement.executeUpdate();
        }
        return course.getId();
    }

    private void synchronizeAssignments(Connection connection, List<AssignmentData> assignments) throws SQLException {
        Set<Integer> persistedIds = new LinkedHashSet<>();
        for (AssignmentData assignment : assignments) {
            persistedIds.add(upsertAssignment(connection, assignment));
        }
        deleteRowsMissingFrom(connection, "assignments", persistedIds);
    }

    private int upsertAssignment(Connection connection, AssignmentData assignment) throws SQLException {
        if (assignment.getId() <= 0) {
            String insertSql = """
                    INSERT INTO assignments (title, course_id, due_date_time, memo, submitted)
                    VALUES (?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                setAssignmentValues(statement, assignment, 1);
                statement.executeUpdate();
            }
            return lastInsertedId(connection);
        }
        String upsertSql = """
                INSERT INTO assignments (id, title, course_id, due_date_time, memo, submitted)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    title = excluded.title,
                    course_id = excluded.course_id,
                    due_date_time = excluded.due_date_time,
                    memo = excluded.memo,
                    submitted = excluded.submitted
                """;
        try (PreparedStatement statement = connection.prepareStatement(upsertSql)) {
            statement.setInt(1, assignment.getId());
            setAssignmentValues(statement, assignment, 2);
            statement.executeUpdate();
        }
        return assignment.getId();
    }

    private void setAssignmentValues(PreparedStatement statement, AssignmentData assignment, int startIndex)
            throws SQLException {
        statement.setString(startIndex, assignment.getTitle());
        statement.setInt(startIndex + 1, assignment.getCourseId());
        statement.setString(startIndex + 2,
                assignment.getDueDateTime() == null ? null : assignment.getDueDateTime().toString());
        statement.setString(startIndex + 3, assignment.getMemo());
        statement.setInt(startIndex + 4, assignment.isSubmitted() ? 1 : 0);
    }

    private int lastInsertedId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT last_insert_rowid()")) {
            if (!resultSet.next()) {
                throw new SQLException("생성된 식별자를 조회할 수 없습니다.");
            }
            return resultSet.getInt(1);
        }
    }

    private void deleteRowsMissingFrom(Connection connection, String tableName, Set<Integer> retainedIds)
            throws SQLException {
        if (retainedIds.isEmpty()) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM " + tableName);
            }
            return;
        }

        StringJoiner placeholders = new StringJoiner(", ");
        retainedIds.forEach(ignored -> placeholders.add("?"));
        String sql = "DELETE FROM " + tableName + " WHERE id NOT IN (" + placeholders + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (int retainedId : retainedIds) {
                statement.setInt(index++, retainedId);
            }
            statement.executeUpdate();
        }
    }

    private NotificationSetting defaultNotificationSetting() {
        return new NotificationSetting(false, null, true, true);
    }

    @FunctionalInterface
    private interface SqlTransaction<T> {
        T execute(Connection connection) throws SQLException;
    }
}
