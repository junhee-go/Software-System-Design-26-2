package kr.ac.jnu.daymate.dto;

import kr.ac.jnu.daymate.model.ScheduleData;

/**
 * 일정 처리 결과를 전달한다.
 */
public final class ScheduleResult {
    private final boolean success;
    private final String message;
    private final ScheduleData schedule;

    /**
     * 일정 처리 결과를 생성한다.
     *
     * @param success 처리 성공 여부
     * @param message 사용자에게 표시할 결과 메시지
     * @param schedule 처리된 일정. 반환할 일정이 없다면 {@code null}
     */
    public ScheduleResult(boolean success, String message, ScheduleData schedule) {
        this.success = success;
        this.message = message;
        this.schedule = schedule;
    }

    /**
     * 처리가 성공했는지 확인한다.
     *
     * @return 성공한 경우 {@code true}
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 결과 메시지를 반환한다.
     *
     * @return 결과 메시지
     */
    public String getMessage() {
        return message;
    }

    /**
     * 처리된 일정을 반환한다.
     *
     * @return 처리된 일정. 일정이 없다면 {@code null}
     */
    public ScheduleData getSchedule() {
        return schedule;
    }
}
