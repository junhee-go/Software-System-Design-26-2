package kr.ac.jnu.daymate.ui;

import kr.ac.jnu.daymate.controller.AlertController;
import kr.ac.jnu.daymate.controller.CalendarController;
import kr.ac.jnu.daymate.controller.CourseAssignmentController;
import kr.ac.jnu.daymate.controller.ScheduleController;
import kr.ac.jnu.daymate.controller.SummaryController;
import kr.ac.jnu.daymate.data.DayMateDataManager;
import kr.ac.jnu.daymate.dto.DailySummary;
import kr.ac.jnu.daymate.model.NotificationSetting;
import kr.ac.jnu.daymate.model.ScheduleData;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DayMate의 모든 주요 화면과 알림 기능을 하나의 창에서 전환하도록 구성한 메인 프레임이다.
 * 이 프레임은 데이터가 로드된 후 Swing 이벤트 디스패치 스레드에서 생성해야 한다.
 *
 * @serial exclude
 */
public final class DayMateFrame extends JFrame {
    private static final String SUMMARY_VIEW = "summary";
    private static final String CALENDAR_VIEW = "calendar";
    private static final String COURSE_ASSIGNMENT_VIEW = "courseAssignment";

    private final DayMateDataManager dataManager;
    private final ScheduleController scheduleController;
    private final SummaryController summaryController;
    private final AlertController alertController;
    private final CalendarView calendarView;
    private final CourseAssignmentView courseAssignmentView;
    private final TodaySummaryView todaySummaryView;
    private final JPanel contentPanel = new JPanel(new CardLayout());
    private final JLabel notificationStatusLabel = new JLabel(" ");

