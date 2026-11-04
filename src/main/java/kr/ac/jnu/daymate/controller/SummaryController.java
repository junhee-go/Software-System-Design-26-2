package kr.ac.jnu.daymate.controller;

import kr.ac.jnu.daymate.data.DayMateDataManager;
import kr.ac.jnu.daymate.dto.DailySummary;
import kr.ac.jnu.daymate.model.AssignmentData;
import kr.ac.jnu.daymate.model.ScheduleData;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 오늘의 일정과 미제출 과제를 정렬하여 일일 요약을 생성한다.
 */
public final class SummaryController {
    private final DayMateDataManager dataManager;

    /**
     * 지정한 데이터 관리자를 사용하는 일일 요약 컨트롤러를 생성한다.
     *
     * @param dataManager 일정과 과제 캐시를 관리할 객체
     * @throws NullPointerException {@code dataManager}가 {@code null}인 경우
     */
    public SummaryController(DayMateDataManager dataManager) {
        this.dataManager = Objects.requireNonNull(dataManager, "데이터 관리자는 null일 수 없습니다.");
    }

    /**
     * 기준 시각이 속한 날짜의 일정과 모든 미제출 과제를 일일 요약으로 생성한다.
     * 일정은 시작 시각 순으로, 과제는 마감 일시가 가까운 순으로 정렬한다.
     *
     * @param currentDateTime 요약을 계산할 기준 시각
     * @return 오늘의 일정과 미제출 과제를 포함한 요약
     * @throws NullPointerException {@code currentDateTime}이 {@code null}인 경우
     */
    public DailySummary createDailySummary(LocalDateTime currentDateTime) {
        Objects.requireNonNull(currentDateTime, "기준 시각은 null일 수 없습니다.");

        List<ScheduleData> schedules = ScheduleOccurrenceResolver.resolve(
                dataManager.getSchedules(),
                currentDateTime.toLocalDate(),
                currentDateTime.toLocalDate()
        );
        List<AssignmentData> assignments = dataManager.getAssignments().stream()
                .filter(assignment -> !assignment.isSubmitted())
                .sorted(Comparator
                        .comparing(AssignmentData::getDueDateTime)
                        .thenComparing(AssignmentData::getTitle))
                .toList();
        return new DailySummary(schedules, assignments);
    }
}
