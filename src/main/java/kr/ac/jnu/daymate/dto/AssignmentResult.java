package kr.ac.jnu.daymate.dto;

import kr.ac.jnu.daymate.model.AssignmentData;

/**
 * 과제 처리 결과를 전달한다.
 */
public final class AssignmentResult {
    private final boolean success;
    private final String message;
    private final AssignmentData assignment;

    /**
     * 과제 처리 결과를 생성한다.
     *
     * @param success 처리 성공 여부
     * @param message 사용자에게 표시할 결과 메시지
     * @param assignment 처리된 과제. 반환할 과제가 없다면 {@code null}
     */
    public AssignmentResult(boolean success, String message, AssignmentData assignment) {
        this.success = success;
        this.message = message;
        this.assignment = assignment;
    }

    /**
     * 처리가 성공했는지 확인한다.
     *
     * @return 성공한 경우 {@code true}
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 결과 메시지를 반환한다.
     *
     * @return 결과 메시지
     */
    public String getMessage() {
        return message;
    }

    /**
     * 처리된 과제를 반환한다.
     *
     * @return 처리된 과제. 과제가 없다면 {@code null}
     */
    public AssignmentData getAssignment() {
        return assignment;
    }
}
