package kr.ac.jnu.daymate.ui;

import kr.ac.jnu.daymate.controller.AlertController;
import kr.ac.jnu.daymate.model.NotificationSetting;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * 일일 요약 알림의 활성화 상태, 시각 및 포함 항목을 설정하는 Swing 대화상자이다.
 *
 * @serial exclude
 */
public final class AlertSettingDialog extends JDialog {
    private final AlertController controller;
    private final Runnable savedCallback;
    private final JCheckBox enabledCheckBox = new JCheckBox("일일 요약 알림 사용");
    private final JTextField alertTimeField = new JTextField(8);
    private final JCheckBox includeScheduleCheckBox = new JCheckBox("오늘의 일정 포함");
    private final JCheckBox includeAssignmentCheckBox = new JCheckBox("3일 이내 미제출 과제 포함");
    private final JButton saveButton = new JButton("저장");
    private final JButton cancelButton = new JButton("취소");

    /**
     * 알림 설정 대화상자를 생성한다.
     *
     * @param owner 대화상자를 소유하는 창. 소유자가 없으면 {@code null}
     * @param controller 알림 설정과 타이머를 관리할 컨트롤러
     */
    public AlertSettingDialog(Window owner, AlertController controller) {
        this(owner, controller, () -> { });
    }

    /**
     * 저장 완료 후 지정한 작업을 실행하는 알림 설정 대화상자를 생성한다.
     *
     * @param owner 대화상자를 소유하는 창. 소유자가 없으면 {@code null}
     * @param controller 알림 설정과 타이머를 관리할 컨트롤러
     * @param savedCallback 설정 저장 성공 후 실행할 작업
     */
    public AlertSettingDialog(
            Window owner,
            AlertController controller,
            Runnable savedCallback
    ) {
        super(owner, "일일 요약 알림 설정", ModalityType.APPLICATION_MODAL);
        this.controller = Objects.requireNonNull(controller, "알림 컨트롤러는 null일 수 없습니다.");
        this.savedCallback = Objects.requireNonNull(savedCallback, "저장 완료 작업은 null일 수 없습니다.");

        buildUserInterface();
        bindActions();
        loadSetting();
        updateFieldsEnabled();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /**
     * 현재 입력값을 알림 설정으로 변환한다.
     * 알림 시각은 {@code HH:mm} 형식이어야 한다.
     *
     * @return 현재 입력값으로 생성한 알림 설정
     * @throws IllegalArgumentException 활성화된 설정의 시각 형식이 올바르지 않은 경우
     */
    public NotificationSetting getInputData() {
        LocalTime alertTime = null;
        if (enabledCheckBox.isSelected()) {
            String value = alertTimeField.getText().trim();
            if (value.isEmpty()) {
                throw new IllegalArgumentException("알림 시각을 HH:mm 형식으로 입력해 주세요.");
            }
            try {
                alertTime = LocalTime.parse(value);
            } catch (DateTimeParseException exception) {
                throw new IllegalArgumentException("알림 시각을 HH:mm 형식으로 입력해 주세요.", exception);
            }
        }
        return new NotificationSetting(
                enabledCheckBox.isSelected(),
                alertTime,
                includeScheduleCheckBox.isSelected(),
                includeAssignmentCheckBox.isSelected()
        );
    }

    /**
     * 알림 설정 대화상자를 화면에 표시한다.
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
        constraints.anchor = GridBagConstraints.WEST;

        addField(formPanel, constraints, 0, "알림", enabledCheckBox);
        addField(formPanel, constraints, 1, "알림 시각 (HH:mm)", alertTimeField);
        addField(formPanel, constraints, 2, "포함 항목", includeScheduleCheckBox);
        addField(formPanel, constraints, 3, "", includeAssignmentCheckBox);

        JPanel descriptionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        descriptionPanel.add(new JLabel("프로그램이 실행 중일 때 지정된 시각에 한 번 알림을 표시합니다."));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        setLayout(new BorderLayout());
        add(descriptionPanel, BorderLayout.NORTH);
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
        panel.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        panel.add(component, constraints);
    }

    private void bindActions() {
        enabledCheckBox.addActionListener(event -> updateFieldsEnabled());
        cancelButton.addActionListener(event -> dispose());
        saveButton.addActionListener(event -> saveSetting());
    }

    private void loadSetting() {
        NotificationSetting setting = controller.loadNotificationSettings();
        enabledCheckBox.setSelected(setting.isEnabled());
        if (setting.getAlertTime() != null) {
            alertTimeField.setText(setting.getAlertTime().toString());
        }
        includeScheduleCheckBox.setSelected(setting.isIncludeSchedule());
        includeAssignmentCheckBox.setSelected(setting.isIncludeAssignment());
    }

    private void updateFieldsEnabled() {
        boolean enabled = enabledCheckBox.isSelected();
        alertTimeField.setEnabled(enabled);
        includeScheduleCheckBox.setEnabled(enabled);
        includeAssignmentCheckBox.setEnabled(enabled);
    }

    private void saveSetting() {
        NotificationSetting input;
        try {
            input = getInputData();
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage(), JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!input.isValid()) {
            showMessage("알림에는 오늘의 일정 또는 과제를 하나 이상 포함해야 합니다.",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        setBusy(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return controller.saveNotificationSettings(input);
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        controller.startTimer();
                        savedCallback.run();
                        showMessage("알림 설정이 저장되었습니다.", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                        return;
                    }
                    showMessage("알림 설정을 저장하지 못했습니다.", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showMessage("알림 설정 저장이 중단되었습니다.", JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException exception) {
                    showMessage("알림 설정을 저장하는 중 오류가 발생했습니다.", JOptionPane.ERROR_MESSAGE);
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
        JOptionPane.showMessageDialog(this, message, "일일 요약 알림", messageType);
    }
}
