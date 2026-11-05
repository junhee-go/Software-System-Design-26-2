package kr.ac.jnu.daymate.ui;

import kr.ac.jnu.daymate.controller.SummaryController;
import kr.ac.jnu.daymate.dto.DailySummary;
import kr.ac.jnu.daymate.model.AssignmentData;
import kr.ac.jnu.daymate.model.ScheduleData;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * 오늘의 일정과 미제출 과제를 마감 상태와 함께 표시하는 Swing 요약 화면이다.
 *
 * @serial exclude
 */
public final class TodaySummaryView extends JPanel {
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SummaryController controller;
    private final JLabel titleLabel = new JLabel("오늘의 일정 및 과제 마감 요약");
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton refreshButton = new JButton("새로고침");
    private final DefaultTableModel scheduleTableModel = createTableModel(
            "시작", "종료", "유형", "제목", "장소"
    );
    private final DefaultTableModel assignmentTableModel = createTableModel(
            "과제명", "마감 일시", "남은 시간", "제출 상태"
    );
    private final JTable scheduleTable = new JTable(scheduleTableModel);
    private final JTable assignmentTable = createAssignmentTable(assignmentTableModel);

    /**
     * 지정한 컨트롤러를 사용하는 오늘의 요약 화면을 생성하고 현재 정보를 불러온다.
     * 이 화면은 Swing 이벤트 디스패치 스레드에서 생성해야 한다.
     *
     * @param controller 오늘의 요약을 생성할 컨트롤러
     * @throws NullPointerException {@code controller}가 {@code null}인 경우
     */
    public TodaySummaryView(SummaryController controller) {
        this.controller = Objects.requireNonNull(controller, "요약 컨트롤러는 null일 수 없습니다.");
        buildUserInterface();
        refreshButton.addActionListener(event -> refreshSummary());
        refreshSummary();
    }

    /**
     * 현재 시각을 기준으로 오늘의 일정과 미제출 과제를 다시 조회한다.
     */
    public void refreshSummary() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshSummary);
            return;
        }
        LocalDateTime referenceTime = LocalDateTime.now();
        setBusy(true);
        new SwingWorker<SummarySnapshot, Void>() {
            @Override
            protected SummarySnapshot doInBackground() {
                return new SummarySnapshot(controller.createDailySummary(referenceTime), referenceTime);
            }

            @Override
            protected void done() {
                try {
                    SummarySnapshot snapshot = get();
                    displaySummary(snapshot.summary(), snapshot.referenceTime());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("요약 조회가 중단되었습니다.");
                } catch (ExecutionException exception) {
                    statusLabel.setText("오늘의 요약을 불러오지 못했습니다.");
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    /**
     * 오늘의 일정과 미제출 과제를 현재 시각 기준으로 화면에 표시한다.
     *
     * @param summary 표시할 일일 요약
     */
    public void displaySummary(DailySummary summary) {
        displaySummary(summary, LocalDateTime.now());
    }

    private void displaySummary(DailySummary summary, LocalDateTime referenceTime) {
        Objects.requireNonNull(summary, "일일 요약은 null일 수 없습니다.");
        Objects.requireNonNull(referenceTime, "기준 시각은 null일 수 없습니다.");
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> displaySummary(summary, referenceTime));
            return;
        }

        displaySchedules(summary.getSchedules());
        displayAssignments(summary.getAssignments(), referenceTime);
        if (summary.hasItems()) {
            statusLabel.setText("일정 " + summary.getSchedules().size()
                    + "개, 미제출 과제 " + summary.getAssignments().size() + "개");
        } else {
            statusLabel.setText("오늘의 일정과 미제출 과제가 없습니다.");
        }
    }

    private void buildUserInterface() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel headerPanel = new JPanel(new BorderLayout());
        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        summaryPanel.add(titleLabel);
        summaryPanel.add(statusLabel);
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.add(refreshButton);
        headerPanel.add(summaryPanel, BorderLayout.CENTER);
        headerPanel.add(actionPanel, BorderLayout.EAST);

        scheduleTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        scheduleTable.getTableHeader().setReorderingAllowed(false);
        assignmentTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        assignmentTable.getTableHeader().setReorderingAllowed(false);

        JPanel schedulePanel = new JPanel(new BorderLayout());
        schedulePanel.setBorder(BorderFactory.createTitledBorder("오늘의 일정"));
        schedulePanel.add(new JScrollPane(scheduleTable), BorderLayout.CENTER);

        JPanel assignmentPanel = new JPanel(new BorderLayout());
        assignmentPanel.setBorder(BorderFactory.createTitledBorder("미제출 과제"));
        assignmentPanel.add(new JScrollPane(assignmentTable), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, schedulePanel, assignmentPanel);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(null);

        add(headerPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void displaySchedules(List<ScheduleData> schedules) {
        scheduleTableModel.setRowCount(0);
        for (ScheduleData schedule : schedules) {
            scheduleTableModel.addRow(new Object[]{
                    schedule.getStartTime(),
                    schedule.getEndTime(),
                    schedule.getType(),
                    schedule.getTitle(),
                    Objects.toString(schedule.getLocation(), "")
            });
        }
    }

    private void displayAssignments(List<AssignmentData> assignments, LocalDateTime referenceTime) {
        assignmentTableModel.setRowCount(0);
        for (AssignmentData assignment : assignments) {
            assignmentTableModel.addRow(new Object[]{
                    assignment.getTitle(),
                    assignment.getDueDateTime().format(DATE_TIME_FORMAT),
                    formatRemainingTime(assignment, referenceTime),
                    assignment.isSubmitted() ? "제출 완료" : "미제출"
            });
        }
    }

    private void setBusy(boolean busy) {
        refreshButton.setEnabled(!busy);
        if (busy) {
            statusLabel.setText("요약을 불러오는 중입니다.");
        }
    }

    private static JTable createAssignmentTable(DefaultTableModel model) {
        return new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    boolean overdue = "기한 초과".equals(getModel().getValueAt(row, 2));
                    component.setBackground(overdue ? new Color(255, 225, 225) : Color.WHITE);
                    component.setForeground(Color.BLACK);
                }
                return component;
            }
        };
    }

    private static DefaultTableModel createTableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static String formatRemainingTime(AssignmentData assignment, LocalDateTime referenceTime) {
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

    private record SummarySnapshot(DailySummary summary, LocalDateTime referenceTime) {
    }
}
