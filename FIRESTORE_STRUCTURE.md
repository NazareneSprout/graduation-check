# Firestore 데이터 구조 문서

## 📚 graduation_requirements 컬렉션

### 문서 ID 형식
```
{학부}_{트랙}_{입학년도}
예: IT학부_멀티미디어_2020
```

### 통합 문서 구조 (Phase 1-2 완료)

```json
{
  // 메타 정보
  "department": "IT학부",
  "track": "멀티미디어",
  "cohort": "2020",
  "version": "v2",
  "updatedAt": Timestamp,

  // 학점 요구사항 (숫자로 저장)
  "전공필수": 24,
  "전공선택": 21,
  "전공심화": 0,        // 또는 "학부공통": 36 (학부별 상이)
  "교양필수": 18,
  "교양선택": 15,
  "소양": 3,
  "일반선택": 10,      // 20-22학번
  "잔여학점": 0,        // 23-25학번

  // 🔗 문서 참조 (관리자가 설정, 선택 사항)
  "majorDocId": "IT학부_컴퓨터학_2020",           // 전공 과목을 다른 문서에서 참조
  "generalEducationDocId": "교양_IT학부_2023",   // 교양 과목을 다른 문서에서 참조

  // ⭐ 통합 졸업 규칙 (Phase 1-2 시스템)
  "rules": {
    // 전공 과목 (Array)
    "전공필수": [
      {
        "name": "일러스트레이션",
        "credits": 3,
        "category": "전공필수",
        "semester": "1학년 1학기"  // 선택 사항
      }
    ],
    "전공선택": [...],
    "전공심화": [...],  // 또는 "학부공통": [...]

    // 교양 과목 - oneOf 그룹 시스템
    "교양필수": {
      "oneOf": [
        // 그룹 1: 최소 1개 이수
        [
          {"name": "영어1", "credits": 3, "category": "교양필수", "groupId": 1},
          {"name": "영어2", "credits": 3, "category": "교양필수", "groupId": 1}
        ],
        // 그룹 2: 최소 1개 이수
        [
          {"name": "채플1", "credits": 0, "category": "교양필수", "groupId": 2},
          {"name": "채플2", "credits": 0, "category": "교양필수", "groupId": 2}
        ]
      ]
    },
    "교양선택": []  // 수동 입력이므로 빈 배열
  },

  // 대체과목 규칙
  "replacementCourses": {
    "폐지된과목명": ["대체과목1", "대체과목2"],
    "컴퓨터프로그래밍1": ["프로그래밍기초", "Python기초"]
  }
}
```

## 👤 users 컬렉션

### 문서 구조
```json
{
  // 기본 정보 (회원가입 시)
  "name": "홍길동",
  "email": "hong@example.com",
  "signUpDate": "2025-10-20 14:30:00",

  // 학적 정보 (UserInfoActivity에서 입력)
  "studentYear": "2020",
  "department": "IT학부",
  "track": "멀티미디어",
  "updatedAt": Timestamp,

  // 졸업검사 정보
  "lastGraduationCheckDate": Timestamp
}
```

### 서브컬렉션: graduation_check_history
```
users/{userId}/graduation_check_history/{docId}

{
  "checkedAt": Timestamp,
  "year": "2020",
  "department": "IT학부",
  "track": "멀티미디어",

  "courses": [
    {
      "name": "컴퓨터프로그래밍",
      "credits": 3,
      "category": "전공필수",
      "groupId": 1,           // 선택 사항
      "competency": "1역량"   // 교양선택만
    }
  ],

  "additionalRequirements": {
    "tlcCount": 8,
    "chapelCount": 4,
    "mileageCompleted": true,
    "extraGradCompleted": false
  }
}
```

---

## 🏫 department_configs 컬렉션

### 문서 구조
```json
{
  "usesMajorAdvanced": true  // true: 전공심화 사용, false: 학부공통 사용
}
```

**문서 ID 예시:**
- `IT학부`
- `체육학과`

---

## 📖 중요 구조 설명

### 1. rules 필드 계층 구조 (통합 시스템)

```
rules (Map)
  ├── 전공필수 (Array of Course Objects)
  ├── 전공선택 (Array of Course Objects)
  ├── 전공심화 (Array of Course Objects) 또는 학부공통
  ├── 교양필수 (Object with oneOf)
  │   └── oneOf (Array of Arrays)
  │       └── Group (Array of Course Objects)
  └── 교양선택 (Empty Array - 수동 입력)
```

### 2. 전공 과목 접근 예제 (Java)

