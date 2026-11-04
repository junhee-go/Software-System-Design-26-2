package kr.ac.jnu.daymate.ui;

import kr.ac.jnu.daymate.data.DayMateDataManager;
import kr.ac.jnu.daymate.data.LocalDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class DayMateFrameTest {
    @TempDir
    Path tempDirectory;

    @Test
    void createsFrameAndSwitchesMainViews() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        DayMateDataManager dataManager = new DayMateDataManager(
                new LocalDatabase(tempDirectory.resolve("integration.db").toString())
        );
        dataManager.loadAllData();

        SwingUtilities.invokeAndWait(() -> {
            DayMateFrame frame = new DayMateFrame(dataManager);
            try {
                assertEquals("DayMate - 대학생 개인 일정 관리", frame.getTitle());
                frame.showView("summary");
                frame.showView("calendar");
                frame.showView("courseAssignment");
                assertThrows(IllegalArgumentException.class, () -> frame.showView("unknown"));
            } finally {
                frame.dispose();
            }
        });
    }
}
