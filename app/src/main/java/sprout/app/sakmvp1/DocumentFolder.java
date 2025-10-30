package sprout.app.sakmvp1;

import com.google.firebase.firestore.Exclude;

/**
 * 서류 폴더 데이터 모델
 */
public class DocumentFolder {
    @Exclude
    private String id;

    private String name;          // 폴더명 (예: "복수전공 신청")
    private int order;            // 표시 순서
    private long createdAt;       // 생성 시간 (Unix timestamp)

    public DocumentFolder() {
        // Firestore 기본 생성자
    }

    public DocumentFolder(String name, int order) {
        this.name = name;
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
