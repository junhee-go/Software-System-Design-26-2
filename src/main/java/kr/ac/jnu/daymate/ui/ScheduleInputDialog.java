package kr.ac.jnu.daymate.ui;

import kr.ac.jnu.daymate.controller.ScheduleController;
import kr.ac.jnu.daymate.dto.ScheduleResult;
import kr.ac.jnu.daymate.model.ScheduleData;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * 사용자가 일정 정보를 입력하고 등록, 수정 또는 삭제할 수 있는 Swing 대화상자이다.
 * 이 대화상자는 Swing 이벤트 디스패치 스레드에서 생성하고 표시해야 한다.
 *
 * @serial exclude
 */
public final class ScheduleInputDialog extends JDialog {
    private static final String DATE_GUIDE = "yyyy-MM-dd";
    private static final String TIME_GUIDE = "HH:mm";

    private final ScheduleController scheduleController;
    private final ScheduleData editingSchedule;
    private final JTextField titleField = new JTextField(20);
    private final JComboBox<String> typeComboBox = new JComboBox<>(
            new String[]{"수업", "시험", "회의", "개인 약속"}
    );
    private final JTextField dateField = new JTextField(10);
    private final JTextField startTimeField = new JTextField(6);
    private final JTextField endTimeField = new JTextField(6);
    private final JTextField locationField = new JTextField(20);
    private final JTextArea memoArea = new JTextArea(4, 20);
    private final JCheckBox repeatCheckBox = new JCheckBox("반복 일정");
    private final Map<DayOfWeek, JCheckBox> repeatDayCheckBoxes = new EnumMap<>(DayOfWeek.class);
    private final JTextField repeatEndDateField = new JTextField(10);
    private final JButton saveButton = new JButton("저장");
    private final JButton cancelButton = new JButton("취소");
    private final JButton deleteButton = new JButton("삭제");

    /**
     * 새로운 일정을 등록하는 입력 대화상자를 생성한다.
     *
     * @param owner 대화상자를 소유하는 창. 소유자가 없으면 {@code null}
     * @param scheduleController 일정 처리를 담당할 컨트롤러
     */
    public ScheduleInputDialog(Window owner, ScheduleController scheduleController) {
        this(owner, scheduleController, null);
    }

    /**
     * 기존 일정을 수정하는 입력 대화상자를 생성한다.
     *
     * @param owner 대화상자를 소유하는 창. 소유자가 없으면 {@code null}
     * @param scheduleController 일정 처리를 담당할 컨트롤러
     * @param editingSchedule 수정할 일정. {@code null}이면 새 일정을 등록한다.
     */
    public ScheduleInputDialog(
            Window owner,
            ScheduleController scheduleController,
            ScheduleData editingSchedule
    ) {
        super(owner, editingSchedule == null ? "일정 등록" : "일정 수정", ModalityType.APPLICATION_MODAL);
        this.scheduleController = Objects.requireNonNull(
                scheduleController,
                "일정 컨트롤러는 null일 수 없습니다."
        );
        this.editingSchedule = editingSchedule;

        initializeRepeatDayCheckBoxes();
        buildUserInterface();
        bindActions();
        loadEditingSchedule();
        updateRepeatFieldsEnabled();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(owner);
    }

