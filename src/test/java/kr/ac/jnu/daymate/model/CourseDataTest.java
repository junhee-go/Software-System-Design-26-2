package kr.ac.jnu.daymate.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseDataTest {
    @Test
    void updatesEditableCourseInformation() {
        CourseData course = new CourseData(1, "Old Name", "Old Professor");

        course.updateInfo("Software System Design", "Kim");

        assertEquals(1, course.getId());
        assertEquals("Software System Design", course.getName());
        assertEquals("Kim", course.getProfessor());
    }
}
