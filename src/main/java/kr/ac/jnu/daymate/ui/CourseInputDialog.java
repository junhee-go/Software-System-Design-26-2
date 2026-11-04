package kr.ac.jnu.daymate.ui;

import kr.ac.jnu.daymate.controller.CourseAssignmentController;
import kr.ac.jnu.daymate.model.CourseData;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * 수강 과목의 이름과 담당 교수 정보를 등록하거나 수정하는 Swing 대화상자이다.
 * 이 대화상자는 Swing 이벤트 디스패치 스레드에서 생성하고 표시해야 한다.
 *
 * @serial exclude
 */
public final class CourseInputDialog extends JDialog {
    private final CourseAssignmentController controller;
    private final CourseData editingCourse;
    private final Runnable savedCallback;
    private final JTextField nameField = new JTextField(20);
    private final JTextField professorField = new JTextField(20);
    private final JButton saveButton = new JButton("저장");
    private final JButton cancelButton = new JButton("취소");

    /**
     * 새로운 과목을 등록하는 입력 대화상자를 생성한다.
     *
     * @param owner 대화상자를 소유하는 창. 소유자가 없으면 {@code null}
     * @param controller 과목 처리를 담당할 컨트롤러
     * @param savedCallback 저장 성공 후 실행할 화면 갱신 작업
     */
    public CourseInputDialog(
            Window owner,
            CourseAssignmentController controller,
            Runnable savedCallback
    ) {
        this(owner, controller, null, savedCallback);
    }

    /**
     * 기존 과목을 수정하는 입력 대화상자를 생성한다.
     *
     * @param owner 대화상자를 소유하는 창. 소유자가 없으면 {@code null}
     * @param controller 과목 처리를 담당할 컨트롤러
     * @param editingCourse 수정할 과목
     * @param savedCallback 저장 성공 후 실행할 화면 갱신 작업
     */
    public CourseInputDialog(
            Window owner,
            CourseAssignmentController controller,
            CourseData editingCourse,
            Runnable savedCallback
    ) {
        super(owner, editingCourse == null ? "과목 등록" : "과목 수정", ModalityType.APPLICATION_MODAL);
        this.controller = Objects.requireNonNull(controller, "과목·과제 컨트롤러는 null일 수 없습니다.");
        this.editingCourse = editingCourse;
        this.savedCallback = Objects.requireNonNull(savedCallback, "저장 완료 작업은 null일 수 없습니다.");

        buildUserInterface();
        bindActions();
        loadEditingCourse();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /**
     * 현재 입력값을 과목 데이터로 변환한다.
     *
     * @return 현재 입력값으로 생성한 과목
     */
    public CourseData getInputData() {
        return new CourseData(
                editingCourse == null ? 0 : editingCourse.getId(),
                nameField.getText().trim(),
                professorField.getText().trim()
        );
    }

    /**
     * 과목 입력 대화상자를 화면에 표시한다.
     */
    public void showDialog() {
        setVisible(true);
    }

    private void buildUserInterface() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        addField(formPanel, constraints, 0, "과목명 *", nameField);
        addField(formPanel, constraints, 1, "담당 교수 *", professorField);

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
            JTextField field
    ) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0.0;
        panel.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        panel.add(field, constraints);
    }

    private void bindActions() {
        cancelButton.addActionListener(event -> dispose());
        saveButton.addActionListener(event -> saveCourse());
    }

    private void loadEditingCourse() {
        if (editingCourse == null) {
            return;
        }
        nameField.setText(editingCourse.getName());
        professorField.setText(editingCourse.getProfessor());
    }

    private void saveCourse() {
        CourseData input = getInputData();
        if (!controller.validateCourse(input)) {
            showMessage("과목명과 담당 교수명을 모두 입력해 주세요.", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setBusy(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                if (editingCourse == null) {
                    return controller.saveCourse(input);
                }
                return controller.updateCourse(input);
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        showMessage(
                                editingCourse == null ? "과목이 등록되었습니다." : "과목이 수정되었습니다.",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        savedCallback.run();
                        dispose();
                        return;
                    }
                    showMessage("과목을 저장하지 못했습니다. 입력 정보를 확인해 주세요.",
                            JOptionPane.WARNING_MESSAGE);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showMessage("과목 저장이 중단되었습니다.", JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException exception) {
                    showMessage("과목을 저장하는 중 오류가 발생했습니다.", JOptionPane.ERROR_MESSAGE);
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
        JOptionPane.showMessageDialog(this, message, "수강 과목 관리", messageType);
    }
}
