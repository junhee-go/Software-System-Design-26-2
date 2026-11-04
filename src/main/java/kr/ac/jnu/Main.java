package kr.ac.jnu;

import kr.ac.jnu.daymate.data.DayMateDataManager;
import kr.ac.jnu.daymate.ui.DayMateFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * DayMate 애플리케이션의 실행 진입점이다.
 */
public final class Main {
    private Main() {
    }

    /**
     * DayMate 애플리케이션을 시작한다.
     *
     * @param args 명령행 인수. 현재는 사용하지 않는다.
     */
    public static void main(String[] args) {
        DayMateDataManager dataManager = new DayMateDataManager();
        try {
            dataManager.loadAllData();
        } catch (IllegalStateException exception) {
            showStartupError(exception);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            applySystemLookAndFeel();
            DayMateFrame frame = new DayMateFrame(dataManager);
            frame.setVisible(true);
        });
    }

    private static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // 시스템 테마를 적용할 수 없으면 Java 기본 테마를 사용한다.
        }
    }

    private static void showStartupError(IllegalStateException exception) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                null,
                "DayMate 데이터를 불러오지 못했습니다.\n" + exception.getMessage(),
                "DayMate 시작 오류",
                JOptionPane.ERROR_MESSAGE
        ));
    }
}
