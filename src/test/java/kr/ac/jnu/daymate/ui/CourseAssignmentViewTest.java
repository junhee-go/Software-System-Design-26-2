package kr.ac.jnu.daymate.ui;

import kr.ac.jnu.daymate.controller.CourseAssignmentController;
import kr.ac.jnu.daymate.data.DayMateDataManager;
import kr.ac.jnu.daymate.data.LocalDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseAssignmentViewTest {
    @TempDir
    Path tempDirectory;

    @Test
    void omitsInternalIdsFromCourseAndAssignmentTables() throws Exception {
        DayMateDataManager dataManager = new DayMateDataManager(
                new LocalDatabase(tempDirectory.resolve("course-assignment-view.db").toString())
        );
        dataManager.loadAllData();

        SwingUtilities.invokeAndWait(() -> {
            CourseAssignmentView view = new CourseAssignmentView(
                    new CourseAssignmentController(dataManager)
            );
            List<JTable> tables = findTables(view);
            List<List<String>> headers = tables.stream()
                    .map(CourseAssignmentViewTest::columnNames)
                    .toList();

            assertEquals(2, tables.size());
            assertTrue(headers.contains(List.of("과목명", "담당 교수")));
            assertTrue(headers.contains(List.of("과제명", "관련 과목", "마감 일시", "제출 상태")));
            assertFalse(headers.stream()
                    .flatMap(List::stream)
                    .anyMatch("ID"::equals));
        });
    }

    private static List<JTable> findTables(Container container) {
        List<JTable> tables = new ArrayList<>();
        for (Component component : container.getComponents()) {
            if (component instanceof JTable table) {
                tables.add(table);
            }
            if (component instanceof Container child) {
                tables.addAll(findTables(child));
            }
        }
        return tables;
    }

    private static List<String> columnNames(JTable table) {
        List<String> names = new ArrayList<>();
        for (int index = 0; index < table.getColumnCount(); index++) {
            names.add(table.getColumnName(index));
        }
        return names;
    }
}