    /**
     * 로드된 데이터를 공유하는 DayMate 메인 창을 생성한다.
     *
     * @param dataManager 초기화 및 데이터 로드가 완료된 데이터 관리자
     * @throws NullPointerException {@code dataManager}가 {@code null}인 경우
     */
    public DayMateFrame(DayMateDataManager dataManager) {
        super("DayMate - 대학생 개인 일정 관리");
        this.dataManager = Objects.requireNonNull(dataManager, "데이터 관리자는 null일 수 없습니다.");

        scheduleController = new ScheduleController(dataManager);
        CalendarController calendarController = new CalendarController(dataManager);
        CourseAssignmentController courseAssignmentController = new CourseAssignmentController(dataManager);
        summaryController = new SummaryController(dataManager);
        alertController = new AlertController(dataManager, summaryController, this::showDailyNotification);

        calendarView = new CalendarView(calendarController, this::openScheduleDialog);
        courseAssignmentView = new CourseAssignmentView(courseAssignmentController);
        todaySummaryView = new TodaySummaryView(summaryController);

        buildUserInterface();
        bindWindowLifecycle();
        updateNotificationStatus();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 700));
        setSize(1200, 780);
        setLocationRelativeTo(null);
        showView(SUMMARY_VIEW);
    }

    /**
     * 지정한 이름의 화면을 표시하고 해당 화면의 데이터를 새로고침한다.
     * 사용할 수 있는 이름은 {@code summary}, {@code calendar}, {@code courseAssignment}이다.
     *
     * @param viewName 표시할 화면 이름
     * @throws NullPointerException {@code viewName}이 {@code null}인 경우
     * @throws IllegalArgumentException 지원하지 않는 화면 이름인 경우
     */
    public void showView(String viewName) {
        Objects.requireNonNull(viewName, "화면 이름은 null일 수 없습니다.");
        validateViewName(viewName);
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showView(viewName));
            return;
        }
        switch (viewName) {
            case SUMMARY_VIEW -> todaySummaryView.refreshSummary();
            case CALENDAR_VIEW -> calendarView.refreshCalendar();
            case COURSE_ASSIGNMENT_VIEW -> courseAssignmentView.refreshData();
        }
        CardLayout layout = (CardLayout) contentPanel.getLayout();
        layout.show(contentPanel, viewName);
    }

    private void validateViewName(String viewName) {
        if (!SUMMARY_VIEW.equals(viewName)
                && !CALENDAR_VIEW.equals(viewName)
                && !COURSE_ASSIGNMENT_VIEW.equals(viewName)) {
            throw new IllegalArgumentException("지원하지 않는 화면입니다: " + viewName);
        }
    }

    /**
     * 일정, 캘린더, 오늘의 요약 및 알림 상태를 현재 캐시 기준으로 갱신한다.
     */
    public void refreshAll() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshAll);
            return;
        }
        calendarView.refreshCalendar();
        todaySummaryView.refreshSummary();
        updateNotificationStatus();
    }

    private void buildUserInterface() {
        setLayout(new BorderLayout());
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createNavigationPanel(), BorderLayout.WEST);

        contentPanel.add(todaySummaryView, SUMMARY_VIEW);
        contentPanel.add(calendarView, CALENDAR_VIEW);
        contentPanel.add(courseAssignmentView, COURSE_ASSIGNMENT_VIEW);
        add(contentPanel, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusPanel.add(notificationStatusLabel, BorderLayout.EAST);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        headerPanel.setBackground(new Color(42, 67, 101));

        JLabel titleLabel = new JLabel("DayMate");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        titleLabel.setForeground(Color.WHITE);

        JButton addScheduleButton = new JButton("일정 추가");
        addScheduleButton.addActionListener(event -> openScheduleDialog(null));
        JButton alertSettingButton = new JButton("알림 설정");
        alertSettingButton.addActionListener(event -> openAlertSettingDialog());
        JPanel actionPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        actionPanel.setOpaque(false);
        actionPanel.add(addScheduleButton);
        actionPanel.add(alertSettingButton);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(actionPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private JPanel createNavigationPanel() {
        JPanel navigationPanel = new JPanel();
        navigationPanel.setLayout(new BoxLayout(navigationPanel, BoxLayout.Y_AXIS));
        navigationPanel.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        navigationPanel.setPreferredSize(new Dimension(180, 0));

        JLabel menuLabel = new JLabel("메뉴", SwingConstants.LEFT);
        menuLabel.setFont(menuLabel.getFont().deriveFont(Font.BOLD));
        menuLabel.setAlignmentX(LEFT_ALIGNMENT);
        navigationPanel.add(menuLabel);
        navigationPanel.add(Box.createVerticalStrut(10));
        navigationPanel.add(createNavigationButton("오늘의 요약", SUMMARY_VIEW));
        navigationPanel.add(Box.createVerticalStrut(6));
        navigationPanel.add(createNavigationButton("캘린더", CALENDAR_VIEW));
        navigationPanel.add(Box.createVerticalStrut(6));
        navigationPanel.add(createNavigationButton("과목·과제 관리", COURSE_ASSIGNMENT_VIEW));
        navigationPanel.add(Box.createVerticalGlue());
        return navigationPanel;
    }

    private JButton createNavigationButton(String text, String viewName) {
        JButton button = new JButton(text);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        button.addActionListener(event -> showView(viewName));
        return button;
    }

    private void bindWindowLifecycle() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                alertController.startTimer();
            }

            @Override
            public void windowClosing(WindowEvent event) {
                alertController.stopTimer();
            }

            @Override
            public void windowClosed(WindowEvent event) {
                alertController.stopTimer();
            }
        });
    }

    private void openScheduleDialog(ScheduleData occurrence) {
        ScheduleData editableSchedule = occurrence == null ? null : findStoredSchedule(occurrence.getId());
        if (occurrence != null && editableSchedule == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "선택한 일정을 찾을 수 없습니다. 캘린더를 새로고침해 주세요.",
                    "일정 관리",
                    JOptionPane.WARNING_MESSAGE
            );
            calendarView.refreshCalendar();
            return;
        }
        ScheduleInputDialog dialog = new ScheduleInputDialog(
                this,
                scheduleController,
                editableSchedule,
                this::refreshAll
        );
        dialog.showDialog();
    }

    private ScheduleData findStoredSchedule(int scheduleId) {
        return dataManager.getSchedules().stream()
                .filter(schedule -> schedule.getId() == scheduleId)
                .findFirst()
                .orElse(null);
    }

    private void openAlertSettingDialog() {
        AlertSettingDialog dialog = new AlertSettingDialog(
                this,
                alertController,
                this::updateNotificationStatus
        );
        dialog.showDialog();
    }

    private void showDailyNotification(DailySummary summary) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showDailyNotification(summary));
            return;
        }
        DailySummaryNotificationDialog dialog = new DailySummaryNotificationDialog(
                this,
                summary,
                LocalDateTime.now()
        );
        dialog.showDialog();
    }

    private void updateNotificationStatus() {
        NotificationSetting setting = alertController.loadNotificationSettings();
        if (!setting.isEnabled()) {
            notificationStatusLabel.setText("일일 요약 알림: 꺼짐");
            return;
        }
        notificationStatusLabel.setText("일일 요약 알림: " + setting.getAlertTime());
    }
}