    /**
     * 입력한 문자열을 일정 데이터로 변환한다.
     * 날짜와 시각은 각각 {@code yyyy-MM-dd}, {@code HH:mm} 형식이어야 한다.
     *
     * @return 현재 입력값으로 생성한 일정
     * @throws IllegalArgumentException 날짜 또는 시각 형식이 올바르지 않은 경우
     */
    public ScheduleData getInputData() {
        try {
            Set<DayOfWeek> repeatDays = EnumSet.noneOf(DayOfWeek.class);
            LocalDate repeatEndDate = null;
            if (repeatCheckBox.isSelected()) {
                repeatDayCheckBoxes.forEach((day, checkBox) -> {
                    if (checkBox.isSelected()) {
                        repeatDays.add(day);
                    }
                });
                repeatEndDate = parseDate(repeatEndDateField.getText(), "반복 종료일");
            }

            return new ScheduleData(
                    editingSchedule == null ? 0 : editingSchedule.getId(),
                    titleField.getText().trim(),
                    Objects.toString(typeComboBox.getSelectedItem(), "").trim(),
                    parseDate(dateField.getText(), "일정 날짜"),
                    parseTime(startTimeField.getText(), "시작 시각"),
                    parseTime(endTimeField.getText(), "종료 시각"),
                    emptyToNull(locationField.getText()),
                    emptyToNull(memoArea.getText()),
                    repeatDays,
                    repeatEndDate
            );
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }

    /**
     * 입력 오류 메시지를 대화상자 위에 표시한다.
     *
     * @param message 사용자에게 안내할 오류 메시지
     */
    public void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, message, "입력 확인", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * 일정 입력 대화상자를 닫고 사용한 화면 자원을 해제한다.
     */
    public void closeDialog() {
        dispose();
    }

    /**
     * 일정 입력 대화상자를 화면에 표시한다.
     */
    public void showDialog() {
        setVisible(true);
    }

    private void initializeRepeatDayCheckBoxes() {
        repeatDayCheckBoxes.put(DayOfWeek.MONDAY, new JCheckBox("월"));
        repeatDayCheckBoxes.put(DayOfWeek.TUESDAY, new JCheckBox("화"));
        repeatDayCheckBoxes.put(DayOfWeek.WEDNESDAY, new JCheckBox("수"));
        repeatDayCheckBoxes.put(DayOfWeek.THURSDAY, new JCheckBox("목"));
        repeatDayCheckBoxes.put(DayOfWeek.FRIDAY, new JCheckBox("금"));
        repeatDayCheckBoxes.put(DayOfWeek.SATURDAY, new JCheckBox("토"));
        repeatDayCheckBoxes.put(DayOfWeek.SUNDAY, new JCheckBox("일"));
    }

    private void buildUserInterface() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;

        int row = 0;
        addField(formPanel, constraints, row++, "제목 *", titleField);
        addField(formPanel, constraints, row++, "유형 *", typeComboBox);
        addField(formPanel, constraints, row++, "날짜 * (" + DATE_GUIDE + ")", dateField);
        addField(formPanel, constraints, row++, "시작 시각 * (" + TIME_GUIDE + ")", startTimeField);
        addField(formPanel, constraints, row++, "종료 시각 * (" + TIME_GUIDE + ")", endTimeField);
        addField(formPanel, constraints, row++, "장소", locationField);
        addField(formPanel, constraints, row++, "메모", new JScrollPane(memoArea));
        addField(formPanel, constraints, row++, "반복", repeatCheckBox);

        JPanel repeatDaysPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        repeatDayCheckBoxes.values().forEach(repeatDaysPanel::add);
        addField(formPanel, constraints, row++, "반복 요일", repeatDaysPanel);
        addField(formPanel, constraints, row, "반복 종료일 (" + DATE_GUIDE + ")", repeatEndDateField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        if (editingSchedule != null && editingSchedule.getId() > 0) {
            buttonPanel.add(deleteButton);
        }
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        setLayout(new BorderLayout());
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(saveButton);
    }

    private void addField(
            JPanel panel,
            GridBagConstraints constraints,
            int row,
            String label,
            java.awt.Component component
    ) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0.0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        panel.add(component, constraints);
    }

    private void bindActions() {
        repeatCheckBox.addActionListener(event -> updateRepeatFieldsEnabled());
        cancelButton.addActionListener(event -> closeDialog());
        saveButton.addActionListener(event -> saveSchedule());
        deleteButton.addActionListener(event -> deleteSchedule());
    }

