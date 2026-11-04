package kr.ac.jnu.daymate.controller;

import kr.ac.jnu.daymate.data.DayMateDataManager;
import kr.ac.jnu.daymate.data.LocalDatabase;
import kr.ac.jnu.daymate.dto.AssignmentResult;
import kr.ac.jnu.daymate.model.AssignmentData;
import kr.ac.jnu.daymate.model.CourseData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseAssignmentControllerTest {
    @TempDir
    Path tempDirectory;

    private DayMateDataManager dataManager;
    private CourseAssignmentController controller;

    @BeforeEach
    void setUp() {
        LocalDatabase database = new LocalDatabase(tempDirectory.resolve("course-assignment.db").toString());
        dataManager = new DayMateDataManager(database);
        dataManager.loadAllData();
        controller = new CourseAssignmentController(dataManager);
    }

    @Test
    void savesValidCourseAndRejectsMissingInformation() {
        assertTrue(controller.saveCourse(new CourseData(0, "소프트웨어 시스템 설계", "김미수")));
        assertFalse(controller.saveCourse(new CourseData(0, " ", "김미수")));
        assertFalse(controller.saveCourse(new CourseData(0, "자료구조", null)));
        assertEquals(1, controller.getCourses().size());
    }

    @Test
    void updatesExistingCourseOnly() {
        CourseData course = saveCourse();

        assertTrue(controller.updateCourse(new CourseData(course.getId(), "시스템 설계", "이교수")));
        assertEquals("시스템 설계", controller.getCourses().get(0).getName());
        assertFalse(controller.updateCourse(new CourseData(999, "없는 과목", "이교수")));
    }

    @Test
    void deletesCourseWithoutAssignments() {
        CourseData course = saveCourse();

        assertTrue(controller.deleteCourse(course.getId()));
        assertFalse(controller.deleteCourse(course.getId()));
        assertTrue(controller.getCourses().isEmpty());
    }

    @Test
    void blocksCourseDeletionWhenAssignmentIsConnected() {
        CourseData course = saveCourse();
        assertTrue(controller.saveAssignment(createAssignment(0, course.getId())).isSuccess());

        assertFalse(controller.deleteCourse(course.getId()));
        assertEquals(1, controller.getCourses().size());
        assertEquals(1, controller.getAssignments().size());
    }

    @Test
    void savesAssignmentAndReturnsPersistedId() {
        CourseData course = saveCourse();

        AssignmentResult result = controller.saveAssignment(createAssignment(0, course.getId()));

        assertTrue(result.isSuccess());
        assertNotNull(result.getAssignment());
        assertTrue(result.getAssignment().getId() > 0);
        assertFalse(result.getAssignment().isSubmitted());
    }

    @Test
    void rejectsAssignmentWithMissingTitleOrUnknownCourse() {
        CourseData course = saveCourse();
        AssignmentData missingTitle = new AssignmentData(
                0,
                " ",
                course.getId(),
                LocalDateTime.of(2026, 11, 10, 23, 59),
                null,
                false
        );

        AssignmentResult titleResult = controller.saveAssignment(missingTitle);
        AssignmentResult courseResult = controller.saveAssignment(createAssignment(0, 999));

        assertFalse(titleResult.isSuccess());
        assertEquals("과제명을 입력해 주세요.", titleResult.getMessage());
        assertFalse(courseResult.isSuccess());
        assertEquals("등록된 관련 과목을 선택해 주세요.", courseResult.getMessage());
        assertTrue(controller.getAssignments().isEmpty());
    }

    @Test
    void permitsPastDeadlineForConfirmedRegistrationFlow() {
        CourseData course = saveCourse();
        AssignmentData overdue = new AssignmentData(
                0,
                "지난 과제",
                course.getId(),
                LocalDateTime.of(2020, 1, 1, 0, 0),
                null,
                false
        );

        assertTrue(controller.saveAssignment(overdue).isSuccess());
        assertTrue(controller.getAssignments().get(0).isOverdue(LocalDateTime.of(2026, 11, 4, 12, 0)));
    }

    @Test
    void updatesExistingAssignment() {
        CourseData course = saveCourse();
        AssignmentData saved = controller.saveAssignment(createAssignment(0, course.getId())).getAssignment();
        AssignmentData update = new AssignmentData(
                saved.getId(),
                "수정된 체크포인트",
                course.getId(),
                LocalDateTime.of(2026, 11, 12, 18, 0),
                "수정된 메모",
                false
        );

        AssignmentResult result = controller.updateAssignment(update);

        assertTrue(result.isSuccess());
        assertEquals("수정된 체크포인트", result.getAssignment().getTitle());
        assertFalse(controller.updateAssignment(createAssignment(999, course.getId())).isSuccess());
    }

    @Test
    void changesSubmissionStatus() {
        CourseData course = saveCourse();
        AssignmentData saved = controller.saveAssignment(createAssignment(0, course.getId())).getAssignment();

        assertTrue(controller.changeSubmissionStatus(saved.getId(), true));
        assertTrue(controller.getAssignments().get(0).isSubmitted());
        assertFalse(controller.changeSubmissionStatus(999, true));
    }

    @Test
    void deletesExistingAssignmentOnlyOnce() {
        CourseData course = saveCourse();
        AssignmentData saved = controller.saveAssignment(createAssignment(0, course.getId())).getAssignment();

        assertTrue(controller.deleteAssignment(saved.getId()));
        assertFalse(controller.deleteAssignment(saved.getId()));
        assertTrue(controller.getAssignments().isEmpty());
    }

    private CourseData saveCourse() {
        assertTrue(controller.saveCourse(new CourseData(0, "소프트웨어 시스템 설계", "김미수")));
        return controller.getCourses().get(0);
    }

    private AssignmentData createAssignment(int id, int courseId) {
        return new AssignmentData(
                id,
                "체크포인트 4",
                courseId,
                LocalDateTime.of(2026, 11, 10, 23, 59),
                "클래스 다이어그램 제출",
                false
        );
    }
}
