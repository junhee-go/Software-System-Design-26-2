package kr.ac.jnu.daymate.controller;

import kr.ac.jnu.daymate.data.DayMateDataManager;
import kr.ac.jnu.daymate.dto.AssignmentResult;
import kr.ac.jnu.daymate.model.AssignmentData;
import kr.ac.jnu.daymate.model.CourseData;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 수강 과목과 과제의 등록, 수정, 삭제 및 제출 상태 변경을 조정한다.
 */
public final class CourseAssignmentController {
    private final DayMateDataManager dataManager;

    /**
     * 지정한 데이터 관리자를 사용하는 과목·과제 컨트롤러를 생성한다.
     * 데이터 관리자는 컨트롤러를 생성하기 전에 모든 데이터를 불러온 상태여야 한다.
     *
     * @param dataManager 과목과 과제의 캐시 및 저장소를 관리할 객체
     * @throws NullPointerException {@code dataManager}가 {@code null}인 경우
     */
    public CourseAssignmentController(DayMateDataManager dataManager) {
        this.dataManager = Objects.requireNonNull(dataManager, "데이터 관리자는 null일 수 없습니다.");
    }

    /**
     * 현재 등록된 수강 과목 목록을 반환한다.
     *
     * @return 외부 변경으로부터 분리된 과목 목록
     */
    public List<CourseData> getCourses() {
        return dataManager.getCourses();
    }

    /**
     * 현재 등록된 과제 목록을 반환한다.
     *
     * @return 외부 변경으로부터 분리된 과제 목록
     */
    public List<AssignmentData> getAssignments() {
        return dataManager.getAssignments();
    }

    /**
     * 새로운 수강 과목을 검증하고 저장한다.
     *
     * @param course 등록할 과목
     * @return 과목이 저장되면 {@code true}
     */
    public synchronized boolean saveCourse(CourseData course) {
        if (!validateCourse(course) || course.getId() != 0) {
            return false;
        }
        return dataManager.saveCourse(course);
    }

    /**
     * 기존 수강 과목의 이름과 담당 교수 정보를 수정한다.
     *
     * @param course 수정할 과목
     * @return 과목이 수정되면 {@code true}
     */
    public synchronized boolean updateCourse(CourseData course) {
        if (!validateCourse(course) || course.getId() <= 0 || !courseExists(course.getId())) {
            return false;
        }
        return dataManager.saveCourse(course);
    }

    /**
     * 연결된 과제가 없는 수강 과목을 삭제한다.
     *
     * @param courseId 삭제할 과목 식별자
     * @return 과목이 삭제되면 {@code true}
     */
    public synchronized boolean deleteCourse(int courseId) {
        if (courseId <= 0 || !courseExists(courseId) || hasAssignments(courseId)) {
            return false;
        }
        return dataManager.deleteCourse(courseId);
    }

    /**
     * 과목명과 담당 교수명이 모두 입력되었는지 확인한다.
     *
     * @param course 검사할 과목
     * @return 과목 정보가 유효하면 {@code true}
     */
    public boolean validateCourse(CourseData course) {
        return course != null
                && !isBlank(course.getName())
                && !isBlank(course.getProfessor());
    }

