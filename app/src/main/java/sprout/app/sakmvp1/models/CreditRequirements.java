package sprout.app.sakmvp1.models;

/**
 * 졸업에 필요한 카테고리별 학점 요건
 * Firestore의 creditRequirements 필드와 매핑됨
 */
public class CreditRequirements {
    private int total;
    private int 전공필수;
    private int 전공선택;
    private int 교양필수;
    private int 교양선택;
    private int 소양;
    private int 학부공통;
    private int 일반선택;
    private int 전공심화;
    private int 잔여학점;

    // Firestore 역직렬화를 위한 빈 생성자
    public CreditRequirements() {
    }

    // Getters and Setters
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int get전공필수() {
        return 전공필수;
    }

    public void set전공필수(int 전공필수) {
        this.전공필수 = 전공필수;
    }

    public int get전공선택() {
        return 전공선택;
    }

    public void set전공선택(int 전공선택) {
        this.전공선택 = 전공선택;
    }

    public int get교양필수() {
        return 교양필수;
    }

    public void set교양필수(int 교양필수) {
        this.교양필수 = 교양필수;
    }

    public int get교양선택() {
        return 교양선택;
    }

    public void set교양선택(int 교양선택) {
        this.교양선택 = 교양선택;
    }

    public int get소양() {
        return 소양;
    }

    public void set소양(int 소양) {
        this.소양 = 소양;
    }

    public int get학부공통() {
        return 학부공통;
    }

    public void set학부공통(int 학부공통) {
        this.학부공통 = 학부공통;
    }

    public int get일반선택() {
        return 일반선택;
    }

    public void set일반선택(int 일반선택) {
        this.일반선택 = 일반선택;
    }

    public int get전공심화() {
        return 전공심화;
    }

    public void set전공심화(int 전공심화) {
        this.전공심화 = 전공심화;
    }

    public int get잔여학점() {
        return 잔여학점;
    }

    public void set잔여학점(int 잔여학점) {
        this.잔여학점 = 잔여학점;
    }

    /**
     * 카테고리 이름으로 필요한 학점 조회
     * @param categoryName 카테고리 이름 (예: "전공필수", "교양선택")
     * @return 해당 카테고리의 필요 학점, 없으면 0
     */
    public int getRequiredCredits(String categoryName) {
        if (categoryName == null) {
            return 0;
        }

        switch (categoryName) {
            case "전공필수":
                return 전공필수;
            case "전공선택":
                return 전공선택;
            case "교양필수":
                return 교양필수;
            case "교양선택":
                return 교양선택;
            case "소양":
                return 소양;
            case "학부공통":
                return 학부공통;
            case "일반선택":
                return 일반선택;
            case "전공심화":
                return 전공심화;
            case "잔여학점":
                return 잔여학점;
            default:
                return 0;
        }
    }

    /**
     * 카테고리별 학점 설정
     * @param categoryName 카테고리 이름
     * @param credits 학점
     */
    public void setRequiredCredits(String categoryName, int credits) {
        if (categoryName == null) {
            return;
        }

        switch (categoryName) {
            case "전공필수":
                전공필수 = credits;
                break;
            case "전공선택":
                전공선택 = credits;
                break;
            case "교양필수":
                교양필수 = credits;
                break;
            case "교양선택":
                교양선택 = credits;
                break;
            case "소양":
                소양 = credits;
                break;
            case "학부공통":
                학부공통 = credits;
                break;
            case "일반선택":
                일반선택 = credits;
                break;
            case "전공심화":
                전공심화 = credits;
                break;
            case "잔여학점":
                잔여학점 = credits;
                break;
        }
    }

    @Override
    public String toString() {
        return "CreditRequirements{" +
                "total=" + total +
                ", 전공필수=" + 전공필수 +
                ", 전공선택=" + 전공선택 +
                ", 교양필수=" + 교양필수 +
                ", 교양선택=" + 교양선택 +
                ", 소양=" + 소양 +
                ", 학부공통=" + 학부공통 +
                ", 일반선택=" + 일반선택 +
                ", 전공심화=" + 전공심화 +
                ", 잔여학점=" + 잔여학점 +
                '}';
    }
}
