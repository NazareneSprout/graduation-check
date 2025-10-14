package sprout.app.sakmvp1;

/**
 * 시간표 수업 정보 데이터 클래스
 */
public class ScheduleItem {
    private int dayIndex;           // 요일 인덱스 (0=월, 1=화, 2=수, 3=목, 4=금)
    private int startHour;          // 시작 시간
    private int startMinute;        // 시작 분
    private int endHour;            // 종료 시간
    private int endMinute;          // 종료 분
    private String subjectName;     // 과목명
    private String professorName;   // 교수명
    private String location;        // 강의실

    public ScheduleItem() {
        // Firestore 역직렬화를 위한 기본 생성자
    }

    public ScheduleItem(int dayIndex, int startHour, int startMinute, int endHour, int endMinute,
                        String subjectName, String professorName, String location) {
        this.dayIndex = dayIndex;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.endHour = endHour;
        this.endMinute = endMinute;
        this.subjectName = subjectName;
        this.professorName = professorName;
        this.location = location;
    }

    // Getters and Setters
    public int getDayIndex() {
        return dayIndex;
    }

    public void setDayIndex(int dayIndex) {
        this.dayIndex = dayIndex;
    }

    public int getStartHour() {
        return startHour;
    }

    public void setStartHour(int startHour) {
        this.startHour = startHour;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(int startMinute) {
        this.startMinute = startMinute;
    }

    public int getEndHour() {
        return endHour;
    }

    public void setEndHour(int endHour) {
        this.endHour = endHour;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public void setEndMinute(int endMinute) {
        this.endMinute = endMinute;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getProfessorName() {
        return professorName;
    }

    public void setProfessorName(String professorName) {
        this.professorName = professorName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
