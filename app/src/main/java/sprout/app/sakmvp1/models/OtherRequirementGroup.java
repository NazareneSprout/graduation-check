package sprout.app.sakmvp1.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 기타 졸업요건 그룹 모델
 *
 * 특정 학번/학부/트랙에 해당하는 기타 졸업요건들의 그룹
 */
public class OtherRequirementGroup implements Serializable {
    private String id;                              // Firestore 문서 ID
    private String studentYear;                     // 적용 학번 (예: "2020")
    private String department;                      // 적용 학부 (예: "컴퓨터공학부")
    private String track;                           // 적용 트랙 (예: "일반")
    private List<RequirementItem> requirements;     // 기타 졸업요건 목록
    private long timestamp;                         // 생성/수정 시간

    /**
     * 개별 졸업요건 항목
     */
    public static class RequirementItem implements Serializable {
        private String name;            // 요건명 (예: "TLC", "채플")
        private String format;          // 형식: "횟수" 또는 "통과"
        private int count;              // 횟수 (format이 "횟수"일 때만 사용)
        private boolean isPass;         // 통과 여부 (format이 "통과"일 때만 사용)

        public RequirementItem() {}

        public RequirementItem(String name, String format, int count, boolean isPass) {
            this.name = name;
            this.format = format;
            this.count = count;
            this.isPass = isPass;
        }

        // 기존 방식과의 호환성을 위한 생성자 (deprecated)
        @Deprecated
        public RequirementItem(String name, String description) {
            this.name = name;
            // description에서 format과 count/isPass 파싱
            if (description != null) {
                if (description.contains("회")) {
                    this.format = "횟수";
                    String countStr = description.replaceAll("[^0-9]", "");
                    this.count = countStr.isEmpty() ? 0 : Integer.parseInt(countStr);
                    this.isPass = false;
                } else if (description.equals("통과")) {
                    this.format = "통과";
                    this.count = 0;
                    this.isPass = true;
                } else {
                    this.format = "통과";
                    this.count = 0;
                    this.isPass = false;
                }
            }
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public boolean isPass() {
            return isPass;
        }

        public void setPass(boolean pass) {
            isPass = pass;
        }

        /**
         * 설명 텍스트 자동 생성
         */
        public String getDescription() {
            if ("횟수".equals(format)) {
                return count + "회 이상";
            } else if ("통과".equals(format)) {
                return "통과";
            }
            return "";
        }

        // Firestore 저장 시 description은 자동 생성되므로 setter는 무시
        @Deprecated
        public void setDescription(String description) {
            // 호환성을 위해 유지하지만 사용하지 않음
        }
    }

    public OtherRequirementGroup() {
        this.requirements = new ArrayList<>();
    }

    public OtherRequirementGroup(String studentYear, String department, String track) {
        this.studentYear = studentYear;
        this.department = department;
        this.track = track;
        this.requirements = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentYear() {
        return studentYear;
    }

    public void setStudentYear(String studentYear) {
        this.studentYear = studentYear;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public List<RequirementItem> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<RequirementItem> requirements) {
        this.requirements = requirements;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 그룹 제목 생성 (예: "2020학번 · 컴퓨터공학부 · 일반")
     */
    public String getGroupTitle() {
        return studentYear + "학번 · " + department + " · " + track;
    }

    /**
     * 요건 개수 텍스트 생성
     */
    public String getRequirementCountText() {
        int count = requirements != null ? requirements.size() : 0;
        return count + "개의 기타 졸업요건";
    }
}
