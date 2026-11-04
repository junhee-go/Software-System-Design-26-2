package kr.ac.jnu.daymate.controller;

import kr.ac.jnu.daymate.data.DayMateDataManager;
import kr.ac.jnu.daymate.data.LocalDatabase;
import kr.ac.jnu.daymate.dto.DailySummary;
import kr.ac.jnu.daymate.model.AssignmentData;
import kr.ac.jnu.daymate.model.CourseData;
import kr.ac.jnu.daymate.model.ScheduleData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummaryControllerTest {
    @TempDir
    Path tempDirectory;

    private DayMateDataManager dataManager;
    private SummaryController controller;

    @BeforeEach
    void setUp() {
        LocalDatabase database = new LocalDatabase(tempDirectory.resolve("summary.db").toString());
        dataManager = new DayMateDataManager(database);
        dataManager.loadAllData();
        controller = new SummaryController(dataManager);
    }

    @Test
    void includesTodaySchedulesAndRepeatedOccurrencesInTimeOrder() {
        saveSchedule("오후 일정", LocalDate.of(2026, 11, 4), LocalTime.of(15, 0), Set.of(), null);
        saveSchedule(
                "반복 수업",
                LocalDate.of(2026, 10, 28),
                LocalTime.of(9, 0),
                Set.of(DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 12, 30)
        );
        saveSchedule("내일 일정", LocalDate.of(2026, 11, 5), LocalTime.of(8, 0), Set.of(), null);

        DailySummary summary = controller.createDailySummary(LocalDateTime.of(2026, 11, 4, 12, 0));

        assertEquals(List.of("반복 수업", "오후 일정"),
                summary.getSchedules().stream().map(ScheduleData::getTitle).toList());
    }

    @Test
    void includesOnlyUnsubmittedAssignmentsInDeadlineOrder() {
        assertTrue(dataManager.saveCourse(new CourseData(0, "소프트웨어 시스템 설계", "김미수")));
        int courseId = dataManager.getCourses().get(0).getId();
        assertTrue(dataManager.saveAssignment(new AssignmentData(
                0,
                "나중 과제",
                courseId,
                LocalDateTime.of(2026, 11, 10, 23, 59),
                null,
                false
        )));
        assertTrue(dataManager.saveAssignment(new AssignmentData(
                0,
                "기한 초과 과제",
                courseId,
                LocalDateTime.of(2026, 11, 1, 23, 59),
                null,
                false
        )));
        assertTrue(dataManager.saveAssignment(new AssignmentData(
                0,
                "제출한 과제",
                courseId,
                LocalDateTime.of(2026, 11, 2, 23, 59),
                null,
                true
        )));

        DailySummary summary = controller.createDailySummary(LocalDateTime.of(2026, 11, 4, 12, 0));

        assertEquals(List.of("기한 초과 과제", "나중 과제"),
                summary.getAssignments().stream().map(AssignmentData::getTitle).toList());
    }

    @Test
    void createsEmptySummaryWhenNoItemsExist() {
        DailySummary summary = controller.createDailySummary(LocalDateTime.of(2026, 11, 4, 12, 0));

        assertTrue(summary.getSchedules().isEmpty());
        assertTrue(summary.getAssignments().isEmpty());
    }

    private void saveSchedule(
            String title,
            LocalDate date,
            LocalTime startTime,
            Set<DayOfWeek> repeatDays,
            LocalDate repeatEndDate
    ) {
        assertTrue(dataManager.saveSchedule(new ScheduleData(
                0,
                title,
                "수업",
                date,
                startTime,
                startTime.plusHours(1),
                null,
                null,
                repeatDays,
                repeatEndDate
        )));
    }
}
