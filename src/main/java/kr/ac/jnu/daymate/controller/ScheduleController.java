package kr.ac.jnu.daymate.controller;

import kr.ac.jnu.daymate.data.DayMateDataManager;
import kr.ac.jnu.daymate.dto.ScheduleResult;
import kr.ac.jnu.daymate.model.ScheduleData;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 일정 등록, 수정, 삭제 과정의 입력 검증과 데이터 저장을 조정한다.
 */
public final class ScheduleController {
    private final DayMateDataManager dataManager;

    /**
     * 지정한 데이터 관리자를 사용하는 일정 컨트롤러를 생성한다.
     * 데이터 관리자는 컨트롤러를 생성하기 전에 모든 데이터를 불러온 상태여야 한다.
     *
     * @param dataManager 일정 캐시와 저장소를 관리할 객체
     * @throws NullPointerException {@code dataManager}가 {@code null}인 경우
     */
    public ScheduleController(DayMateDataManager dataManager) {
        this.dataManager = Objects.requireNonNull(dataManager, "데이터 관리자는 null일 수 없습니다.");
    }

    /**
     * 새로운 일정을 검증하고 저장한다.
     * 반복 일정은 여러 일정으로 복제하지 않고 반복 요일과 종료일을 일정 정보에 함께 저장한다.
     *
     * @param schedule 등록할 일정
     * @return 등록 성공 여부, 안내 메시지, 저장된 일정을 포함한 결과
     */
    public synchronized ScheduleResult saveSchedule(ScheduleData schedule) {
        String validationError = findValidationError(schedule);
        if (validationError != null) {
            return failure(validationError);
        }
        if (schedule.getId() != 0) {
            return failure("새 일정의 식별자는 0이어야 합니다.");
        }

        Set<Integer> existingIds = new HashSet<>();
        for (ScheduleData existing : dataManager.getSchedules()) {
            existingIds.add(existing.getId());
        }

        if (!dataManager.saveSchedule(schedule)) {
            return failure("일정을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }

        ScheduleData savedSchedule = dataManager.getSchedules().stream()
                .filter(saved -> !existingIds.contains(saved.getId()))
                .findFirst()
                .orElse(null);
        return new ScheduleResult(true, "일정이 등록되었습니다.", savedSchedule);
    }

    /**
     * 기존 일정을 검증하고 변경된 내용을 저장한다.
     *
     * @param schedule 수정할 일정
     * @return 수정 성공 여부, 안내 메시지, 저장된 일정을 포함한 결과
     */
    public synchronized ScheduleResult updateSchedule(ScheduleData schedule) {
        String validationError = findValidationError(schedule);
        if (validationError != null) {
            return failure(validationError);
        }
        if (schedule.getId() <= 0) {
            return failure("수정할 일정의 식별자가 올바르지 않습니다.");
        }

        boolean exists = dataManager.getSchedules().stream()
                .anyMatch(existing -> existing.getId() == schedule.getId());
        if (!exists) {
            return failure("수정할 일정을 찾을 수 없습니다.");
        }
        if (!dataManager.saveSchedule(schedule)) {
            return failure("일정을 수정하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }

        ScheduleData updatedSchedule = dataManager.getSchedules().stream()
                .filter(saved -> saved.getId() == schedule.getId())
                .findFirst()
                .orElse(null);
        return new ScheduleResult(true, "일정이 수정되었습니다.", updatedSchedule);
    }

    /**
     * 지정한 식별자의 일정을 삭제한다.
     *
     * @param scheduleId 삭제할 일정 식별자
     * @return 실제로 일정이 삭제되면 {@code true}
     */
    public synchronized boolean deleteSchedule(int scheduleId) {
        return scheduleId > 0 && dataManager.deleteSchedule(scheduleId);
    }

    /**
     * 일정 정보가 등록 또는 수정 조건을 만족하는지 확인한다.
     *
     * @param schedule 검사할 일정
     * @return 일정 정보가 유효하면 {@code true}
     */
    public boolean validateSchedule(ScheduleData schedule) {
        return findValidationError(schedule) == null;
    }

    private String findValidationError(ScheduleData schedule) {
        if (schedule == null) {
            return "일정 정보를 입력해 주세요.";
        }
        if (isBlank(schedule.getTitle())) {
            return "일정 제목을 입력해 주세요.";
        }
        if (isBlank(schedule.getType())) {
            return "일정 유형을 선택해 주세요.";
        }
        if (schedule.getDate() == null) {
            return "일정 날짜를 입력해 주세요.";
        }
        if (schedule.getStartTime() == null || schedule.getEndTime() == null) {
            return "시작 시각과 종료 시각을 모두 입력해 주세요.";
        }
        if (schedule.getEndTime().isBefore(schedule.getStartTime())) {
            return "종료 시각은 시작 시각보다 빠를 수 없습니다.";
        }

        boolean hasRepeatDays = !schedule.getRepeatDays().isEmpty();
        boolean hasRepeatEndDate = schedule.getRepeatEndDate() != null;
        if (hasRepeatDays != hasRepeatEndDate) {
            return "반복 일정은 반복 요일과 종료일을 모두 설정해야 합니다.";
        }
        if (hasRepeatEndDate && schedule.getRepeatEndDate().isBefore(schedule.getDate())) {
            return "반복 종료일은 일정 시작일보다 빠를 수 없습니다.";
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static ScheduleResult failure(String message) {
        return new ScheduleResult(false, message, null);
    }
}