    /**
     * 새로운 과제를 검증하고 미제출 또는 지정된 제출 상태로 저장한다.
     * 과거 마감 일시도 허용하며 화면에서 사용자 확인 후 호출할 수 있다.
     *
     * @param assignment 등록할 과제
     * @return 등록 성공 여부, 안내 메시지, 저장된 과제를 포함한 결과
     */
    public synchronized AssignmentResult saveAssignment(AssignmentData assignment) {
        String validationError = findAssignmentValidationError(assignment);
        if (validationError != null) {
            return failure(validationError);
        }
        if (assignment.getId() != 0) {
            return failure("새 과제의 식별자는 0이어야 합니다.");
        }

        Set<Integer> existingIds = new HashSet<>();
        for (AssignmentData existing : dataManager.getAssignments()) {
            existingIds.add(existing.getId());
        }

        if (!dataManager.saveAssignment(assignment)) {
            return failure("과제를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }

        AssignmentData savedAssignment = dataManager.getAssignments().stream()
                .filter(saved -> !existingIds.contains(saved.getId()))
                .findFirst()
                .orElse(null);
        return new AssignmentResult(true, "과제가 등록되었습니다.", savedAssignment);
    }

    /**
     * 기존 과제 정보를 검증하고 변경된 내용을 저장한다.
     *
     * @param assignment 수정할 과제
     * @return 수정 성공 여부, 안내 메시지, 저장된 과제를 포함한 결과
     */
    public synchronized AssignmentResult updateAssignment(AssignmentData assignment) {
        String validationError = findAssignmentValidationError(assignment);
        if (validationError != null) {
            return failure(validationError);
        }
        if (assignment.getId() <= 0) {
            return failure("수정할 과제의 식별자가 올바르지 않습니다.");
        }
        if (!assignmentExists(assignment.getId())) {
            return failure("수정할 과제를 찾을 수 없습니다.");
        }
        if (!dataManager.saveAssignment(assignment)) {
            return failure("과제를 수정하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }

        AssignmentData updatedAssignment = findAssignment(assignment.getId());
        return new AssignmentResult(true, "과제가 수정되었습니다.", updatedAssignment);
    }

    /**
     * 지정한 식별자의 과제를 삭제한다.
     *
     * @param assignmentId 삭제할 과제 식별자
     * @return 과제가 삭제되면 {@code true}
     */
    public synchronized boolean deleteAssignment(int assignmentId) {
        return assignmentId > 0 && dataManager.deleteAssignment(assignmentId);
    }

    /**
     * 과제의 제출 상태를 미제출 또는 제출 완료로 변경한다.
     *
     * @param assignmentId 상태를 변경할 과제 식별자
     * @param submitted 새로운 제출 완료 여부
     * @return 상태가 저장되면 {@code true}
     */
    public synchronized boolean changeSubmissionStatus(int assignmentId, boolean submitted) {
        AssignmentData assignment = findAssignment(assignmentId);
        if (assignment == null) {
            return false;
        }
        AssignmentData updated = new AssignmentData(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getCourseId(),
                assignment.getDueDateTime(),
                assignment.getMemo(),
                submitted
        );
        return dataManager.saveAssignment(updated);
    }

    /**
     * 과제 정보가 등록 또는 수정 조건을 만족하는지 확인한다.
     *
     * @param assignment 검사할 과제
     * @return 과제 정보가 유효하면 {@code true}
     */
    public boolean validateAssignment(AssignmentData assignment) {
        return findAssignmentValidationError(assignment) == null;
    }

    private String findAssignmentValidationError(AssignmentData assignment) {
        if (assignment == null) {
            return "과제 정보를 입력해 주세요.";
        }
        if (isBlank(assignment.getTitle())) {
            return "과제명을 입력해 주세요.";
        }
        if (assignment.getDueDateTime() == null) {
            return "제출 마감 일시를 입력해 주세요.";
        }
        if (assignment.getCourseId() <= 0 || !courseExists(assignment.getCourseId())) {
            return "등록된 관련 과목을 선택해 주세요.";
        }
        return null;
    }

    private boolean courseExists(int courseId) {
        return dataManager.getCourses().stream().anyMatch(course -> course.getId() == courseId);
    }

    private boolean assignmentExists(int assignmentId) {
        return findAssignment(assignmentId) != null;
    }

    private AssignmentData findAssignment(int assignmentId) {
        return dataManager.getAssignments().stream()
                .filter(assignment -> assignment.getId() == assignmentId)
                .findFirst()
                .orElse(null);
    }

    private boolean hasAssignments(int courseId) {
        return dataManager.getAssignments().stream()
                .anyMatch(assignment -> assignment.getCourseId() == courseId);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static AssignmentResult failure(String message) {
        return new AssignmentResult(false, message, null);
    }
}