```java
// 1. 문서 가져오기
DocumentSnapshot doc = ...; // from Firestore

// 2. rules 필드 접근
Map<String, Object> rules = (Map<String, Object>) doc.get("rules");

// 3. 전공필수 과목 접근
List<Map<String, Object>> majorRequired = (List<Map<String, Object>>) rules.get("전공필수");

// 4. 개별 과목 접근
for (Map<String, Object> course : majorRequired) {
    String name = (String) course.get("name");
    Long credits = (Long) course.get("credits");
    String category = (String) course.get("category");
    String semester = (String) course.get("semester");  // null 가능
}
```

### 3. 교양필수 oneOf 그룹 접근 예제

```java
// 1. 교양필수 가져오기
Map<String, Object> generalRequired = (Map<String, Object>) rules.get("교양필수");

// 2. oneOf 배열 가져오기
List<List<Map<String, Object>>> oneOfGroups =
    (List<List<Map<String, Object>>>) generalRequired.get("oneOf");

// 3. 각 그룹 순회
for (int groupIndex = 0; groupIndex < oneOfGroups.size(); groupIndex++) {
    List<Map<String, Object>> group = oneOfGroups.get(groupIndex);

    // 4. 그룹 내 과목 순회
    for (Map<String, Object> course : group) {
        String name = (String) course.get("name");
        Long groupId = (Long) course.get("groupId");  // 그룹 번호
    }
}
```

### 4. 문서 참조 (majorDocId, generalEducationDocId) 활용

```java
// 1. 참조 문서 ID 확인
String majorDocId = doc.getString("majorDocId");
String generalDocId = doc.getString("generalEducationDocId");

// 2. 참조 문서가 있으면 해당 문서에서 과목 로드
if (majorDocId != null && !majorDocId.isEmpty()) {
    // 다른 문서의 전공 과목 사용
    DocumentSnapshot refDoc = db.collection("graduation_requirements")
        .document(majorDocId)
        .get()
        .await();

    Map<String, Object> refRules = (Map<String, Object>) refDoc.get("rules");
    List<Map<String, Object>> majorCourses =
        (List<Map<String, Object>>) refRules.get("전공필수");
}
```

### 5. Firestore 업데이트 예제

```java
// 전공필수 과목 업데이트
Map<String, Object> updates = new HashMap<>();
updates.put("rules.전공필수", newCourseList);

db.collection("graduation_requirements")
  .document("IT학부_멀티미디어_2020")
  .update(updates);

// 문서 참조 설정
updates.put("majorDocId", "IT학부_컴퓨터학_2020");
updates.put("generalEducationDocId", "교양_IT학부_2023");
db.collection("graduation_requirements")
  .document("IT학부_멀티미디어_2020")
  .update(updates);
```

## 카테고리 종류

### 전공 관련
- `전공필수`: 전공 필수 과목
- `전공선택`: 전공 선택 과목
- `전공심화`: 전공 심화 과목

### 교양 관련
- `교양필수`: 교양 필수 과목
- `교양선택`: 교양 선택 과목
- `소양`: 소양 과목

### 학부 공통
- `학부공통`: IT학부 공통 과목 (구 "학부공통필수" 포함)

### 기타
- `자율선택`: 자유 선택 과목
- `잔여학점`: 나머지 학점

## 주의사항

1. **타입 확인 필수**: Firestore에서 가져온 데이터는 항상 타입 체크 후 캐스팅해야 합니다.
2. **Null 체크**: 특정 학기나 카테고리가 없을 수 있으므로 null 체크가 필요합니다.
3. **학기 이름**: "1학년 1학기", "2학년 1학기" 등 고정된 형식을 사용합니다.
4. **카테고리 일관성**: 과목 객체의 `category` 필드는 상위 카테고리 키와 일치해야 합니다.

---

## 📊 데이터 마이그레이션 히스토리

### 2025-10-20: Phase 1-2 완료 - 통합 졸업요건 시스템 구축
- **변경**: 학기별 구조 → 카테고리별 배열 구조로 전환
- **추가**: majorDocId, generalEducationDocId 문서 참조 시스템
- **추가**: 교양필수 oneOf 그룹 시스템
- **추가**: 대체과목 replacementCourses 통합
- **영향 범위**: 전체 graduation_requirements 컬렉션

### 2025-10-20: 학부공통필수 → 학부공통 일괄 병합
- **문제**: "학부공통필수"와 "학부공통" 카테고리 중복으로 학점 계산 오류
- **해결**: 모든 graduation_requirements 문서에서 "학부공통필수" → "학부공통" 병합
- **방법**: AdminActivity의 bulk 변환 기능 사용 (완료 후 제거)
- **영향 범위**: 전체 graduation_requirements 컬렉션

---

**📝 마지막 업데이트**: 2025년 10월 20일
