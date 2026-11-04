package kr.ac.jnu.daymate.dto;

import kr.ac.jnu.daymate.model.ScheduleData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailySummaryTest {
    @Test
    void reportsWhetherAnyItemsExist() {
        DailySummary emptySummary = new DailySummary(null, null);
        DailySummary scheduleSummary = new DailySummary(List.of(createSchedule()), List.of());

        assertFalse(emptySummary.hasItems());
        assertTrue(scheduleSummary.hasItems());
    }

    @Test
    void copiesAndProtectsSummaryLists() {
        List<ScheduleData> source = new ArrayList<>();
        source.add(createSchedule());
        DailySummary summary = new DailySummary(source, List.of());

        source.clear();

        assertTrue(summary.hasItems());
        assertThrows(UnsupportedOperationException.class, () -> summary.getSchedules().clear());
    }

    private ScheduleData createSchedule() {
        return new ScheduleData(
                1,
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
    }
}
