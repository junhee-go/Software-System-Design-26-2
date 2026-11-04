package kr.ac.jnu.daymate.ui;

import kr.ac.jnu.daymate.controller.CalendarController;
import kr.ac.jnu.daymate.model.ScheduleData;
import kr.ac.jnu.daymate.model.ViewType;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * 등록된 일정을 월간 격자 또는 주간 목록으로 표시하는 Swing 캘린더 화면이다.
 *
 * @serial exclude
 */
public final class CalendarView extends JPanel {
    private static final String MONTH_CARD = "month";
    private static final String WEEK_CARD = "week";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final CalendarController controller;
    private final JLabel periodLabel = new JLabel("", SwingConstants.CENTER);
    private final JButton previousButton = new JButton("이전");
    private final JButton todayButton = new JButton("오늘");
    private final JButton nextButton = new JButton("다음");
    private final JButton monthButton = new JButton("월간");
    private final JButton weekButton = new JButton("주간");
    private final JButton detailButton = new JButton("상세 보기");
    private final DefaultTableModel monthTableModel = createTableModel(
            "월", "화", "수", "목", "금", "토", "일"
    );
    private final DefaultTableModel weekTableModel = createTableModel(
            "날짜", "시작", "종료", "유형", "제목"
    );
    private final DefaultTableModel detailTableModel = createTableModel(
            "날짜", "시작", "종료", "유형", "제목"
    );
    private final JTable monthTable = new JTable(monthTableModel);
    private final JTable weekTable = createColoredScheduleTable(weekTableModel);
    private final JTable detailTable = createColoredScheduleTable(detailTableModel);
    private final JPanel calendarCards = new JPanel(new CardLayout());
    private final LocalDate[][] monthCellDates = new LocalDate[6][7];
    private ViewType viewType = ViewType.MONTH;
    private LocalDate currentDate = LocalDate.now();
    private LocalDate selectedDate = currentDate;
    private List<ScheduleData> visibleSchedules = List.of();
    private List<ScheduleData> detailSchedules = List.of();

    /**
     * 지정한 컨트롤러를 사용하는 캘린더 화면을 생성하고 현재 기간의 일정을 불러온다.
     * 이 화면은 Swing 이벤트 디스패치 스레드에서 생성해야 한다.
     *
     * @param controller 캘린더 일정을 조회할 컨트롤러
     * @throws NullPointerException {@code controller}가 {@code null}인 경우
     */
    public CalendarView(CalendarController controller) {
        this.controller = Objects.requireNonNull(controller, "캘린더 컨트롤러는 null일 수 없습니다.");
        buildUserInterface();
        bindActions();
        refreshCalendar();
    }

