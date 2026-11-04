package kr.ac.jnu.daymate.data;

import kr.ac.jnu.daymate.model.AssignmentData;
import kr.ac.jnu.daymate.model.CourseData;
import kr.ac.jnu.daymate.model.NotificationSetting;
import kr.ac.jnu.daymate.model.ScheduleData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalDatabaseTest {
    @TempDir
    Path tempDirectory;

    private Path databaseFile;
    private LocalDatabase database;

    @BeforeEach
    void setUp() {
        databaseFile = tempDirectory.resolve("nested").resolve("daymate-test.db");
        database = new LocalDatabase(databaseFile.toString());
        database.initialize();
    }

    @Test
    void initializesDatabaseFileAndEmptyData() {
        assertTrue(Files.exists(databaseFile));
        assertTrue(database.loadSchedules().isEmpty());
        assertTrue(database.loadCourses().isEmpty());
        assertTrue(database.loadAssignments().isEmpty());
        assertFalse(database.loadNotificationSetting().isEnabled());
    }

    @Test
    void synchronizesSchedulesAndRepeatDays() {
        ScheduleData newSchedule = new ScheduleData(
                0,
                "Software System Design",
                "CLASS",
                LocalDate.of(2026, 11, 4),
                LocalTime.of(10, 0),
                LocalTime.of(11, 30),
                "Engineering Building",
                "Chapter 4",
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 12, 16)
        );

        assertTrue(database.saveSchedules(List.of(newSchedule)));
        ScheduleData saved = database.loadSchedules().get(0);

        assertNotEquals(0, saved.getId());
        assertEquals(newSchedule.getTitle(), saved.getTitle());
        assertEquals(newSchedule.getRepeatDays(), saved.getRepeatDays());
        assertEquals(newSchedule.getRepeatEndDate(), saved.getRepeatEndDate());

        ScheduleData updated = new ScheduleData(
                saved.getId(),
                "Updated Class",
                saved.getType(),
                saved.getDate(),
                saved.getStartTime(),
                saved.getEndTime(),
                saved.getLocation(),
                saved.getMemo(),
                EnumSet.of(DayOfWeek.FRIDAY),
                saved.getRepeatEndDate()
        );

        assertTrue(database.saveSchedules(List.of(updated)));
        assertEquals("Updated Class", database.loadSchedules().get(0).getTitle());
        assertEquals(EnumSet.of(DayOfWeek.FRIDAY), database.loadSchedules().get(0).getRepeatDays());

        assertTrue(database.saveSchedules(List.of()));
        assertTrue(database.loadSchedules().isEmpty());
    }

    @Test
    void enforcesCourseAssignmentForeignKey() {
        assertTrue(database.saveCourses(List.of(new CourseData(0, "Software System Design", "Kim"))));
        CourseData savedCourse = database.loadCourses().get(0);
        AssignmentData assignment = new AssignmentData(
                0,
                "Checkpoint 4",
                savedCourse.getId(),
                LocalDateTime.of(2026, 11, 5, 23, 59),
                null,
                false
        );

        assertTrue(database.saveAssignments(List.of(assignment)));
        assertFalse(database.saveCourses(List.of()));
        assertEquals(1, database.loadCourses().size());
        assertEquals(savedCourse.getId(), database.loadAssignments().get(0).getCourseId());

        assertTrue(database.saveAssignments(List.of()));
        assertTrue(database.saveCourses(List.of()));
        assertTrue(database.loadCourses().isEmpty());
    }

    @Test
    void rejectsAssignmentForUnknownCourse() {
        AssignmentData assignment = new AssignmentData(
                0,
                "Invalid Assignment",
                999,
                LocalDateTime.of(2026, 11, 5, 23, 59),
                null,
                false
        );

        assertFalse(database.saveAssignments(List.of(assignment)));
        assertTrue(database.loadAssignments().isEmpty());
    }

    @Test
    void savesAndUpdatesNotificationSetting() {
        NotificationSetting setting = new NotificationSetting(
                true,
                LocalTime.of(9, 30),
                true,
                false
        );

        assertTrue(database.saveNotificationSetting(setting));
        NotificationSetting loaded = database.loadNotificationSetting();

        assertTrue(loaded.isEnabled());
        assertEquals(LocalTime.of(9, 30), loaded.getAlertTime());
        assertTrue(loaded.isIncludeSchedule());
        assertFalse(loaded.isIncludeAssignment());

        setting.setEnabled(false);
        setting.setAlertTime(null);
        assertTrue(database.saveNotificationSetting(setting));
        assertFalse(database.loadNotificationSetting().isEnabled());
    }
}
