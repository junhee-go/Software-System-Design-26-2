package kr.ac.jnu.daymate.controller;

import kr.ac.jnu.daymate.data.DayMateDataManager;
import kr.ac.jnu.daymate.dto.DailySummary;
import kr.ac.jnu.daymate.model.AssignmentData;
import kr.ac.jnu.daymate.model.NotificationSetting;
import kr.ac.jnu.daymate.model.ScheduleData;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 일일 요약 알림 설정을 저장하고 지정 시각에 한 번만 알림을 전달한다.
 */
public final class AlertController {
    private static final int TIMER_INTERVAL_MILLIS = 30_000;
    private static final int INITIAL_DELAY_MILLIS = 1_000;

    private final DayMateDataManager dataManager;
    private final SummaryController summaryController;
    private final Consumer<DailySummary> notificationHandler;
    private Timer timer;
    private LocalDateTime lastNotificationMinute;

    /**
     * 지정한 데이터 관리자, 요약 컨트롤러 및 알림 처리 함수를 사용하는 컨트롤러를 생성한다.
     * 알림 처리 함수는 타이머가 실행되는 Swing 이벤트 디스패치 스레드에서 호출된다.
     *
     * @param dataManager 알림 설정 캐시와 저장소를 관리할 객체
     * @param summaryController 알림에 포함할 일일 요약을 생성할 컨트롤러
     * @param notificationHandler 알림 대상이 존재할 때 요약을 전달받을 함수
     * @throws NullPointerException 인수 중 하나가 {@code null}인 경우
     */
    public AlertController(
            DayMateDataManager dataManager,
            SummaryController summaryController,
            Consumer<DailySummary> notificationHandler
    ) {
        this.dataManager = Objects.requireNonNull(dataManager, "데이터 관리자는 null일 수 없습니다.");
        this.summaryController = Objects.requireNonNull(summaryController, "요약 컨트롤러는 null일 수 없습니다.");
        this.notificationHandler = Objects.requireNonNull(
                notificationHandler,
                "알림 처리 함수는 null일 수 없습니다."
        );
    }

    /**
     * 현재 캐시에 저장된 일일 요약 알림 설정을 반환한다.
     *
     * @return 외부 변경으로부터 분리된 알림 설정
     */
    public NotificationSetting loadNotificationSettings() {
        return dataManager.getNotificationSetting();
    }

    /**
     * 유효한 일일 요약 알림 설정을 저장한다.
     * 비활성화된 설정은 알림 시각이나 포함 항목이 없어도 저장할 수 있다.
     *
     * @param setting 저장할 알림 설정
     * @return 설정이 유효하고 저장되면 {@code true}
     */
    public boolean saveNotificationSettings(NotificationSetting setting) {
        return setting != null && setting.isValid() && dataManager.saveNotificationSetting(setting);
    }

    /**
     * 저장된 설정에 따라 일일 요약 타이머를 시작한다.
     * 기존 타이머가 있으면 먼저 중지하므로 여러 번 호출해도 하나의 타이머만 유지된다.
     */
    public void startTimer() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::startTimer);
            return;
        }
        stopTimerOnEventDispatchThread();
        NotificationSetting setting = loadNotificationSettings();
        if (!setting.isEnabled() || !setting.isValid()) {
            return;
        }

        timer = new Timer(TIMER_INTERVAL_MILLIS, event -> checkAndNotify(LocalDateTime.now()));
        timer.setInitialDelay(INITIAL_DELAY_MILLIS);
        timer.setRepeats(true);
        timer.start();
    }

    /**
     * 실행 중인 일일 요약 타이머를 중지한다.
     */
    public void stopTimer() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::stopTimer);
            return;
        }
        stopTimerOnEventDispatchThread();
    }

    /**
     * 기준 시각과 알림 설정을 비교하여 알림 대상이 있으면 일일 요약을 한 번 전달한다.
     * 과제는 미제출 상태이면서 기준 시각부터 3일 이내에 마감되거나 이미 기한이 지난 항목을 포함한다.
     * 같은 날짜와 분에는 중복 알림을 전달하지 않는다.
     *
     * @param currentDateTime 알림 여부를 검사할 기준 시각
     * @return 실제로 알림 요약이 전달되면 {@code true}
     * @throws NullPointerException {@code currentDateTime}이 {@code null}인 경우
     */
    public synchronized boolean checkAndNotify(LocalDateTime currentDateTime) {
        Objects.requireNonNull(currentDateTime, "기준 시각은 null일 수 없습니다.");
        NotificationSetting setting = loadNotificationSettings();
        if (!setting.isEnabled() || !setting.isValid() || !isAlertTime(setting, currentDateTime)) {
            return false;
        }

        LocalDateTime currentMinute = currentDateTime.truncatedTo(ChronoUnit.MINUTES);
        if (currentMinute.equals(lastNotificationMinute)) {
            return false;
        }

        DailySummary fullSummary = summaryController.createDailySummary(currentDateTime);
        List<ScheduleData> schedules = setting.isIncludeSchedule()
                ? fullSummary.getSchedules()
                : List.of();
        List<AssignmentData> assignments = setting.isIncludeAssignment()
                ? fullSummary.getAssignments().stream()
                .filter(assignment -> !assignment.getDueDateTime().isAfter(currentDateTime.plusDays(3)))
                .toList()
                : List.of();
        DailySummary notificationSummary = new DailySummary(schedules, assignments);

        lastNotificationMinute = currentMinute;
        if (!notificationSummary.hasItems()) {
            return false;
        }
        notificationHandler.accept(notificationSummary);
        return true;
    }

    Timer getTimer() {
        return timer;
    }

    private boolean isAlertTime(NotificationSetting setting, LocalDateTime currentDateTime) {
        LocalTime alertTime = setting.getAlertTime().truncatedTo(ChronoUnit.MINUTES);
        LocalTime currentTime = currentDateTime.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        return alertTime.equals(currentTime);
    }

    private void stopTimerOnEventDispatchThread() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }
}
