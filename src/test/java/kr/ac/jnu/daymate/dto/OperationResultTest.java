package kr.ac.jnu.daymate.dto;

import kr.ac.jnu.daymate.model.AssignmentData;
import kr.ac.jnu.daymate.model.ScheduleData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationResultTest {
    @Test
    void scheduleResultExposesSuccessData() {
        ScheduleData schedule = createSchedule();
        ScheduleResult result = new ScheduleResult(true, "Saved", schedule);

        assertTrue(result.isSuccess());
        assertEquals("Saved", result.getMessage());
        assertSame(schedule, result.getSchedule());
    }

    @Test
    void assignmentResultCanRepresentFailureWithoutData() {
        AssignmentResult result = new AssignmentResult(false, "Invalid deadline", null);

        assertFalse(result.isSuccess());
        assertEquals("Invalid deadline", result.getMessage());
        assertNull(result.getAssignment());
    }

    @Test
    void assignmentResultExposesSuccessData() {
        AssignmentData assignment = new AssignmentData(
                1,
                "Report",
                2,
                LocalDateTime.of(2026, 11, 5, 18, 0),
                null,
                false
        );
        AssignmentResult result = new AssignmentResult(true, "Saved", assignment);

        assertTrue(result.isSuccess());
        assertSame(assignment, result.getAssignment());
    }

    private ScheduleData createSchedule() {
        return new ScheduleData(
                1,
                "Exam",
                "EXAM",
                LocalDate.of(2026, 11, 10),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                null,
                null,
                Set.of(),
                null
        );
    }
}
