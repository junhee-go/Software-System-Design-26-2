package kr.ac.jnu.daymate.data;

import kr.ac.jnu.daymate.model.AssignmentData;
import kr.ac.jnu.daymate.model.CourseData;
import kr.ac.jnu.daymate.model.NotificationSetting;
import kr.ac.jnu.daymate.model.ScheduleData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SQLite 데이터와 애플리케이션의 메모리 캐시를 일관되게 관리한다.
 */
public final class DayMateDataManager {
    private final LocalDatabase database;
    private List<ScheduleData> schedules = List.of();
    private List<CourseData> courses = List.of();
    private List<AssignmentData> assignments = List.of();
    private NotificationSetting notificationSetting = defaultNotificationSetting();

    /**
     * 기본 사용자 데이터베이스를 사용하는 데이터 관리자를 생성한다.
     */
    public DayMateDataManager() {
        this(new LocalDatabase());
    }

    /**
     * 지정한 데이터베이스를 사용하는 데이터 관리자를 생성한다.
     * 테스트에서는 임시 파일을 사용하는 {@link LocalDatabase}를 전달해야 한다.
     *
     * @param database 사용할 로컬 데이터베이스
     * @throws NullPointerException {@code database}가 {@code null}인 경우
     */
    public DayMateDataManager(LocalDatabase database) {
        this.database = Objects.requireNonNull(database, "데이터베이스는 null일 수 없습니다.");
    }

    /**
     * 데이터베이스를 초기화하고 모든 저장 데이터를 캐시에 불러온다.
     * 모든 조회가 성공한 후에만 기존 캐시를 교체한다.
     *
     * @throws IllegalStateException 데이터베이스 초기화 또는 데이터 조회에 실패한 경우
     */
    public synchronized void loadAllData() {
        database.initialize();
        List<ScheduleData> loadedSchedules = database.loadSchedules();
        List<CourseData> loadedCourses = database.loadCourses();
        List<AssignmentData> loadedAssignments = database.loadAssignments();
        NotificationSetting loadedSetting = database.loadNotificationSetting();

        schedules = copySchedules(loadedSchedules);
        courses = copyCourses(loadedCourses);
        assignments = copyAssignments(loadedAssignments);
        notificationSetting = copyNotificationSetting(loadedSetting);
    }

    /**
     * 일정을 새로 저장하거나 같은 식별자의 기존 일정을 갱신한다.
     * 저장이 성공한 경우에만 데이터베이스에서 일정을 다시 불러와 캐시를 교체한다.
     *
     * @param schedule 저장할 일정
     * @return 저장에 성공하면 {@code true}
     */
    public synchronized boolean saveSchedule(ScheduleData schedule) {
        Objects.requireNonNull(schedule, "일정은 null일 수 없습니다.");
        List<ScheduleData> candidate = new ArrayList<>(schedules);
        replaceOrAddSchedule(candidate, copySchedule(schedule));
        List<ScheduleData> persisted = database.saveAndLoadSchedules(candidate);
        if (persisted == null) {
            return false;
        }
        schedules = copySchedules(persisted);
        return true;
    }

    /**
     * 지정한 식별자의 일정을 삭제한다.
     * 삭제할 일정이 없으면 데이터베이스를 변경하지 않고 {@code false}를 반환한다.
     *
     * @param scheduleId 삭제할 일정 식별자
     * @return 일정이 삭제되면 {@code true}
     */
    public synchronized boolean deleteSchedule(int scheduleId) {
        List<ScheduleData> candidate = schedules.stream()
                .filter(schedule -> schedule.getId() != scheduleId)
                .map(DayMateDataManager::copySchedule)
                .toList();
        if (candidate.size() == schedules.size()) {
            return false;
        }
        List<ScheduleData> persisted = database.saveAndLoadSchedules(candidate);
        if (persisted == null) {
            return false;
        }
        schedules = copySchedules(persisted);
        return true;
    }

    /**
     * 과목을 새로 저장하거나 같은 식별자의 기존 과목을 갱신한다.
     *
     * @param course 저장할 과목
     * @return 저장에 성공하면 {@code true}
     */
    public synchronized boolean saveCourse(CourseData course) {
        Objects.requireNonNull(course, "과목은 null일 수 없습니다.");
        List<CourseData> candidate = new ArrayList<>(courses);
        replaceOrAddCourse(candidate, copyCourse(course));
        List<CourseData> persisted = database.saveAndLoadCourses(candidate);
        if (persisted == null) {
            return false;
        }
        courses = copyCourses(persisted);
        return true;
    }

    /**
     * 지정한 식별자의 과목을 삭제한다.
     * 연결된 과제가 남아 있으면 외래 키 제약에 의해 삭제하지 않는다.
     *
     * @param courseId 삭제할 과목 식별자
     * @return 과목이 삭제되면 {@code true}
     */
    public synchronized boolean deleteCourse(int courseId) {
        List<CourseData> candidate = courses.stream()
                .filter(course -> course.getId() != courseId)
                .map(DayMateDataManager::copyCourse)
                .toList();
        if (candidate.size() == courses.size()) {
            return false;
        }
        List<CourseData> persisted = database.saveAndLoadCourses(candidate);
        if (persisted == null) {
            return false;
        }
        courses = copyCourses(persisted);
        return true;
    }

