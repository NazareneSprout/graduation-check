package sprout.app.sakmvp1.timetable;

import java.util.List;

public class GroupModel {
    private String documentId; // 문서 ID (수정/삭제용)
    private String groupName;  // 그룹 이름
    private String password;   // 비밀번호
    private String description;// 설명
    private List<String> members; // 멤버들의 UID 리스트

    // Firestore는 빈 생성자가 필수입니다.
    public GroupModel() {}

    public GroupModel(String groupName, String password, String description, List<String> members) {
        this.groupName = groupName;
        this.password = password;
        this.description = description;
        this.members = members;
    }

    // Getter & Setter
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getGroupName() { return groupName; }
    public String getPassword() { return password; }
    public String getDescription() { return description; }
    public List<String> getMembers() { return members; }
}