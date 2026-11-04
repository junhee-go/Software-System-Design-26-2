package kr.ac.jnu.daymate.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentDataTest {
    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 11, 5, 23, 59);

    @Test
    void unsubmittedAssignmentBecomesOverdueAfterDeadline() {
        AssignmentData assignment = createAssignment(false);

        assertFalse(assignment.isOverdue(DEADLINE));
        assertTrue(assignment.isOverdue(DEADLINE.plusSeconds(1)));
    }

    @Test
    void submittedAssignmentIsNotOverdue() {
        AssignmentData assignment = createAssignment(true);

        assertFalse(assignment.isOverdue(DEADLINE.plusDays(1)));
    }

    @Test
    void submissionStateCanBeChanged() {
        AssignmentData assignment = createAssignment(false);

        assignment.setSubmitted(true);

        assertTrue(assignment.isSubmitted());
    }

    @Test
    void rejectsNullComparisonTime() {
        AssignmentData assignment = createAssignment(false);

        assertThrows(NullPointerException.class, () -> assignment.isOverdue(null));
    }

    private AssignmentData createAssignment(boolean submitted) {
        return new AssignmentData(
                1,
                "Checkpoint 4",
                10,
                DEADLINE,
                "Submit the PDF and source archive",
                submitted
        );
    }
}
