package sprout.app.sakmvp1;

import com.google.firebase.firestore.Exclude;

/**
 * 서류 파일 데이터 모델
 */
public class DocumentFile {
    @Exclude
    private String id;

    private String name;           // 파일명 (예: "성적증명서")
    private String description;    // 설명 (예: "최근 1년 성적증명서")
    private String url;            // 다운로드 URL 또는 참고 링크 (선택)
    private int order;             // 표시 순서
    private long createdAt;        // 생성 시간 (Unix timestamp)

    public DocumentFile() {
        // Firestore 기본 생성자
    }

    public DocumentFile(String name, String description, String url, int order) {
        this.name = name;
        this.description = description;
        this.url = url;
        this.order = order;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
