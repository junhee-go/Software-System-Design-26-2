package kr.ac.jnu.daymate.ui;

import kr.ac.jnu.daymate.controller.CourseAssignmentController;
import kr.ac.jnu.daymate.model.AssignmentData;
import kr.ac.jnu.daymate.model.CourseData;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * 등록된 수강 과목과 과제를 표로 표시하고 관리 기능을 제공하는 Swing 화면이다.
 * 데이터 조회와 변경은 백그라운드 작업으로 실행하며 화면 갱신은 이벤트 디스패치 스레드에서 수행한다.
 *
 * @serial exclude
 */
public final class CourseAssignmentView extends JPanel {
    private static final DateTimeFormatter DUE_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CourseAssignmentController controller;
    private final DefaultTableModel courseTableModel = createTableModel("과목명", "담당 교수");
    private final DefaultTableModel assignmentTableModel = createTableModel(
            "과제명", "관련 과목", "마감 일시", "제출 상태"
    );
    private final JTable courseTable = new JTable(courseTableModel);
    private final JTable assignmentTable = new JTable(assignmentTableModel);
    private final JButton addCourseButton = new JButton("과목 등록");
    private final JButton editCourseButton = new JButton("과목 수정");
    private final JButton deleteCourseButton = new JButton("과목 삭제");
    private final JButton addAssignmentButton = new JButton("과제 등록");
    private final JButton editAssignmentButton = new JButton("과제 수정");
    private final JButton deleteAssignmentButton = new JButton("과제 삭제");
    private final JButton toggleSubmissionButton = new JButton("제출 상태 변경");
    private List<CourseData> courses = List.of();
    private List<AssignmentData> assignments = List.of();

    /**
     * 지정한 컨트롤러를 사용하는 과목·과제 관리 화면을 생성하고 목록을 불러온다.
     * 이 화면은 Swing 이벤트 디스패치 스레드에서 생성해야 한다.
     *
     * @param controller 과목과 과제 처리를 담당할 컨트롤러
     * @throws NullPointerException {@code controller}가 {@code null}인 경우
     */
    public CourseAssignmentView(CourseAssignmentController controller) {
        this.controller = Objects.requireNonNull(controller, "과목·과제 컨트롤러는 null일 수 없습니다.");
        buildUserInterface();
        bindActions();
        refreshData();
    }

