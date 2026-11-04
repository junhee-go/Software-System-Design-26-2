package kr.ac.jnu.daymate.model;

import java.time.LocalTime;

/**
 * 사용자의 일일 요약 알림 설정을 저장한다.
 */
public final class NotificationSetting {
    private boolean enabled;
    private LocalTime alertTime;
    private boolean includeSchedule;
    private boolean includeAssignment;

    /**
     * 알림 설정을 생성한다.
     *
     * @param enabled 일일 알림 활성화 여부
     * @param alertTime 알림 시각. 비활성화 상태에서는 {@code null}일 수 있다.
     * @param includeSchedule 요약에 일정을 포함할지 여부
     * @param includeAssignment 요약에 과제를 포함할지 여부
     */
    public NotificationSetting(
            boolean enabled,
            LocalTime alertTime,
            boolean includeSchedule,
            boolean includeAssignment
    ) {
        this.enabled = enabled;
        this.alertTime = alertTime;
        this.includeSchedule = includeSchedule;
        this.includeAssignment = includeAssignment;
    }

    /**
     * 알림이 활성화되어 있는지 확인한다.
     *
     * @return 활성화 상태이면 {@code true}
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 알림을 활성화하거나 비활성화한다.
     *
     * @param enabled 새로운 활성화 상태
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 설정된 알림 시각을 반환한다.
     *
     * @return 알림 시각. 설정되지 않았다면 {@code null}
     */
    public LocalTime getAlertTime() {
        return alertTime;
    }

    /**
     * 알림 시각을 변경한다.
     *
     * @param alertTime 새로운 알림 시각
     */
    public void setAlertTime(LocalTime alertTime) {
        this.alertTime = alertTime;
    }

    /**
     * 알림에 일정을 포함하는지 확인한다.
     *
     * @return 일정을 포함하면 {@code true}
     */
    public boolean isIncludeSchedule() {
        return includeSchedule;
    }

    /**
     * 알림에 일정을 포함할지 변경한다.
     *
     * @param includeSchedule 새로운 일정 포함 상태
     */
    public void setIncludeSchedule(boolean includeSchedule) {
        this.includeSchedule = includeSchedule;
    }

    /**
     * 알림에 과제를 포함하는지 확인한다.
     *
     * @return 과제를 포함하면 {@code true}
     */
    public boolean isIncludeAssignment() {
        return includeAssignment;
    }

    /**
     * 알림에 과제를 포함할지 변경한다.
     *
     * @param includeAssignment 새로운 과제 포함 상태
     */
    public void setIncludeAssignment(boolean includeAssignment) {
        this.includeAssignment = includeAssignment;
    }

    /**
     * 알림을 표시하는 데 필요한 설정이 유효한지 검사한다.
     * 비활성화된 설정은 항상 유효하다. 활성화된 설정에는 알림 시각과 하나 이상의 요약 항목이 필요하다.
     *
     * @return 저장할 수 있는 설정이면 {@code true}
     */
    public boolean isValid() {
        return !enabled || (alertTime != null && (includeSchedule || includeAssignment));
    }
}
