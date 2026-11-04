package kr.ac.jnu.daymate.data;

import kr.ac.jnu.daymate.model.AssignmentData;
import kr.ac.jnu.daymate.model.CourseData;
import kr.ac.jnu.daymate.model.NotificationSetting;
import kr.ac.jnu.daymate.model.ScheduleData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DayMateDataManagerTest {
    @TempDir
    Path tempDirectory;

    private LocalDatabase database;
    private DayMateDataManager dataManager;

    @BeforeEach
    void setUp() {
        database = new LocalDatabase(tempDirectory.resolve("daymate-manager.db").toString());
        dataManager = new DayMateDataManager(database);
        dataManager.loadAllData();
    }

    @Test
    void savesReloadsAndDeletesSchedule() {
        ScheduleData schedule = new ScheduleData(
                0,
                "Team Meeting",
                "MEETING",
                LocalDate.of(2026, 11, 4),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                null,
                null,
                Set.of(),
                null
        );

        assertTrue(dataManager.saveSchedule(schedule));
        ScheduleData cached = dataManager.getSchedules().get(0);
        assertNotEquals(0, cached.getId());

        DayMateDataManager reloadedManager = new DayMateDataManager(database);
        reloadedManager.loadAllData();
        assertEquals("Team Meeting", reloadedManager.getSchedules().get(0).getTitle());

        assertTrue(dataManager.deleteSchedule(cached.getId()));
        assertTrue(dataManager.getSchedules().isEmpty());
        assertFalse(dataManager.deleteSchedule(cached.getId()));
    }

    @Test
    void keepsCacheDetachedFromReturnedObjects() {
        assertTrue(dataManager.saveCourse(new CourseData(0, "Original Course", "Kim")));

        CourseData external = dataManager.getCourses().get(0);
        external.updateInfo("Changed Outside", "Lee");

        assertEquals("Original Course", dataManager.getCourses().get(0).getName());
        assertEquals("Original Course", database.loadCourses().get(0).getName());
    }

    @Test
    void preservesCourseCacheWhenForeignKeyDeletionFails() {
        assertTrue(dataManager.saveCourse(new CourseData(0, "Software System Design", "Kim")));
        CourseData course = dataManager.getCourses().get(0);
        assertTrue(dataManager.saveAssignment(new AssignmentData(
                0,
                "Checkpoint 4",
                course.getId(),
                LocalDateTime.of(2026, 11, 5, 23, 59),
                null,
                false
        )));

        assertFalse(dataManager.deleteCourse(course.getId()));

        assertEquals(1, dataManager.getCourses().size());
        assertEquals(1, dataManager.getAssignments().size());
    }

    @Test
    void failedAssignmentSaveDoesNotChangeCache() {
        AssignmentData invalidAssignment = new AssignmentData(
                0,
                "Invalid Assignment",
                999,
                LocalDateTime.of(2026, 11, 5, 23, 59),
                null,
                false
        );

        assertFalse(dataManager.saveAssignment(invalidAssignment));
        assertTrue(dataManager.getAssignments().isEmpty());
    }

    @Test
    void savesNotificationSettingAndReturnsDetachedCopy() {
        NotificationSetting setting = new NotificationSetting(
                true,
                LocalTime.of(8, 0),
                true,
                true
        );

        assertTrue(dataManager.saveNotificationSetting(setting));
        NotificationSetting external = dataManager.getNotificationSetting();
        external.setEnabled(false);

        assertTrue(dataManager.getNotificationSetting().isEnabled());

        DayMateDataManager reloadedManager = new DayMateDataManager(database);
        reloadedManager.loadAllData();
        assertTrue(reloadedManager.getNotificationSetting().isEnabled());
        assertEquals(LocalTime.of(8, 0), reloadedManager.getNotificationSetting().getAlertTime());
    }
}
