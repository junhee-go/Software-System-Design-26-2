package kr.ac.jnu.daymate.dto;

import kr.ac.jnu.daymate.model.AssignmentData;
import kr.ac.jnu.daymate.model.ScheduleData;

import java.util.List;

/**
 * 일일 요약 화면에 표시할 일정과 과제를 하나로 묶는다.
 */
public final class DailySummary {
    private final List<ScheduleData> schedules;
    private final List<AssignmentData> assignments;

    /**
     * 일정과 과제 목록을 각각 복사하여 일일 요약을 생성한다.
     *
     * @param schedules 표시할 일정 목록. {@code null}은 빈 목록으로 처리한다.
     * @param assignments 표시할 과제 목록. {@code null}은 빈 목록으로 처리한다.
     */
    public DailySummary(List<ScheduleData> schedules, List<AssignmentData> assignments) {
        this.schedules = schedules == null ? List.of() : List.copyOf(schedules);
        this.assignments = assignments == null ? List.of() : List.copyOf(assignments);
    }

    /**
     * 외부에서 수정할 수 없는 일정 목록을 반환한다.
     *
     * @return 요약에 포함된 일정 목록
     */
    public List<ScheduleData> getSchedules() {
        return schedules;
    }

    /**
     * 외부에서 수정할 수 없는 과제 목록을 반환한다.
     *
     * @return 요약에 포함된 과제 목록
     */
    public List<AssignmentData> getAssignments() {
        return assignments;
    }

    /**
     * 요약에 일정이나 과제가 하나 이상 존재하는지 확인한다.
     *
     * @return 하나 이상의 항목이 존재하면 {@code true}
     */
    public boolean hasItems() {
        return !schedules.isEmpty() || !assignments.isEmpty();
    }
}