    /**
     * 캐시에서 과목과 과제 목록을 다시 불러와 두 표를 갱신한다.
     */
    public void refreshData() {
        setBusy(true);
        new SwingWorker<ManagementData, Void>() {
            @Override
            protected ManagementData doInBackground() {
                return new ManagementData(controller.getCourses(), controller.getAssignments());
            }

            @Override
            protected void done() {
                try {
                    ManagementData data = get();
                    displayCourses(data.courses());
                    displayAssignments(data.assignments());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showMessage("과목과 과제 목록 조회가 중단되었습니다.");
                } catch (ExecutionException exception) {
                    showMessage("과목과 과제 목록을 불러오지 못했습니다.");
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    /**
     * 지정한 과목 목록을 과목 표에 표시한다.
     *
     * @param courseList 표시할 과목 목록
     */
    public void displayCourses(List<CourseData> courseList) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> displayCourses(courseList));
            return;
        }
        courses = List.copyOf(courseList);
        courseTableModel.setRowCount(0);
        for (CourseData course : courses) {
            courseTableModel.addRow(new Object[]{course.getName(), course.getProfessor()});
        }
    }

    /**
     * 지정한 과제 목록을 과제 표에 표시한다.
     *
     * @param assignmentList 표시할 과제 목록
     */
    public void displayAssignments(List<AssignmentData> assignmentList) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> displayAssignments(assignmentList));
            return;
        }
        assignments = List.copyOf(assignmentList);
        assignmentTableModel.setRowCount(0);
        for (AssignmentData assignment : assignments) {
            assignmentTableModel.addRow(new Object[]{
                    assignment.getTitle(),
                    findCourseName(assignment.getCourseId()),
                    assignment.getDueDateTime().format(DUE_DATE_TIME_FORMAT),
                    assignment.isSubmitted() ? "제출 완료" : "미제출"
            });
        }
    }

    /**
     * 과목·과제 관리 안내 메시지를 표시한다.
     *
     * @param message 사용자에게 표시할 메시지
     */
    public void showMessage(String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showMessage(message));
            return;
        }
        JOptionPane.showMessageDialog(this, message, "수강 과목 및 과제 관리", JOptionPane.INFORMATION_MESSAGE);
    }

    private void buildUserInterface() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("수강 과목 및 과제 관리");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        courseTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        assignmentTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        JPanel coursePanel = createSection(
                "수강 과목",
                courseTable,
                addCourseButton,
                editCourseButton,
                deleteCourseButton
        );
        JPanel assignmentPanel = createSection(
                "과제",
                assignmentTable,
                addAssignmentButton,
                editAssignmentButton,
                deleteAssignmentButton,
                toggleSubmissionButton
        );

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, coursePanel, assignmentPanel);
        splitPane.setResizeWeight(0.4);
        splitPane.setBorder(null);

        add(titleLabel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createSection(String title, JTable table, JButton... buttons) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        for (JButton button : buttons) {
            buttonPanel.add(button);
        }
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void bindActions() {
        addCourseButton.addActionListener(event -> openCourseDialog(null));
        editCourseButton.addActionListener(event -> editSelectedCourse());
        deleteCourseButton.addActionListener(event -> deleteSelectedCourse());
        addAssignmentButton.addActionListener(event -> openAssignmentDialog(null));
        editAssignmentButton.addActionListener(event -> editSelectedAssignment());
        deleteAssignmentButton.addActionListener(event -> deleteSelectedAssignment());
        toggleSubmissionButton.addActionListener(event -> toggleSelectedAssignmentStatus());
    }

    private void openCourseDialog(CourseData editingCourse) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        CourseInputDialog dialog;
        if (editingCourse == null) {
            dialog = new CourseInputDialog(owner, controller, this::refreshData);
        } else {
            dialog = new CourseInputDialog(owner, controller, editingCourse, this::refreshData);
        }
        dialog.showDialog();
    }

    private void openAssignmentDialog(AssignmentData editingAssignment) {
        if (courses.isEmpty()) {
            showMessage("과제를 등록하려면 먼저 수강 과목을 등록해 주세요.");
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        AssignmentInputDialog dialog;
        if (editingAssignment == null) {
            dialog = new AssignmentInputDialog(owner, controller, courses, this::refreshData);
        } else {
            dialog = new AssignmentInputDialog(
                    owner,
                    controller,
                    courses,
                    editingAssignment,
                    this::refreshData
            );
        }
        dialog.showDialog();
    }

    private void editSelectedCourse() {
        CourseData selected = getSelectedCourse();
        if (selected == null) {
            showMessage("수정할 과목을 선택해 주세요.");
            return;
        }
        openCourseDialog(selected);
    }

    private void deleteSelectedCourse() {
        CourseData selected = getSelectedCourse();
        if (selected == null) {
            showMessage("삭제할 과목을 선택해 주세요.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(
                this,
                "선택한 과목을 삭제하시겠습니까?",
                "과목 삭제",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        runBooleanTask(
                () -> controller.deleteCourse(selected.getId()),
                "과목이 삭제되었습니다.",
                "연결된 과제가 있거나 과목을 찾을 수 없어 삭제하지 못했습니다."
        );
    }

    private void editSelectedAssignment() {
        AssignmentData selected = getSelectedAssignment();
        if (selected == null) {
            showMessage("수정할 과제를 선택해 주세요.");
            return;
        }
        openAssignmentDialog(selected);
    }

    private void deleteSelectedAssignment() {
        AssignmentData selected = getSelectedAssignment();
        if (selected == null) {
            showMessage("삭제할 과제를 선택해 주세요.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(
                this,
                "선택한 과제를 삭제하시겠습니까?",
                "과제 삭제",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        runBooleanTask(
                () -> controller.deleteAssignment(selected.getId()),
                "과제가 삭제되었습니다.",
                "과제를 찾을 수 없거나 삭제하지 못했습니다."
        );
    }

    private void toggleSelectedAssignmentStatus() {
        AssignmentData selected = getSelectedAssignment();
        if (selected == null) {
            showMessage("제출 상태를 변경할 과제를 선택해 주세요.");
            return;
        }
        boolean newStatus = !selected.isSubmitted();
        runBooleanTask(
                () -> controller.changeSubmissionStatus(selected.getId(), newStatus),
                newStatus ? "과제를 제출 완료로 변경했습니다." : "과제를 미제출로 변경했습니다.",
                "과제 제출 상태를 변경하지 못했습니다."
        );
    }

    private CourseData getSelectedCourse() {
        int row = courseTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int modelRow = courseTable.convertRowIndexToModel(row);
        return modelRow >= 0 && modelRow < courses.size() ? courses.get(modelRow) : null;
    }

    private AssignmentData getSelectedAssignment() {
        int row = assignmentTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int modelRow = assignmentTable.convertRowIndexToModel(row);
        return modelRow >= 0 && modelRow < assignments.size() ? assignments.get(modelRow) : null;
    }

    private String findCourseName(int courseId) {
        return courses.stream()
                .filter(course -> course.getId() == courseId)
                .map(CourseData::getName)
                .findFirst()
                .orElse("알 수 없는 과목");
    }

    private void runBooleanTask(Supplier<Boolean> task, String successMessage, String failureMessage) {
        setBusy(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return task.get();
            }

            @Override
            protected void done() {
                boolean refreshStarted = false;
                try {
                    if (Boolean.TRUE.equals(get())) {
                        showMessage(successMessage);
                        refreshData();
                        refreshStarted = true;
                        return;
                    }
                    showMessage(failureMessage);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showMessage("요청한 작업이 중단되었습니다.");
                } catch (ExecutionException exception) {
                    showMessage("요청한 작업을 처리하는 중 오류가 발생했습니다.");
                } finally {
                    if (!refreshStarted) {
                        setBusy(false);
                    }
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        addCourseButton.setEnabled(!busy);
        editCourseButton.setEnabled(!busy);
        deleteCourseButton.setEnabled(!busy);
        addAssignmentButton.setEnabled(!busy);
        editAssignmentButton.setEnabled(!busy);
        deleteAssignmentButton.setEnabled(!busy);
        toggleSubmissionButton.setEnabled(!busy);
    }

    private static DefaultTableModel createTableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private record ManagementData(List<CourseData> courses, List<AssignmentData> assignments) {
    }
}
