package kr.ac.jnu.daymate.controller;

import kr.ac.jnu.daymate.data.DayMateDataManager;
import kr.ac.jnu.daymate.model.ScheduleData;
import kr.ac.jnu.daymate.model.ViewType;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 월간·주간 캘린더에 표시할 일정을 조회하고 반복 일정을 발생 날짜별로 변환한다.
 */
public final class CalendarController {
    private final DayMateDataManager dataManager;

    /**
     * 지정한 데이터 관리자를 사용하는 캘린더 컨트롤러를 생성한다.
     *
     * @param dataManager 일정 캐시를 관리할 객체
     * @throws NullPointerException {@code dataManager}가 {@code null}인 경우
     */
    public CalendarController(DayMateDataManager dataManager) {
        this.dataManager = Objects.requireNonNull(dataManager, "데이터 관리자는 null일 수 없습니다.");
    }

    /**
     * 지정한 기간의 캘린더 일정을 날짜와 시작 시각 순으로 반환한다.
     * 반복 일정은 조회 기간에 해당하는 각 발생 날짜의 표시용 복사본으로 반환되며 저장에는 사용하지 않는다.
     *
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @param viewType 월간 또는 주간 표시 방식
     * @return 조회 기간에 표시할 일정 목록
     * @throws NullPointerException 인수 중 하나가 {@code null}인 경우
     * @throws IllegalArgumentException 종료일이 시작일보다 빠른 경우
     */
    public List<ScheduleData> getCalendarSchedules(
            LocalDate startDate,
            LocalDate endDate,
            ViewType viewType
    ) {
        Objects.requireNonNull(startDate, "조회 시작일은 null일 수 없습니다.");
        Objects.requireNonNull(endDate, "조회 종료일은 null일 수 없습니다.");
        Objects.requireNonNull(viewType, "캘린더 표시 방식은 null일 수 없습니다.");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("조회 종료일은 시작일보다 빠를 수 없습니다.");
        }
        return ScheduleOccurrenceResolver.resolve(dataManager.getSchedules(), startDate, endDate);
    }

    /**
     * 지정한 날짜에 발생하는 일정을 시작 시각 순으로 반환한다.
     *
     * @param date 조회할 날짜
     * @return 해당 날짜에 발생하는 일정 목록
     * @throws NullPointerException {@code date}가 {@code null}인 경우
     */
    public List<ScheduleData> getSchedulesForDate(LocalDate date) {
        Objects.requireNonNull(date, "조회 날짜는 null일 수 없습니다.");
        return ScheduleOccurrenceResolver.resolve(dataManager.getSchedules(), date, date);
    }
}
