package kr.ac.jnu.daymate.controller;

import kr.ac.jnu.daymate.data.DayMateDataManager;
import kr.ac.jnu.daymate.data.LocalDatabase;
import kr.ac.jnu.daymate.dto.ScheduleResult;
import kr.ac.jnu.daymate.model.ScheduleData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleControllerTest {
    @TempDir
    Path tempDirectory;

    private DayMateDataManager dataManager;
    private ScheduleController controller;

    @BeforeEach
    void setUp() {
        LocalDatabase database = new LocalDatabase(tempDirectory.resolve("schedule-controller.db").toString());
        dataManager = new DayMateDataManager(database);
        dataManager.loadAllData();
        controller = new ScheduleController(dataManager);
    }

    @Test
    void savesValidScheduleAndReturnsPersistedId() {
        ScheduleResult result = controller.saveSchedule(createSchedule(0));

        assertTrue(result.isSuccess());
        assertNotNull(result.getSchedule());
        assertTrue(result.getSchedule().getId() > 0);
        assertEquals(1, dataManager.getSchedules().size());
    }

    @Test
    void rejectsMissingTitle() {
        ScheduleData schedule = createSchedule(0);
        schedule.setTitle("  ");

        ScheduleResult result = controller.saveSchedule(schedule);

        assertFalse(result.isSuccess());
        assertEquals("일정 제목을 입력해 주세요.", result.getMessage());
        assertTrue(dataManager.getSchedules().isEmpty());
    }

    @Test
    void rejectsEndTimeBeforeStartTime() {
        ScheduleData schedule = createSchedule(0);
        schedule.setEndTime(LocalTime.of(8, 59));

        ScheduleResult result = controller.saveSchedule(schedule);

        assertFalse(result.isSuccess());
        assertEquals("종료 시각은 시작 시각보다 빠를 수 없습니다.", result.getMessage());
    }

    @Test
    void rejectsIncompleteRepeatSettings() {
        ScheduleData missingEndDate = createSchedule(0);
        missingEndDate.setRepeatDays(Set.of(DayOfWeek.MONDAY));

        ScheduleData missingRepeatDays = createSchedule(0);
        missingRepeatDays.setRepeatEndDate(LocalDate.of(2026, 12, 31));

        assertFalse(controller.validateSchedule(missingEndDate));
        assertFalse(controller.validateSchedule(missingRepeatDays));
    }

    @Test
    void rejectsRepeatEndDateBeforeScheduleDate() {
        ScheduleData schedule = createSchedule(0);
        schedule.setRepeatDays(Set.of(DayOfWeek.MONDAY));
        schedule.setRepeatEndDate(schedule.getDate().minusDays(1));

        ScheduleResult result = controller.saveSchedule(schedule);

        assertFalse(result.isSuccess());
        assertEquals("반복 종료일은 일정 시작일보다 빠를 수 없습니다.", result.getMessage());
    }

    @Test
    void savesCompleteRepeatSettings() {
        ScheduleData schedule = createSchedule(0);
        schedule.setRepeatDays(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
        schedule.setRepeatEndDate(LocalDate.of(2026, 12, 31));

        ScheduleResult result = controller.saveSchedule(schedule);

        assertTrue(result.isSuccess());
        assertEquals(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                result.getSchedule().getRepeatDays());
        assertEquals(LocalDate.of(2026, 12, 31), result.getSchedule().getRepeatEndDate());
    }

    @Test
    void updatesExistingSchedule() {
        ScheduleResult created = controller.saveSchedule(createSchedule(0));
        ScheduleData update = createSchedule(created.getSchedule().getId());
        update.setTitle("변경된 팀 회의");

        ScheduleResult result = controller.updateSchedule(update);

        assertTrue(result.isSuccess());
        assertEquals("변경된 팀 회의", result.getSchedule().getTitle());
        assertEquals("변경된 팀 회의", dataManager.getSchedules().get(0).getTitle());
    }

    @Test
    void rejectsUpdateForUnknownSchedule() {
        ScheduleResult result = controller.updateSchedule(createSchedule(999));

        assertFalse(result.isSuccess());
        assertEquals("수정할 일정을 찾을 수 없습니다.", result.getMessage());
        assertTrue(dataManager.getSchedules().isEmpty());
    }

    @Test
    void deletesExistingScheduleOnlyOnce() {
        ScheduleResult created = controller.saveSchedule(createSchedule(0));
        int scheduleId = created.getSchedule().getId();

        assertTrue(controller.deleteSchedule(scheduleId));
        assertFalse(controller.deleteSchedule(scheduleId));
        assertFalse(controller.deleteSchedule(0));
        assertTrue(dataManager.getSchedules().isEmpty());
    }

    private ScheduleData createSchedule(int id) {
        return new ScheduleData(
                id,
                "팀 회의",
                "회의",
                LocalDate.of(2026, 11, 6),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                "공대 1호관",
                "체크포인트 진행 상황 공유",
                Set.of(),
                null
        );
    }
}
