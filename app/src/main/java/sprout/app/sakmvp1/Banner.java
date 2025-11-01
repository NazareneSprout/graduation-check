package sprout.app.sakmvp1;

/**
 * 배너 데이터 모델
 *
 * 배너 타입:
 * - INTERNAL: 앱 내부 기능으로 이동 (graduation, recommendation, certificate 등)
 * - EXTERNAL: 외부 웹페이지 (WebView)
 * - NONE: 클릭 불가 (단순 공지)
 */
public class Banner {
    private String id;                  // 배너 ID (자동 생성)
    private String imageUrl;            // 배너 이미지 URL
    private String title;               // 배너 제목 (관리용)

    // 배너 타입 및 이동 설정
    private String type;                // INTERNAL, EXTERNAL, NONE
    private String targetScreen;        // 내부 화면: graduation, recommendation, certificate, documents, meal, timetable
    private String targetUrl;           // 외부 링크

    // 관리 기능
    private int priority;               // 우선순위 (낮을수록 앞에 표시, 기본값: 99)
    private boolean active;             // 활성화 여부 (기본값: true)
    private long startDate;             // 시작일 (timestamp, 0이면 제한 없음)
    private long endDate;               // 종료일 (timestamp, 0이면 제한 없음)
    private String targetDepartment;    // 대상 학부 ("ALL"이면 전체)

    // Firestore Deserialization을 위한 기본 생성자
    public Banner() {
        this.type = "EXTERNAL";
        this.priority = 99;
        this.active = true;
        this.startDate = 0;
        this.endDate = 0;
        this.targetDepartment = "ALL";
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTargetScreen() {
        return targetScreen;
    }

    public void setTargetScreen(String targetScreen) {
        this.targetScreen = targetScreen;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public String getTargetDepartment() {
        return targetDepartment;
    }

    public void setTargetDepartment(String targetDepartment) {
        this.targetDepartment = targetDepartment;
    }

    /**
     * 현재 배너가 활성화 기간 내인지 확인
     */
    public boolean isInActivePeriod() {
        long now = System.currentTimeMillis();

        // startDate가 0이면 시작일 제한 없음
        boolean afterStart = (startDate == 0) || (now >= startDate);

        // endDate가 0이면 종료일 제한 없음
        boolean beforeEnd = (endDate == 0) || (now <= endDate);

        return afterStart && beforeEnd;
    }

    /**
     * 특정 학부 학생에게 표시 가능한지 확인
     * @param userDepartment 사용자의 학부
     */
    public boolean isVisibleForDepartment(String userDepartment) {
        if ("ALL".equals(targetDepartment)) {
            return true;
        }
        if (userDepartment == null) {
            return false;
        }
        return userDepartment.equals(targetDepartment);
    }
}