    /**
     * 현재 선택한 월 또는 주의 일정을 다시 조회하여 화면을 갱신한다.
     */
    public void refreshCalendar() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshCalendar);
            return;
        }
        LocalDate startDate = getPeriodStart();
        LocalDate endDate = getPeriodEnd();
        ViewType requestedViewType = viewType;
        setBusy(true);

        new SwingWorker<List<ScheduleData>, Void>() {
            @Override
            protected List<ScheduleData> doInBackground() {
                return controller.getCalendarSchedules(startDate, endDate, requestedViewType);
            }

            @Override
            protected void done() {
                try {
                    if (requestedViewType == viewType) {
                        displayCalendar(get(), requestedViewType);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("캘린더 조회가 중단되었습니다.");
                } catch (ExecutionException exception) {
                    showError("캘린더 일정을 불러오지 못했습니다.");
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    /**
     * 조회한 일정을 지정한 캘린더 표시 방식으로 화면에 반영한다.
     *
     * @param schedules 표시할 발생 일정 목록
     * @param requestedViewType 월간 또는 주간 표시 방식
     */
    public void displayCalendar(List<ScheduleData> schedules, ViewType requestedViewType) {
        Objects.requireNonNull(schedules, "일정 목록은 null일 수 없습니다.");
        Objects.requireNonNull(requestedViewType, "캘린더 표시 방식은 null일 수 없습니다.");
        if (!SwingUtilities.isEventDispatchThread()) {
            List<ScheduleData> copiedSchedules = List.copyOf(schedules);
            SwingUtilities.invokeLater(() -> displayCalendar(copiedSchedules, requestedViewType));
            return;
        }

        visibleSchedules = List.copyOf(schedules);
        viewType = requestedViewType;
        if (viewType == ViewType.MONTH) {
            renderMonth();
        } else {
            renderWeek();
        }
        updatePeriodLabel();
        showActiveCard();
    }

    /**
     * 캘린더 표시 방식을 월간 또는 주간으로 변경하고 해당 기간을 조회한다.
     *
     * @param newViewType 변경할 캘린더 표시 방식
     */
    public void changeView(ViewType newViewType) {
        Objects.requireNonNull(newViewType, "캘린더 표시 방식은 null일 수 없습니다.");
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> changeView(newViewType));
            return;
        }
        if (viewType == newViewType) {
            return;
        }
        viewType = newViewType;
        selectedDate = currentDate;
        refreshCalendar();
    }

    /**
     * 선택한 일정의 날짜, 시각, 장소, 메모 및 반복 정보를 대화상자로 표시한다.
     *
     * @param schedule 상세 정보를 표시할 일정
     */
    public void displayScheduleDetail(ScheduleData schedule) {
        Objects.requireNonNull(schedule, "일정은 null일 수 없습니다.");
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> displayScheduleDetail(schedule));
            return;
        }
        String repeatText = schedule.isRepeated()
                ? schedule.getRepeatDays() + " / " + schedule.getRepeatEndDate()
                : "반복 없음";
        String detail = """
                <html><b>%s</b><br><br>
                유형: %s<br>
                날짜: %s<br>
                시각: %s ~ %s<br>
                장소: %s<br>
                메모: %s<br>
                반복: %s</html>
                """.formatted(
                escapeHtml(schedule.getTitle()),
                escapeHtml(schedule.getType()),
                schedule.getDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                escapeHtml(Objects.toString(schedule.getLocation(), "없음")),
                escapeHtml(Objects.toString(schedule.getMemo(), "없음")),
                escapeHtml(repeatText)
        );
        JOptionPane.showMessageDialog(this, detail, "일정 상세 정보", JOptionPane.INFORMATION_MESSAGE);
    }

    private void buildUserInterface() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        periodLabel.setFont(periodLabel.getFont().deriveFont(Font.BOLD, 16f));
        JPanel navigationPanel = new JPanel(new BorderLayout());
        JPanel movePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        movePanel.add(previousButton);
        movePanel.add(todayButton);
        movePanel.add(nextButton);
        JPanel viewPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        viewPanel.add(monthButton);
        viewPanel.add(weekButton);
        navigationPanel.add(movePanel, BorderLayout.WEST);
        navigationPanel.add(periodLabel, BorderLayout.CENTER);
        navigationPanel.add(viewPanel, BorderLayout.EAST);

        configureMonthTable();
        configureScheduleTable(weekTable);
        configureScheduleTable(detailTable);
        calendarCards.add(new JScrollPane(monthTable), MONTH_CARD);
        calendarCards.add(new JScrollPane(weekTable), WEEK_CARD);

        JPanel detailPanel = new JPanel(new BorderLayout(4, 4));
        detailPanel.setBorder(BorderFactory.createTitledBorder("선택 기간의 일정"));
        detailPanel.add(new JScrollPane(detailTable), BorderLayout.CENTER);
        JPanel detailButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        detailButtonPanel.add(detailButton);
        detailPanel.add(detailButtonPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, calendarCards, detailPanel);
        splitPane.setResizeWeight(0.68);
        splitPane.setBorder(null);

        add(navigationPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void configureMonthTable() {
        monthTable.setRowHeight(88);
        monthTable.setCellSelectionEnabled(true);
        monthTable.getTableHeader().setReorderingAllowed(false);
        monthTable.setDefaultRenderer(Object.class, new MonthCellRenderer());
    }

    private void configureScheduleTable(JTable table) {
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
    }

    private void bindActions() {
        previousButton.addActionListener(event -> movePeriod(-1));
        todayButton.addActionListener(event -> moveToToday());
        nextButton.addActionListener(event -> movePeriod(1));
        monthButton.addActionListener(event -> changeView(ViewType.MONTH));
        weekButton.addActionListener(event -> changeView(ViewType.WEEK));
        detailButton.addActionListener(event -> showSelectedDetail());
        detailTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    showSelectedDetail();
                }
            }
        });
        monthTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int row = monthTable.rowAtPoint(event.getPoint());
                int column = monthTable.columnAtPoint(event.getPoint());
                if (row >= 0 && column >= 0 && monthCellDates[row][column] != null) {
                    selectedDate = monthCellDates[row][column];
                    updateDetailTable(visibleSchedules.stream()
                            .filter(schedule -> schedule.getDate().equals(selectedDate))
                            .toList());
                }
            }
        });
        weekTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                int row = weekTable.getSelectedRow();
                if (row >= 0 && row < visibleSchedules.size()) {
                    detailTable.setRowSelectionInterval(row, row);
                }
            }
        });
    }

    private void movePeriod(int amount) {
        currentDate = viewType == ViewType.MONTH
                ? currentDate.plusMonths(amount)
                : currentDate.plusWeeks(amount);
        selectedDate = currentDate;
        refreshCalendar();
    }

    private void moveToToday() {
        currentDate = LocalDate.now();
        selectedDate = currentDate;
        refreshCalendar();
    }

    private void renderMonth() {
        monthTableModel.setRowCount(0);
        YearMonth yearMonth = YearMonth.from(currentDate);
        LocalDate gridStart = yearMonth.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<LocalDate, List<ScheduleData>> schedulesByDate = new HashMap<>();
        for (ScheduleData schedule : visibleSchedules) {
            schedulesByDate.computeIfAbsent(schedule.getDate(), ignored -> new java.util.ArrayList<>()).add(schedule);
        }

        for (int row = 0; row < 6; row++) {
            Object[] values = new Object[7];
            for (int column = 0; column < 7; column++) {
                LocalDate date = gridStart.plusDays(row * 7L + column);
                monthCellDates[row][column] = date;
                values[column] = createMonthCellText(date, schedulesByDate.getOrDefault(date, List.of()));
            }
            monthTableModel.addRow(values);
        }
        updateDetailTable(visibleSchedules.stream()
                .filter(schedule -> schedule.getDate().equals(selectedDate))
                .toList());
    }

    private void renderWeek() {
        weekTableModel.setRowCount(0);
        for (ScheduleData schedule : visibleSchedules) {
            weekTableModel.addRow(createScheduleRow(schedule));
        }
        updateDetailTable(visibleSchedules);
    }

    private void updateDetailTable(List<ScheduleData> schedules) {
        detailSchedules = List.copyOf(schedules);
        detailTableModel.setRowCount(0);
        for (ScheduleData schedule : detailSchedules) {
            detailTableModel.addRow(createScheduleRow(schedule));
        }
    }

    private Object[] createScheduleRow(ScheduleData schedule) {
        return new Object[]{
                schedule.getDate().format(DATE_FORMAT),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getType(),
                schedule.getTitle()
        };
    }

    private String createMonthCellText(LocalDate date, List<ScheduleData> schedules) {
        StringBuilder html = new StringBuilder("<html><b>").append(date.getDayOfMonth()).append("</b>");
        int displayed = 0;
        for (ScheduleData schedule : schedules) {
            if (displayed == 3) {
                html.append("<br>외 ").append(schedules.size() - displayed).append("개");
                break;
            }
            html.append("<br><font color='")
                    .append(typeColorHex(schedule.getType()))
                    .append("'>")
                    .append(schedule.getStartTime())
                    .append(" ")
                    .append(escapeHtml(schedule.getTitle()))
                    .append("</font>");
            displayed++;
        }
        return html.append("</html>").toString();
    }

    private void showSelectedDetail() {
        int row = detailTable.getSelectedRow();
        if (row < 0 || row >= detailSchedules.size()) {
            showError("상세 정보를 확인할 일정을 선택해 주세요.");
            return;
        }
        displayScheduleDetail(detailSchedules.get(row));
    }

    private void updatePeriodLabel() {
        if (viewType == ViewType.MONTH) {
            periodLabel.setText(currentDate.getYear() + "년 " + currentDate.getMonthValue() + "월");
            return;
        }
        periodLabel.setText(getPeriodStart() + " ~ " + getPeriodEnd());
    }

    private void showActiveCard() {
        CardLayout layout = (CardLayout) calendarCards.getLayout();
        layout.show(calendarCards, viewType == ViewType.MONTH ? MONTH_CARD : WEEK_CARD);
        monthButton.setEnabled(viewType != ViewType.MONTH);
        weekButton.setEnabled(viewType != ViewType.WEEK);
    }

    private LocalDate getPeriodStart() {
        if (viewType == ViewType.MONTH) {
            return YearMonth.from(currentDate).atDay(1);
        }
        return currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate getPeriodEnd() {
        if (viewType == ViewType.MONTH) {
            return YearMonth.from(currentDate).atEndOfMonth();
        }
        return getPeriodStart().plusDays(6);
    }

    private void setBusy(boolean busy) {
        previousButton.setEnabled(!busy);
        todayButton.setEnabled(!busy);
        nextButton.setEnabled(!busy);
        if (busy) {
            monthButton.setEnabled(false);
            weekButton.setEnabled(false);
        } else {
            showActiveCard();
        }
        detailButton.setEnabled(!busy);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "캘린더", JOptionPane.WARNING_MESSAGE);
    }

    private static JTable createColoredScheduleTable(DefaultTableModel model) {
        return new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    Object type = getModel().getValueAt(row, 3);
                    component.setBackground(typeBackground(Objects.toString(type, "")));
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

    private static Color typeBackground(String type) {
        return switch (normalizeType(type)) {
            case "CLASS", "수업" -> new Color(225, 238, 255);
            case "EXAM", "시험" -> new Color(255, 225, 225);
            case "MEETING", "회의" -> new Color(226, 246, 230);
            default -> new Color(241, 232, 255);
        };
    }

    private static String typeColorHex(String type) {
        return switch (normalizeType(type)) {
            case "CLASS", "수업" -> "#2764B7";
            case "EXAM", "시험" -> "#B3261E";
            case "MEETING", "회의" -> "#237A3B";
            default -> "#7040A0";
        };
    }

    private static String normalizeType(String type) {
        return Objects.toString(type, "").trim().toUpperCase(Locale.ROOT);
    }

    private static String escapeHtml(String value) {
        return Objects.toString(value, "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private final class MonthCellRenderer extends DefaultTableCellRenderer {
        private MonthCellRenderer() {
            setVerticalAlignment(TOP);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            Component component = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column
            );
            if (!isSelected) {
                LocalDate date = monthCellDates[row][column];
                boolean currentMonth = date != null && YearMonth.from(date).equals(YearMonth.from(currentDate));
                component.setBackground(currentMonth ? Color.WHITE : new Color(242, 242, 242));
                component.setForeground(currentMonth ? Color.BLACK : Color.GRAY);
            }
            return component;
        }
    }
}
