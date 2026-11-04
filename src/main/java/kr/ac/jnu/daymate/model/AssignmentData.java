package kr.ac.jnu.daymate.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 과제 정보와 관련 과목, 제출 마감 일시 및 제출 상태를 저장한다.
 */
public final class AssignmentData {
    private final int id;
    private final String title;
    private final int courseId;
    private final LocalDateTime dueDateTime;
    private final String memo;
    private boolean submitted;

    /**
     * 과제 데이터를 생성한다.
     *
     * @param id 과제 식별자. 저장 전에는 {@code 0}을 사용한다.
     * @param title 과제명
     * @param courseId 관련 과목의 식별자
     * @param dueDateTime 제출 마감 일시
     * @param memo 선택 입력인 메모
     * @param submitted 제출 완료 여부
     */
    public AssignmentData(
            int id,
            String title,
            int courseId,
            LocalDateTime dueDateTime,
            String memo,
            boolean submitted
    ) {
        this.id = id;
        this.title = title;
        this.courseId = courseId;
        this.dueDateTime = dueDateTime;
        this.memo = memo;
        this.submitted = submitted;
    }

    /**
     * 과제 식별자를 반환한다.
     *
     * @return 과제 식별자
     */
    public int getId() {
        return id;
    }

    /**
     * 과제명을 반환한다.
     *
     * @return 과제명
     */
    public String getTitle() {
        return title;
    }

    /**
     * 관련 과목의 식별자를 반환한다.
     *
     * @return 관련 과목 식별자
     */
    public int getCourseId() {
        return courseId;
    }

    /**
     * 제출 마감 일시를 반환한다.
     *
     * @return 제출 마감 일시
     */
    public LocalDateTime getDueDateTime() {
        return dueDateTime;
    }

    /**
     * 과제 메모를 반환한다.
     *
     * @return 과제 메모. 입력되지 않았다면 {@code null}
     */
    public String getMemo() {
        return memo;
    }

    /**
     * 과제가 제출 완료 상태인지 확인한다.
     *
     * @return 제출 완료 상태이면 {@code true}
     */
    public boolean isSubmitted() {
        return submitted;
    }

    /**
     * 과제의 제출 상태를 변경한다.
     *
     * @param submitted 새로운 제출 상태
     */
    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    /**
     * 미제출 과제가 마감 일시를 지났는지 확인한다.
     *
     * @param currentTime 비교 기준 시각
     * @return 과제가 미제출 상태이고 마감 일시가 {@code currentTime}보다 이전이면 {@code true}
     * @throws NullPointerException {@code currentTime}이 {@code null}인 경우
     */
    public boolean isOverdue(LocalDateTime currentTime) {
        Objects.requireNonNull(currentTime, "현재 시각은 null일 수 없습니다.");
        return !submitted && dueDateTime != null && dueDateTime.isBefore(currentTime);
    }
}
