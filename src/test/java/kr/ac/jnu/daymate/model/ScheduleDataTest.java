package kr.ac.jnu.daymate.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleDataTest {
    @Test
    void reportsRepeatedOnlyWhenDaysAndEndDateAreConfigured() {
        ScheduleData schedule = createSchedule(
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 12, 31)
        );

        assertTrue(schedule.isRepeated());

        schedule.setRepeatEndDate(null);
        assertFalse(schedule.isRepeated());

        schedule.setRepeatEndDate(LocalDate.of(2026, 12, 31));
        schedule.setRepeatDays(Set.of());
        assertFalse(schedule.isRepeated());
    }

    @Test
    void copiesAndProtectsRepeatDays() {
        EnumSet<DayOfWeek> source = EnumSet.of(DayOfWeek.TUESDAY);
        ScheduleData schedule = createSchedule(source, LocalDate.of(2026, 12, 31));

        source.add(DayOfWeek.THURSDAY);

        assertEquals(Set.of(DayOfWeek.TUESDAY), schedule.getRepeatDays());
        assertThrows(UnsupportedOperationException.class,
                () -> schedule.getRepeatDays().add(DayOfWeek.FRIDAY));
    }

    @Test
    void treatsNullRepeatDaysAsEmpty() {
        ScheduleData schedule = createSchedule(null, LocalDate.of(2026, 12, 31));

        assertTrue(schedule.getRepeatDays().isEmpty());
        assertFalse(schedule.isRepeated());
    }

    private ScheduleData createSchedule(Set<DayOfWeek> repeatDays, LocalDate repeatEndDate) {
        return new ScheduleData(
                1,
                "Software System Design",
                "CLASS",
                LocalDate.of(2026, 11, 4),
                LocalTime.of(10, 0),
                LocalTime.of(11, 30),
                "Engineering Building",
                "Chapter 4",
                repeatDays,
                repeatEndDate
        );
    }
}
