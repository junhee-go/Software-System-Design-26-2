package kr.ac.jnu.daymate.ui;

import kr.ac.jnu.daymate.controller.CourseAssignmentController;
import kr.ac.jnu.daymate.dto.AssignmentResult;
import kr.ac.jnu.daymate.model.AssignmentData;
import kr.ac.jnu.daymate.model.CourseData;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * 과제 정보와 제출 상태를 등록하거나 수정하는 Swing 대화상자이다.
 * 이 대화상자는 Swing 이벤트 디스패치 스레드에서 생성하고 표시해야 한다.
 *
 * @serial exclude
 */
public final class AssignmentInputDialog extends JDialog {
    private static final DateTimeFormatter DUE_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CourseAssignmentController controller;
    private final AssignmentData editingAssignment;
    private final Runnable savedCallback;
    private final JTextField titleField = new JTextField(20);
    private final JComboBox<CourseOption> courseComboBox = new JComboBox<>();
    private final JTextField dueDateTimeField = new JTextField(16);
    private final JTextArea memoArea = new JTextArea(4, 20);
    private final JCheckBox submittedCheckBox = new JCheckBox("제출 완료");
    private final JButton saveButton = new JButton("저장");
    private final JButton cancelButton = new JButton("취소");

    /**
     * 새로운 과제를 등록하는 입력 대화상자를 생성한다.
     *
     * @param owner 대화상자를 소유하는 창. 소유자가 없으면 {@code null}
     * @param controller 과제 처리를 담당할 컨트롤러
     * @param courses 선택할 수 있는 등록 과목 목록
     * @param savedCallback 저장 성공 후 실행할 화면 갱신 작업
     */
    public AssignmentInputDialog(
            Window owner,
            CourseAssignmentController controller,
            List<CourseData> courses,
            Runnable savedCallback
    ) {
        this(owner, controller, courses, null, savedCallback);
    }

    /**
     * 기존 과제를 수정하는 입력 대화상자를 생성한다.
     *
     * @param owner 대화상자를 소유하는 창. 소유자가 없으면 {@code null}
     * @param controller 과제 처리를 담당할 컨트롤러
     * @param courses 선택할 수 있는 등록 과목 목록
     * @param editingAssignment 수정할 과제
     * @param savedCallback 저장 성공 후 실행할 화면 갱신 작업
     */
    public AssignmentInputDialog(
            Window owner,
            CourseAssignmentController controller,
            List<CourseData> courses,
            AssignmentData editingAssignment,
            Runnable savedCallback
    ) {
        super(owner, editingAssignment == null ? "과제 등록" : "과제 수정", ModalityType.APPLICATION_MODAL);
        this.controller = Objects.requireNonNull(controller, "과목·과제 컨트롤러는 null일 수 없습니다.");
        this.editingAssignment = editingAssignment;
        this.savedCallback = Objects.requireNonNull(savedCallback, "저장 완료 작업은 null일 수 없습니다.");

        populateCourses(Objects.requireNonNull(courses, "과목 목록은 null일 수 없습니다."));
        buildUserInterface();
        bindActions();
        loadEditingAssignment();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /**
     * 현재 입력값을 과제 데이터로 변환한다.
     * 마감 일시는 {@code yyyy-MM-dd HH:mm} 형식이어야 한다.
     *
     * @return 현재 입력값으로 생성한 과제
     * @throws IllegalArgumentException 마감 일시 형식이 올바르지 않은 경우
     */
    public AssignmentData getInputData() {
        CourseOption selectedCourse = (CourseOption) courseComboBox.getSelectedItem();
        try {
            return new AssignmentData(
                    editingAssignment == null ? 0 : editingAssignment.getId(),
                    titleField.getText().trim(),
                    selectedCourse == null ? 0 : selectedCourse.id(),
                    parseDueDateTime(dueDateTimeField.getText()),
                    emptyToNull(memoArea.getText()),
                    submittedCheckBox.isSelected()
            );
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "제출 마감 일시는 yyyy-MM-dd HH:mm 형식으로 입력해 주세요.",
                    exception
            );
        }
    }

    /**
     * 과제 입력 대화상자를 화면에 표시한다.
     */
    public void showDialog() {
        setVisible(true);
    }

    private void populateCourses(List<CourseData> courses) {
        for (CourseData course : courses) {
            courseComboBox.addItem(new CourseOption(course.getId(), course.getName()));
        }
    }

    private void buildUserInterface() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        addField(formPanel, constraints, 0, "과제명 *", titleField);
        addField(formPanel, constraints, 1, "관련 과목 *", courseComboBox);
        addField(formPanel, constraints, 2, "마감 일시 * (yyyy-MM-dd HH:mm)", dueDateTimeField);
        addField(formPanel, constraints, 3, "메모", new JScrollPane(memoArea));
        addField(formPanel, constraints, 4, "제출 상태", submittedCheckBox);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
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
        cancelButton.addActionListener(event -> dispose());
        saveButton.addActionListener(event -> saveAssignment());
    }

    private void loadEditingAssignment() {
        if (editingAssignment == null) {
            return;
        }
        titleField.setText(editingAssignment.getTitle());
        selectCourse(editingAssignment.getCourseId());
        dueDateTimeField.setText(editingAssignment.getDueDateTime().format(DUE_DATE_TIME_FORMAT));
        memoArea.setText(Objects.toString(editingAssignment.getMemo(), ""));
        submittedCheckBox.setSelected(editingAssignment.isSubmitted());
    }

    private void selectCourse(int courseId) {
        for (int index = 0; index < courseComboBox.getItemCount(); index++) {
            if (courseComboBox.getItemAt(index).id() == courseId) {
                courseComboBox.setSelectedIndex(index);
                return;
            }
        }
    }

    private void saveAssignment() {
        AssignmentData input;
        try {
            input = getInputData();
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage(), JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!controller.validateAssignment(input)) {
            showMessage("과제명, 관련 과목, 제출 마감 일시를 확인해 주세요.", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!input.isSubmitted() && input.getDueDateTime().isBefore(LocalDateTime.now())) {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "제출 마감 일시가 이미 지났습니다. 기한 초과 과제로 저장하시겠습니까?",
                    "마감 일시 확인",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }

        setBusy(true);
        new SwingWorker<AssignmentResult, Void>() {
            @Override
            protected AssignmentResult doInBackground() {
                if (editingAssignment == null) {
                    return controller.saveAssignment(input);
                }
                return controller.updateAssignment(input);
            }

            @Override
            protected void done() {
                try {
                    AssignmentResult result = get();
                    if (result.isSuccess()) {
                        showMessage(result.getMessage(), JOptionPane.INFORMATION_MESSAGE);
                        savedCallback.run();
                        dispose();
                        return;
                    }
                    showMessage(result.getMessage(), JOptionPane.WARNING_MESSAGE);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showMessage("과제 저장이 중단되었습니다.", JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException exception) {
                    showMessage("과제를 저장하는 중 오류가 발생했습니다.", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        saveButton.setEnabled(!busy);
        cancelButton.setEnabled(!busy);
    }

    private void showMessage(String message, int messageType) {
        JOptionPane.showMessageDialog(this, message, "과제 관리", messageType);
    }

    private static LocalDateTime parseDueDateTime(String text) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(value, DUE_DATE_TIME_FORMAT);
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record CourseOption(int id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }
}
