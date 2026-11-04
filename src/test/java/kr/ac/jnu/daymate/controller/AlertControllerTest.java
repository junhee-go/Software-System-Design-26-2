package kr.ac.jnu.daymate.controller;

import kr.ac.jnu.daymate.data.DayMateDataManager;
import kr.ac.jnu.daymate.data.LocalDatabase;
import kr.ac.jnu.daymate.dto.DailySummary;
import kr.ac.jnu.daymate.model.AssignmentData;
import kr.ac.jnu.daymate.model.CourseData;
import kr.ac.jnu.daymate.model.NotificationSetting;
import kr.ac.jnu.daymate.model.ScheduleData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertControllerTest {
    @TempDir
    Path tempDirectory;

    private DayMateDataManager dataManager;
    private List<DailySummary> notifications;
    private AlertController controller;

    @BeforeEach
    void setUp() {
        LocalDatabase database = new LocalDatabase(tempDirectory.resolve("alerts.db").toString());
        dataManager = new DayMateDataManager(database);
        dataManager.loadAllData();
        notifications = new ArrayList<>();
        controller = new AlertController(dataManager, new SummaryController(dataManager), notifications::add);
    }

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(controller::stopTimer);
    }

    @Test
    void savesAndLoadsValidNotificationSettings() {
        NotificationSetting setting = new NotificationSetting(true, LocalTime.of(8, 30), true, false);

        assertTrue(controller.saveNotificationSettings(setting));
        NotificationSetting loaded = controller.loadNotificationSettings();
        assertTrue(loaded.isEnabled());
        assertEquals(LocalTime.of(8, 30), loaded.getAlertTime());
        assertTrue(loaded.isIncludeSchedule());
        assertFalse(loaded.isIncludeAssignment());
    }

    @Test
    void rejectsInvalidEnabledSettings() {
        assertFalse(controller.saveNotificationSettings(
                new NotificationSetting(true, null, true, true)
        ));
        assertFalse(controller.saveNotificationSettings(
                new NotificationSetting(true, LocalTime.of(8, 0), false, false)
        ));
        assertFalse(controller.loadNotificationSettings().isEnabled());
    }

    @Test
    void deliversNotificationOnlyOnceInConfiguredMinute() {
        LocalDateTime alertTime = LocalDateTime.of(2026, 11, 5, 8, 0);
        saveTodaySchedule(alertTime.toLocalDate());
        assertTrue(controller.saveNotificationSettings(
                new NotificationSetting(true, alertTime.toLocalTime(), true, false)
        ));

        assertTrue(controller.checkAndNotify(alertTime.withSecond(5)));
        assertFalse(controller.checkAndNotify(alertTime.withSecond(40)));
        assertEquals(1, notifications.size());
        assertEquals(1, notifications.get(0).getSchedules().size());
    }

    @Test
    void includesOverdueAndThreeDayAssignmentsButExcludesLaterAndSubmittedItems() {
        LocalDateTime alertTime = LocalDateTime.of(2026, 11, 5, 8, 0);
        int courseId = saveCourse();
        saveAssignment("기한 초과", courseId, alertTime.minusDays(1), false);
        saveAssignment("3일 이내", courseId, alertTime.plusDays(3), false);
        saveAssignment("4일 이후", courseId, alertTime.plusDays(4), false);
        saveAssignment("제출 완료", courseId, alertTime.plusDays(1), true);
        assertTrue(controller.saveNotificationSettings(
                new NotificationSetting(true, alertTime.toLocalTime(), false, true)
        ));

        assertTrue(controller.checkAndNotify(alertTime));

        assertEquals(1, notifications.size());
        assertTrue(notifications.get(0).getSchedules().isEmpty());
        assertEquals(List.of("기한 초과", "3일 이내"),
                notifications.get(0).getAssignments().stream().map(AssignmentData::getTitle).toList());
    }

    @Test
    void skipsNotificationAtWrongTimeOrWithoutTargets() {
        LocalDateTime alertTime = LocalDateTime.of(2026, 11, 5, 8, 0);
        assertTrue(controller.saveNotificationSettings(
                new NotificationSetting(true, alertTime.toLocalTime(), true, true)
        ));

        assertFalse(controller.checkAndNotify(alertTime.minusMinutes(1)));
        assertFalse(controller.checkAndNotify(alertTime));
        assertTrue(notifications.isEmpty());
    }

    @Test
    void restartingTimerStopsPreviousTimerAndKeepsOneRunning() throws Exception {
        assertTrue(controller.saveNotificationSettings(
                new NotificationSetting(true, LocalTime.of(8, 0), true, false)
        ));

        SwingUtilities.invokeAndWait(controller::startTimer);
        Timer firstTimer = controller.getTimer();
        SwingUtilities.invokeAndWait(controller::startTimer);
        Timer secondTimer = controller.getTimer();

        assertFalse(firstTimer.isRunning());
        assertNotSame(firstTimer, secondTimer);
        assertTrue(secondTimer.isRunning());

        SwingUtilities.invokeAndWait(controller::stopTimer);
        assertNull(controller.getTimer());
    }

    private void saveTodaySchedule(LocalDate date) {
        assertTrue(dataManager.saveSchedule(new ScheduleData(
                0,
                "오늘 수업",
                "수업",
                date,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                null,
                null,
                Set.of(),
                null
        )));
    }

    private int saveCourse() {
        assertTrue(dataManager.saveCourse(new CourseData(0, "소프트웨어 시스템 설계", "김미수")));
        return dataManager.getCourses().get(0).getId();
    }

    private void saveAssignment(
            String title,
            int courseId,
            LocalDateTime dueDateTime,
            boolean submitted
    ) {
        assertTrue(dataManager.saveAssignment(new AssignmentData(
                0,
                title,
                courseId,
                dueDateTime,
                null,
                submitted
        )));
    }
}
