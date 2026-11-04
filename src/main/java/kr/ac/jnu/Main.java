package kr.ac.jnu;

/**
 * DayMate 애플리케이션의 실행 진입점이다.
 */
public class Main {
    /**
     * DayMate 애플리케이션을 시작한다.
     *
     * @param args 명령행 인수. 현재는 사용하지 않는다.
     */
    public static void main(String[] args) {
        System.out.printf("Hello and welcome!");

        for (int i = 1; i <= 5; i++) {
            System.out.println("i = " + i);
        }
    }
}
