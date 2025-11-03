package sprout.app.sakmvp1; // 패키지 경로 확인

import com.google.firebase.firestore.DocumentId;

import java.util.HashMap;
import java.util.Map;

/**
 * Firestore 'certificates' 컬렉션의 문서 모델
 */
public class Certificate {

    @DocumentId
    private String id; // Firestore 문서 ID

    private String title;       // 자격증 이름
    private String issuer;      // 발급 기관
    private String department;  // 필터링용 (예: "IT학부", "경찰행정학부")
    private String targetUrl;   // 클릭 시 이동할 URL
    private long bookmarkCount; // 인기순 정렬용

    // 어떤 유저가 북마크했는지 저장 (UID: true)
    private Map<String, Boolean> bookmarks = new HashMap<>();

    // Firestore 매핑을 위한 기본 생성자
    public Certificate() {
    }

    // --- Getter 및 Setter ---
    // (모든 필드에 대한 Getter와 Setter가 있어야 Firestore가 정상 동작합니다)

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }


    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public long getBookmarkCount() {
        return bookmarkCount;
    }

    public void setBookmarkCount(long bookmarkCount) {
        this.bookmarkCount = bookmarkCount;
    }

    public Map<String, Boolean> getBookmarks() {
        return bookmarks;
    }

    public void setBookmarks(Map<String, Boolean> bookmarks) {
        this.bookmarks = bookmarks;
    }
}
