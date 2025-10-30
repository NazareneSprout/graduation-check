package sprout.app.sakmvp1;

/**
 * 자격증 데이터 모델 (POJO)
 * RecyclerView의 각 항목에 표시될 데이터를 담습니다.
 */
public class Certificate {

    private String title;       // 자격증 이름
    private String issuer;      // 주최 기관
    private String dDay;        // D-Day (e.g., "D-10", "접수중")
    private int viewCount;      // 조회수
    private String department;  // 관련 학부 (필터링을 위해)

    // Firestore 연동을 위한 기본 생성자
    public Certificate() {
    }

    // 데이터 생성을 위한 생성자
    public Certificate(String title, String issuer, String dDay, int viewCount, String department) {
        this.title = title;
        this.issuer = issuer;
        this.dDay = dDay;
        this.viewCount = viewCount;
        this.department = department; // "IT학부", "경찰행정학부" 등
    }

    // --- Getter ---
    public String getTitle() {
        return title;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getDDay() {
        return dDay;
    }

    public int getViewCount() {
        return viewCount;
    }

    public String getDepartment() {
        return department;
    }

    // --- Setter (Firestore 사용 시 필요할 수 있음) ---
    public void setTitle(String title) {
        this.title = title;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setDDay(String dDay) {
        this.dDay = dDay;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