    private void loadEditingSchedule() {
        if (editingSchedule == null) {
            dateField.setText(LocalDate.now().toString());
            return;
        }

        titleField.setText(editingSchedule.getTitle());
        selectType(editingSchedule.getType());
        dateField.setText(editingSchedule.getDate().toString());
        startTimeField.setText(editingSchedule.getStartTime().toString());
        endTimeField.setText(editingSchedule.getEndTime().toString());
        locationField.setText(Objects.toString(editingSchedule.getLocation(), ""));
        memoArea.setText(Objects.toString(editingSchedule.getMemo(), ""));
        repeatCheckBox.setSelected(editingSchedule.isRepeated());
        editingSchedule.getRepeatDays().forEach(day -> repeatDayCheckBoxes.get(day).setSelected(true));
        if (editingSchedule.getRepeatEndDate() != null) {
            repeatEndDateField.setText(editingSchedule.getRepeatEndDate().toString());
        }
    }

    private void selectType(String type) {
        for (int index = 0; index < typeComboBox.getItemCount(); index++) {
            if (Objects.equals(typeComboBox.getItemAt(index), type)) {
                typeComboBox.setSelectedIndex(index);
                return;
            }
        }
        typeComboBox.addItem(type);
        typeComboBox.setSelectedItem(type);
    }

    private void updateRepeatFieldsEnabled() {
        boolean enabled = repeatCheckBox.isSelected();
        repeatDayCheckBoxes.values().forEach(checkBox -> checkBox.setEnabled(enabled));
        repeatEndDateField.setEnabled(enabled);
    }

    private void saveSchedule() {
        ScheduleData inputData;
        try {
            inputData = getInputData();
        } catch (IllegalArgumentException exception) {
            showValidationError(exception.getMessage());
            return;
        }

        setBusy(true);
        new SwingWorker<ScheduleResult, Void>() {
            @Override
            protected ScheduleResult doInBackground() {
                if (editingSchedule == null) {
                    return scheduleController.saveSchedule(inputData);
                }
                return scheduleController.updateSchedule(inputData);
            }

            @Override
            protected void done() {
                try {
                    ScheduleResult result = get();
                    if (result.isSuccess()) {
                        JOptionPane.showMessageDialog(
                                ScheduleInputDialog.this,
                                result.getMessage(),
                                "일정 관리",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        closeDialog();
                        return;
                    }
                    showValidationError(result.getMessage());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showValidationError("일정 처리가 중단되었습니다.");
                } catch (ExecutionException exception) {
                    showValidationError("일정을 처리하는 중 오류가 발생했습니다.");
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void deleteSchedule() {
        int answer = JOptionPane.showConfirmDialog(
                this,
                "이 일정을 삭제하시겠습니까?",
                "일정 삭제",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        setBusy(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return scheduleController.deleteSchedule(editingSchedule.getId());
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        JOptionPane.showMessageDialog(
                                ScheduleInputDialog.this,
                                "일정이 삭제되었습니다.",
                                "일정 관리",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        closeDialog();
                        return;
                    }
                    showValidationError("삭제할 일정을 찾지 못했거나 삭제하지 못했습니다.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showValidationError("일정 삭제가 중단되었습니다.");
                } catch (ExecutionException exception) {
                    showValidationError("일정을 삭제하는 중 오류가 발생했습니다.");
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        saveButton.setEnabled(!busy);
        cancelButton.setEnabled(!busy);
        deleteButton.setEnabled(!busy);
    }

    private static LocalDate parseDate(String text, String fieldName) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new DateTimeParseException(
                    fieldName + "은 " + DATE_GUIDE + " 형식으로 입력해 주세요.",
                    value,
                    exception.getErrorIndex()
            );
        }
    }

    private static LocalTime parseTime(String text, String fieldName) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw new DateTimeParseException(
                    fieldName + "은 " + TIME_GUIDE + " 형식으로 입력해 주세요.",
                    value,
                    exception.getErrorIndex()
            );
        }
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
