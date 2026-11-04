package kr.ac.jnu.daymate.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * DayMate에서 하나의 일정을 표현하는 데 필요한 정보를 저장한다.
 */
public final class ScheduleData {
    private final int id;
    private String title;
    private String type;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private String memo;
    private Set<DayOfWeek> repeatDays;
    private LocalDate repeatEndDate;

    /**
     * 저장에 필요한 전체 상태를 포함하는 일정 데이터를 생성한다.
     *
     * @param id 일정 식별자. 저장 전에는 {@code 0}을 사용한다.
     * @param title 일정 제목
     * @param type 수업, 시험, 회의, 개인 약속 등의 일정 유형
     * @param date 일정의 최초 날짜
     * @param startTime 일정 시작 시각
     * @param endTime 일정 종료 시각
     * @param location 선택 입력인 장소
     * @param memo 선택 입력인 메모
     * @param repeatDays 반복할 요일. 일회성 일정이면 빈 집합을 사용한다.
     * @param repeatEndDate 반복 종료일. 일회성 일정이면 {@code null}을 사용한다.
     */
    public ScheduleData(
            int id,
            String title,
            String type,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String location,
            String memo,
            Set<DayOfWeek> repeatDays,
            LocalDate repeatEndDate
    ) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.memo = memo;
        setRepeatDays(repeatDays);
        this.repeatEndDate = repeatEndDate;
    }

    /**
     * 일정 식별자를 반환한다.
     *
     * @return 일정 식별자
     */
    public int getId() {
        return id;
    }

    /**
     * 일정 제목을 반환한다.
     *
     * @return 일정 제목
     */
    public String getTitle() {
        return title;
    }

    /**
     * 일정 제목을 변경한다.
     *
     * @param title 새로운 일정 제목
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 일정 유형을 반환한다.
     *
     * @return 일정 유형
     */
    public String getType() {
        return type;
    }

    /**
     * 일정 유형을 변경한다.
     *
     * @param type 새로운 일정 유형
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 일정의 최초 날짜를 반환한다.
     *
     * @return 일정 날짜
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * 일정의 최초 날짜를 변경한다.
     *
     * @param date 새로운 일정 날짜
     */
    public void setDate(LocalDate date) {
        this.date = date;
    }

    /**
     * 일정 시작 시각을 반환한다.
     *
     * @return 시작 시각
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * 일정 시작 시각을 변경한다.
     *
     * @param startTime 새로운 시작 시각
     */
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    /**
     * 일정 종료 시각을 반환한다.
     *
     * @return 종료 시각
     */
    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * 일정 종료 시각을 변경한다.
     *
     * @param endTime 새로운 종료 시각
     */
    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    /**
     * 일정 장소를 반환한다.
     *
     * @return 일정 장소. 입력되지 않았다면 {@code null}
     */
    public String getLocation() {
        return location;
    }

    /**
     * 일정 장소를 변경한다.
     *
     * @param location 새로운 장소
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * 일정 메모를 반환한다.
     *
     * @return 일정 메모. 입력되지 않았다면 {@code null}
     */
    public String getMemo() {
        return memo;
    }

    /**
     * 일정 메모를 변경한다.
     *
     * @param memo 새로운 메모
     */
    public void setMemo(String memo) {
        this.memo = memo;
    }

    /**
     * 외부에서 수정할 수 없는 반복 요일 집합을 반환한다.
     *
     * @return 반복 요일 집합. {@code null}을 반환하지 않는다.
     */
    public Set<DayOfWeek> getRepeatDays() {
        return Collections.unmodifiableSet(repeatDays);
    }

    /**
     * 전달받은 반복 요일을 복사하여 기존 반복 요일을 교체한다.
     *
     * @param repeatDays 반복 요일. 반복 요일을 비우려면 {@code null}을 사용한다.
     */
    public void setRepeatDays(Set<DayOfWeek> repeatDays) {
        if (repeatDays == null || repeatDays.isEmpty()) {
            this.repeatDays = EnumSet.noneOf(DayOfWeek.class);
            return;
        }
        this.repeatDays = EnumSet.copyOf(repeatDays);
    }

    /**
     * 반복 종료일을 반환한다.
     *
     * @return 반복 종료일. 일회성 일정이면 {@code null}
     */
    public LocalDate getRepeatEndDate() {
        return repeatEndDate;
    }

    /**
     * 반복 종료일을 변경한다.
     *
     * @param repeatEndDate 새로운 반복 종료일. 종료일을 비우려면 {@code null}을 사용한다.
     */
    public void setRepeatEndDate(LocalDate repeatEndDate) {
        this.repeatEndDate = repeatEndDate;
    }

    /**
     * 반복 요일과 반복 종료일이 모두 설정되었는지 확인한다.
     *
     * @return 반복 설정이 완전하면 {@code true}
     */
    public boolean isRepeated() {
        return !repeatDays.isEmpty() && repeatEndDate != null;
    }
}
