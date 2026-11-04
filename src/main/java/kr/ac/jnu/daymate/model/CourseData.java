package kr.ac.jnu.daymate.model;

/**
 * 수강 과목의 식별 정보와 기본 정보를 저장한다.
 */
public final class CourseData {
    private final int id;
    private String name;
    private String professor;

    /**
     * 수강 과목 데이터를 생성한다.
     *
     * @param id 과목 식별자. 저장 전에는 {@code 0}을 사용한다.
     * @param name 과목명
     * @param professor 담당 교수명
     */
    public CourseData(int id, String name, String professor) {
        this.id = id;
        this.name = name;
        this.professor = professor;
    }

    /**
     * 과목 식별자를 반환한다.
     *
     * @return 과목 식별자
     */
    public int getId() {
        return id;
    }

    /**
     * 과목명을 반환한다.
     *
     * @return 과목명
     */
    public String getName() {
        return name;
    }

    /**
     * 담당 교수명을 반환한다.
     *
     * @return 담당 교수명
     */
    public String getProfessor() {
        return professor;
    }

    /**
     * 수정할 수 있는 과목 정보를 변경한다.
     *
     * @param name 새로운 과목명
     * @param professor 새로운 담당 교수명
     */
    public void updateInfo(String name, String professor) {
        this.name = name;
        this.professor = professor;
    }
}
