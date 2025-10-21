package sprout.app.sakmvp1;

import com.google.firebase.firestore.PropertyName;

/**
 * 졸업요건 데이터 모델
 * Firestore의 graduation_requirements 컬렉션 구조
 * 문서 ID 형식: "졸업요건_{학과}_{트랙}_{학번}" (예: "졸업요건_IT학부_멀티미디어_2023")
 *
 * Firestore는 한글 필드명을 사용하므로 @PropertyName 어노테이션으로 매핑
 */
public class GraduationRequirement {
    private String id; // Firestore document ID (예: "IT학부_멀티미디어_2023")
    private String department; // 학과 (예: "IT학부")
    private String track; // 트랙 (예: "멀티미디어")
    private String year; // 학번 (예: "2023")

    // 졸업요건 상세 (Firestore는 한글 필드명 사용)
    private int totalCredits; // 총 이수학점

    @PropertyName("전공필수")
    private int majorRequired; // 전공 필수

    @PropertyName("전공선택")
    private int majorElective; // 전공 선택

    @PropertyName("교양필수")
    private int generalRequired; // 교양 필수

    @PropertyName("교양선택")
    private int generalElective; // 교양 선택

    @PropertyName("소양")
    private int liberalArts; // 소양

    @PropertyName("자율선택")
    private int freeElective; // 자율선택

    @PropertyName("학부공통")
    private int departmentCommon; // 학부공통

    @PropertyName("전공심화")
    private int majorAdvanced; // 전공심화

    @PropertyName("잔여학점")
    private int remainingCredits; // 잔여학점

    private String majorDocRef; // 참조된 전공 문서 ID
    private String generalDocRef; // 참조된 교양 문서 ID

    public GraduationRequirement() {
        // Firestore 역직렬화를 위한 기본 생성자
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        // ID에서 학과, 트랙, 학번 추출
        // 형식: "졸업요건_IT학부_멀티미디어_2023"
        if (id != null && id.contains("_")) {
            String[] parts = id.split("_");
            if (id.startsWith("졸업요건_") && parts.length >= 4) {
                // 졸업요건 문서 형식: 졸업요건_학부_트랙_학번
                this.department = parts[1];
                this.track = parts[2];
                this.year = parts[3];
            } else if (parts.length >= 3) {
                // 기존 형식 (전공/교양 문서): 학부_트랙_학번
                this.department = parts[0];
                this.track = parts[1];
                this.year = parts[2];
            }
        }
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

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public int getTotalCredits() {
        return totalCredits;
    }

    public void setTotalCredits(int totalCredits) {
        this.totalCredits = totalCredits;
    }

    @PropertyName("전공필수")
    public int getMajorRequired() {
        return majorRequired;
    }

    @PropertyName("전공필수")
    public void setMajorRequired(int majorRequired) {
        this.majorRequired = majorRequired;
    }

    @PropertyName("전공선택")
    public int getMajorElective() {
        return majorElective;
    }

    @PropertyName("전공선택")
    public void setMajorElective(int majorElective) {
        this.majorElective = majorElective;
    }

    @PropertyName("교양필수")
    public int getGeneralRequired() {
        return generalRequired;
    }

    @PropertyName("교양필수")
    public void setGeneralRequired(int generalRequired) {
        this.generalRequired = generalRequired;
    }

    @PropertyName("교양선택")
    public int getGeneralElective() {
        return generalElective;
    }

    @PropertyName("교양선택")
    public void setGeneralElective(int generalElective) {
        this.generalElective = generalElective;
    }

    @PropertyName("소양")
    public int getLiberalArts() {
        return liberalArts;
    }

    @PropertyName("소양")
    public void setLiberalArts(int liberalArts) {
        this.liberalArts = liberalArts;
    }

    @PropertyName("자율선택")
    public int getFreeElective() {
        return freeElective;
    }

    @PropertyName("자율선택")
    public void setFreeElective(int freeElective) {
        this.freeElective = freeElective;
    }

    @PropertyName("학부공통")
    public int getDepartmentCommon() {
        return departmentCommon;
    }

    @PropertyName("학부공통")
    public void setDepartmentCommon(int departmentCommon) {
        this.departmentCommon = departmentCommon;
    }

    @PropertyName("전공심화")
    public int getMajorAdvanced() {
        return majorAdvanced;
    }

    @PropertyName("전공심화")
    public void setMajorAdvanced(int majorAdvanced) {
        this.majorAdvanced = majorAdvanced;
    }

    @PropertyName("잔여학점")
    public int getRemainingCredits() {
        return remainingCredits;
    }

    @PropertyName("잔여학점")
    public void setRemainingCredits(int remainingCredits) {
        this.remainingCredits = remainingCredits;
    }

    /**
     * 표시용 제목 생성
     */
    public String getDisplayTitle() {
        return year + "학번 - " + department;
    }

    /**
     * 트랙 표시
     */
    public String getDisplayTrack() {
        return track != null ? track : "기본";
    }

    /**
     * 전공 학점 합계 (필수 + 선택)
     */
    public int getTotalMajorCredits() {
        return majorRequired + majorElective;
    }

    /**
     * 교양 학점 합계 (필수 + 선택)
     */
    public int getTotalGeneralCredits() {
        return generalRequired + generalElective;
    }

    public String getMajorDocRef() {
        return majorDocRef;
    }

    public void setMajorDocRef(String majorDocRef) {
        this.majorDocRef = majorDocRef;
    }

    public String getGeneralDocRef() {
        return generalDocRef;
    }

    public void setGeneralDocRef(String generalDocRef) {
        this.generalDocRef = generalDocRef;
    }
}
