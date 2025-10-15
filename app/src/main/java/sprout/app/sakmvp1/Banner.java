package sprout.app.sakmvp1;

public class Banner {
    private String imageUrl;
    private String targetUrl; // << 추가: 클릭 시 이동할 URL

    // Firestore Deserialization을 위해 꼭 비어있는 생성자를 만들어주세요.
    public Banner() {}

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // << 추가: targetUrl의 getter와 setter
    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }
}