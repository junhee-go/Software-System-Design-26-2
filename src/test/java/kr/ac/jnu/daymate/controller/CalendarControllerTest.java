package kr.ac.jnu.daymate.controller;

import kr.ac.jnu.daymate.data.DayMateDataManager;
import kr.ac.jnu.daymate.data.LocalDatabase;
import kr.ac.jnu.daymate.model.ScheduleData;
import kr.ac.jnu.daymate.model.ViewType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarControllerTest {
    @TempDir
    Path tempDirectory;

    private DayMateDataManager dataManager;
    private CalendarController controller;

    @BeforeEach
    void setUp() {
        LocalDatabase database = new LocalDatabase(tempDirectory.resolve("calendar.db").toString());
        dataManager = new DayMateDataManager(database);
        dataManager.loadAllData();
        controller = new CalendarController(dataManager);
    }

    @Test
    void returnsSchedulesWithinRangeInDateAndTimeOrder() {
        saveSchedule("오후 회의", LocalDate.of(2026, 11, 4), LocalTime.of(14, 0));
        saveSchedule("오전 수업", LocalDate.of(2026, 11, 4), LocalTime.of(9, 0));
        saveSchedule("범위 밖 일정", LocalDate.of(2026, 12, 1), LocalTime.of(9, 0));

        List<ScheduleData> schedules = controller.getCalendarSchedules(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30),
                ViewType.MONTH
        );

        assertEquals(List.of("오전 수업", "오후 회의"),
                schedules.stream().map(ScheduleData::getTitle).toList());
    }

    @Test
    void expandsRepeatedScheduleForSelectedWeekdaysUntilEndDate() {
        ScheduleData repeated = new ScheduleData(
                0,
                "정기 회의",
                "회의",
                LocalDate.of(2026, 11, 2),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                null,
                null,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 11, 11)
        );
        assertTrue(dataManager.saveSchedule(repeated));

        List<ScheduleData> schedules = controller.getCalendarSchedules(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30),
                ViewType.MONTH
        );

        assertEquals(
                List.of(
                        LocalDate.of(2026, 11, 2),
                        LocalDate.of(2026, 11, 4),
                        LocalDate.of(2026, 11, 9),
                        LocalDate.of(2026, 11, 11)
                ),
                schedules.stream().map(ScheduleData::getDate).toList()
        );
        assertEquals(1, dataManager.getSchedules().size());
    }

    @Test
    void returnsRepeatedOccurrenceWhenInitialDateIsBeforeRange() {
        ScheduleData repeated = new ScheduleData(
                0,
                "월요일 수업",
                "수업",
                LocalDate.of(2026, 10, 26),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                null,
                null,
                Set.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 11, 30)
        );
        assertTrue(dataManager.saveSchedule(repeated));

        List<ScheduleData> schedules = controller.getSchedulesForDate(LocalDate.of(2026, 11, 9));

        assertEquals(1, schedules.size());
        assertEquals(LocalDate.of(2026, 11, 9), schedules.get(0).getDate());
    }

    @Test
    void rejectsInvalidDateRange() {
        assertThrows(IllegalArgumentException.class, () -> controller.getCalendarSchedules(
                LocalDate.of(2026, 11, 30),
                LocalDate.of(2026, 11, 1),
                ViewType.MONTH
        ));
    }

    private void saveSchedule(String title, LocalDate date, LocalTime startTime) {
        assertTrue(dataManager.saveSchedule(new ScheduleData(
                0,
                title,
                "회의",
                date,
                startTime,
                startTime.plusHours(1),
                null,
                null,
                Set.of(),
                null
        )));
    }
}
