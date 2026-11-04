package kr.ac.jnu.daymate.controller;

import kr.ac.jnu.daymate.model.ScheduleData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 저장된 일정의 반복 설정을 조회 기간에 해당하는 발생 일정으로 변환한다.
 */
final class ScheduleOccurrenceResolver {
    private ScheduleOccurrenceResolver() {
    }

    static List<ScheduleData> resolve(List<ScheduleData> schedules, LocalDate startDate, LocalDate endDate) {
        List<ScheduleData> occurrences = new ArrayList<>();
        for (ScheduleData schedule : schedules) {
            addOccurrences(occurrences, schedule, startDate, endDate);
        }
        occurrences.sort(Comparator
                .comparing(ScheduleData::getDate)
                .thenComparing(ScheduleData::getStartTime)
                .thenComparing(ScheduleData::getTitle));
        return List.copyOf(occurrences);
    }

    private static void addOccurrences(
            List<ScheduleData> occurrences,
            ScheduleData schedule,
            LocalDate startDate,
            LocalDate endDate
    ) {
        LocalDate initialDate = schedule.getDate();
        if (!initialDate.isBefore(startDate) && !initialDate.isAfter(endDate)) {
            occurrences.add(copyForDate(schedule, initialDate));
        }
        if (!schedule.isRepeated()) {
            return;
        }

        LocalDate firstCandidate = startDate.isAfter(initialDate) ? startDate : initialDate;
        LocalDate repeatEnd = schedule.getRepeatEndDate().isBefore(endDate)
                ? schedule.getRepeatEndDate()
                : endDate;
        if (firstCandidate.isAfter(repeatEnd)) {
            return;
        }

        LocalDate candidate = firstCandidate;
        while (!candidate.isAfter(repeatEnd)) {
            if (!candidate.equals(initialDate) && schedule.getRepeatDays().contains(candidate.getDayOfWeek())) {
                occurrences.add(copyForDate(schedule, candidate));
            }
            if (candidate.equals(repeatEnd)) {
                break;
            }
            candidate = candidate.plusDays(1);
        }
    }

    private static ScheduleData copyForDate(ScheduleData source, LocalDate occurrenceDate) {
        return new ScheduleData(
                source.getId(),
                source.getTitle(),
                source.getType(),
                occurrenceDate,
                source.getStartTime(),
                source.getEndTime(),
                source.getLocation(),
                source.getMemo(),
                source.getRepeatDays(),
                source.getRepeatEndDate()
        );
    }
}
