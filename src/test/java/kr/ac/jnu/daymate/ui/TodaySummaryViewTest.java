package kr.ac.jnu.daymate.ui;

import kr.ac.jnu.daymate.controller.SummaryController;
import kr.ac.jnu.daymate.data.DayMateDataManager;
import kr.ac.jnu.daymate.data.LocalDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TodaySummaryViewTest {
    @TempDir
    Path tempDirectory;

    @Test
    void stacksSummaryTitleAndStatusVertically() throws Exception {
        DayMateDataManager dataManager = new DayMateDataManager(
                new LocalDatabase(tempDirectory.resolve("summary-view.db").toString())
        );
        dataManager.loadAllData();

        SwingUtilities.invokeAndWait(() -> {
            TodaySummaryView view = new TodaySummaryView(new SummaryController(dataManager));
            JLabel titleLabel = findLabel(view, "오늘의 일정 및 과제 마감 요약");
            assertNotNull(titleLabel);
            JPanel summaryPanel = assertInstanceOf(JPanel.class, titleLabel.getParent());

            BoxLayout layout = assertInstanceOf(BoxLayout.class, summaryPanel.getLayout());
            assertEquals(BoxLayout.Y_AXIS, layout.getAxis());
            assertEquals(2, summaryPanel.getComponentCount());
            assertEquals(titleLabel, summaryPanel.getComponent(0));
            assertInstanceOf(JLabel.class, summaryPanel.getComponent(1));
        });
    }

    private static JLabel findLabel(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && text.equals(label.getText())) {
                return label;
            }
            if (component instanceof Container child) {
                JLabel label = findLabel(child, text);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }
}
