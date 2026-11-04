package kr.ac.jnu.daymate.ui;

import kr.ac.jnu.daymate.dto.DailySummary;
import kr.ac.jnu.daymate.model.AssignmentData;
import kr.ac.jnu.daymate.model.ScheduleData;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 지정 시각의 오늘 일정과 3일 이내 미제출 과제를 보여주는 비모달 알림 대화상자이다.
 *
 * @serial exclude
 */
public final class DailySummaryNotificationDialog extends JDialog {
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 일일 요약 알림 대화상자를 생성한다.
     *
     * @param owner 대화상자를 소유하는 창. 소유자가 없으면 {@code null}
     * @param summary 알림에 표시할 요약
     * @param referenceTime 남은 시간과 기한 초과 여부를 계산할 기준 시각
     * @throws NullPointerException {@code summary} 또는 {@code referenceTime}이 {@code null}인 경우
     */
    public DailySummaryNotificationDialog(
            Window owner,
            DailySummary summary,
            LocalDateTime referenceTime
    ) {
        super(owner, "DayMate 일일 요약 알림", ModalityType.MODELESS);
        Objects.requireNonNull(summary, "일일 요약은 null일 수 없습니다.");
        Objects.requireNonNull(referenceTime, "기준 시각은 null일 수 없습니다.");

        buildUserInterface(summary, referenceTime);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
    }

    /**
     * 일일 요약 알림을 화면에 표시한다.
     */
    public void showDialog() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::showDialog);
            return;
        }
        setVisible(true);
        toFront();
    }

    private void buildUserInterface(DailySummary summary, LocalDateTime referenceTime) {
        JLabel titleLabel = new JLabel("오늘의 일정 및 마감 임박 과제");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));

        JTextArea summaryArea = new JTextArea(16, 48);
        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setText(createSummaryText(summary, referenceTime));
        summaryArea.setCaretPosition(0);

        JButton closeButton = new JButton("확인");
        closeButton.addActionListener(event -> dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);

        JPanel contentPanel = new JPanel(new BorderLayout(8, 8));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        contentPanel.add(titleLabel, BorderLayout.NORTH);
        contentPanel.add(new JScrollPane(summaryArea), BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(contentPanel);
        getRootPane().setDefaultButton(closeButton);
    }

    private String createSummaryText(DailySummary summary, LocalDateTime referenceTime) {
        StringBuilder text = new StringBuilder();
        if (!summary.getSchedules().isEmpty()) {
            text.append("[오늘의 일정]\n");
            for (ScheduleData schedule : summary.getSchedules()) {
                text.append("• ")
                        .append(schedule.getStartTime())
                        .append(" ")
                        .append(schedule.getTitle())
                        .append(" (")
                        .append(schedule.getType())
                        .append(")\n");
            }
        }

        if (!summary.getAssignments().isEmpty()) {
            if (!text.isEmpty()) {
                text.append("\n");
            }
            text.append("[미제출 과제]\n");
            for (AssignmentData assignment : summary.getAssignments()) {
                text.append("• ")
                        .append(assignment.getTitle())
                        .append(" - ")
                        .append(assignment.getDueDateTime().format(DATE_TIME_FORMAT))
                        .append(" (")
                        .append(formatRemainingTime(assignment, referenceTime))
                        .append(")\n");
            }
        }
        return text.toString();
    }

    private String formatRemainingTime(AssignmentData assignment, LocalDateTime referenceTime) {
        if (assignment.isOverdue(referenceTime)) {
            return "기한 초과";
        }
        Duration remaining = Duration.between(referenceTime, assignment.getDueDateTime());
        long days = remaining.toDays();
        if (days > 0) {
            return "D-" + days;
        }
        long hours = remaining.toHours();
        if (hours > 0) {
            return hours + "시간 남음";
        }
        long minutes = Math.max(remaining.toMinutes(), 0);
        return minutes == 0 ? "마감 임박" : minutes + "분 남음";
    }
}