    /**
     * 과제를 새로 저장하거나 같은 식별자의 기존 과제를 갱신한다.
     * 존재하지 않는 과목을 참조하면 저장하지 않는다.
     *
     * @param assignment 저장할 과제
     * @return 저장에 성공하면 {@code true}
     */
    public synchronized boolean saveAssignment(AssignmentData assignment) {
        Objects.requireNonNull(assignment, "과제는 null일 수 없습니다.");
        List<AssignmentData> candidate = new ArrayList<>(assignments);
        replaceOrAddAssignment(candidate, copyAssignment(assignment));
        List<AssignmentData> persisted = database.saveAndLoadAssignments(candidate);
        if (persisted == null) {
            return false;
        }
        assignments = copyAssignments(persisted);
        return true;
    }

    /**
     * 지정한 식별자의 과제를 삭제한다.
     *
     * @param assignmentId 삭제할 과제 식별자
     * @return 과제가 삭제되면 {@code true}
     */
    public synchronized boolean deleteAssignment(int assignmentId) {
        List<AssignmentData> candidate = assignments.stream()
                .filter(assignment -> assignment.getId() != assignmentId)
                .map(DayMateDataManager::copyAssignment)
                .toList();
        if (candidate.size() == assignments.size()) {
            return false;
        }
        List<AssignmentData> persisted = database.saveAndLoadAssignments(candidate);
        if (persisted == null) {
            return false;
        }
        assignments = copyAssignments(persisted);
        return true;
    }

    /**
     * 알림 설정을 저장하고 성공한 경우 캐시를 갱신한다.
     *
     * @param setting 저장할 알림 설정
     * @return 저장에 성공하면 {@code true}
     */
    public synchronized boolean saveNotificationSetting(NotificationSetting setting) {
        Objects.requireNonNull(setting, "알림 설정은 null일 수 없습니다.");
        NotificationSetting candidate = copyNotificationSetting(setting);
        NotificationSetting persisted = database.saveAndLoadNotificationSetting(candidate);
        if (persisted == null) {
            return false;
        }
        notificationSetting = copyNotificationSetting(persisted);
        return true;
    }

    /**
     * 현재 캐시의 일정 목록을 복사하여 반환한다.
     * 반환된 목록과 일정 객체를 변경해도 내부 캐시에 영향을 주지 않는다.
     *
     * @return 복사된 일정 목록
     */
    public synchronized List<ScheduleData> getSchedules() {
        return copySchedules(schedules);
    }

    /**
     * 현재 캐시의 과목 목록을 복사하여 반환한다.
     *
     * @return 복사된 과목 목록
     */
    public synchronized List<CourseData> getCourses() {
        return copyCourses(courses);
    }

    /**
     * 현재 캐시의 과제 목록을 복사하여 반환한다.
     *
     * @return 복사된 과제 목록
     */
    public synchronized List<AssignmentData> getAssignments() {
        return copyAssignments(assignments);
    }

    /**
     * 현재 캐시의 알림 설정을 복사하여 반환한다.
     *
     * @return 복사된 알림 설정
     */
    public synchronized NotificationSetting getNotificationSetting() {
        return copyNotificationSetting(notificationSetting);
    }

    private static void replaceOrAddSchedule(List<ScheduleData> values, ScheduleData replacement) {
        if (replacement.getId() > 0) {
            for (int index = 0; index < values.size(); index++) {
                if (values.get(index).getId() == replacement.getId()) {
                    values.set(index, replacement);
                    return;
                }
            }
        }
        values.add(replacement);
    }

    private static void replaceOrAddCourse(List<CourseData> values, CourseData replacement) {
        if (replacement.getId() > 0) {
            for (int index = 0; index < values.size(); index++) {
                if (values.get(index).getId() == replacement.getId()) {
                    values.set(index, replacement);
                    return;
                }
            }
        }
        values.add(replacement);
    }

    private static void replaceOrAddAssignment(List<AssignmentData> values, AssignmentData replacement) {
        if (replacement.getId() > 0) {
            for (int index = 0; index < values.size(); index++) {
                if (values.get(index).getId() == replacement.getId()) {
                    values.set(index, replacement);
                    return;
                }
            }
        }
        values.add(replacement);
    }

    private static List<ScheduleData> copySchedules(List<ScheduleData> source) {
        return source.stream().map(DayMateDataManager::copySchedule).toList();
    }

    private static ScheduleData copySchedule(ScheduleData source) {
        return new ScheduleData(
                source.getId(),
                source.getTitle(),
                source.getType(),
                source.getDate(),
                source.getStartTime(),
                source.getEndTime(),
                source.getLocation(),
                source.getMemo(),
                source.getRepeatDays(),
                source.getRepeatEndDate()
        );
    }

    private static List<CourseData> copyCourses(List<CourseData> source) {
        return source.stream().map(DayMateDataManager::copyCourse).toList();
    }

    private static CourseData copyCourse(CourseData source) {
        return new CourseData(source.getId(), source.getName(), source.getProfessor());
    }

    private static List<AssignmentData> copyAssignments(List<AssignmentData> source) {
        return source.stream().map(DayMateDataManager::copyAssignment).toList();
    }

    private static AssignmentData copyAssignment(AssignmentData source) {
        return new AssignmentData(
                source.getId(),
                source.getTitle(),
                source.getCourseId(),
                source.getDueDateTime(),
                source.getMemo(),
                source.isSubmitted()
        );
    }

    private static NotificationSetting copyNotificationSetting(NotificationSetting source) {
        return new NotificationSetting(
                source.isEnabled(),
                source.getAlertTime(),
                source.isIncludeSchedule(),
                source.isIncludeAssignment()
        );
    }

    private static NotificationSetting defaultNotificationSetting() {
        return new NotificationSetting(false, null, true, true);
    }
}
