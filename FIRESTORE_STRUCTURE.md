# Firestore 데이터 구조 문서

## graduation_requirements 컬렉션

각 문서는 `{학부}_{전공}_{입학년도}` 형식의 ID를 가집니다.
예: `IT학부_인공지능_2020`

### 문서 구조

```json
{
  // 메타 정보
  "department": "IT학부",
  "track": "인공지능",
  "cohort": "2020",
  "version": "v2",
  "generalEducationDocId": "교양_2020",
  "updatedAt": "2024-01-15T10:30:00Z",

  // 총 이수 학점
  "totalCredits": {
    "required": 130,
    "current": 0
  },

  // 최상위 카테고리별 학점 요구사항 (레거시, 참고용)
  "전공필수": { "required": 36, "current": 0 },
  "전공선택": { "required": 24, "current": 0 },
  "전공심화": { "required": 12, "current": 0 },
  "교양필수": { "required": 15, "current": 0 },
  "교양선택": { "required": 6, "current": 0 },
  "학부공통": { "required": 36, "current": 0 },
  "소양": { "required": 3, "current": 0 },
  "자율선택": { "required": 0, "current": 0 },
  "잔여학점": { "required": 0, "current": 0 },

  // ⭐ 핵심: rules 필드 구조
  "rules": {
    // 각 학기별 Map 구조
    "1학년 1학기": {
      // 카테고리명: 과목 리스트
      "전공필수": [
        {
          "name": "컴퓨터프로그래밍",
          "code": "CS101",
          "credits": 3,
          "category": "전공필수"
        },
        {
          "name": "이산수학",
          "code": "CS102",
          "credits": 3,
          "category": "전공필수"
        }
      ],
      "학부공통": [
        {
          "name": "IT개론",
          "code": "IT100",
          "credits": 3,
          "category": "학부공통"
        }
      ],
      "교양필수": [
        {
          "name": "영어회화",
          "code": "ENG101",
          "credits": 3,
          "category": "교양필수"
        }
      ]
    },

    "1학년 2학기": {
      "전공필수": [...],
      "전공선택": [...],
      "학부공통": [...]
    },

    "2학년 1학기": { ... },
    "2학년 2학기": { ... },
    "3학년 1학기": { ... },
    "3학년 2학기": { ... },
    "4학년 1학기": { ... },
    "4학년 2학기": { ... }
  }
}
```

## 중요 구조 설명

### 1. rules 필드의 계층 구조

```
rules (Map)
  └── 학기명 (String, 예: "1학년 1학기") (Map)
       └── 카테고리명 (String, 예: "전공필수", "학부공통") (List)
            └── 과목 객체 (Map)
                 ├── name: String
                 ├── code: String
                 ├── credits: Number
                 └── category: String
```

### 2. 데이터 접근 예제 (Java)

```java
// 1. 문서 전체 가져오기
Map<String, Object> data = documentSnapshot.getData();

// 2. rules 필드 접근
Map<String, Object> rules = (Map<String, Object>) data.get("rules");

// 3. 특정 학기 접근
Map<String, Object> semester1_1 = (Map<String, Object>) rules.get("1학년 1학기");

// 4. 특정 학기의 카테고리별 과목 리스트 접근
List<Map<String, Object>> 전공필수과목들 = (List<Map<String, Object>>) semester1_1.get("전공필수");

// 5. 개별 과목 접근
for (Map<String, Object> course : 전공필수과목들) {
    String name = (String) course.get("name");
    String code = (String) course.get("code");
    Number credits = (Number) course.get("credits");
    String category = (String) course.get("category");
}
```

### 3. 모든 학기 순회 예제

```java
Map<String, Object> rules = (Map<String, Object>) data.get("rules");

for (String semesterName : rules.keySet()) {
    Object semesterObj = rules.get(semesterName);

    if (semesterObj instanceof Map) {
        Map<String, Object> semesterMap = (Map<String, Object>) semesterObj;

        // 각 학기의 모든 카테고리 순회
        for (String categoryName : semesterMap.keySet()) {
            Object categoryObj = semesterMap.get(categoryName);

            if (categoryObj instanceof List) {
                List<Map<String, Object>> courses = (List<Map<String, Object>>) categoryObj;

                // 각 과목 처리
                for (Map<String, Object> course : courses) {
                    // course 처리
                }
            }
        }
    }
}
```

### 4. Firestore 업데이트 예제

```java
// 특정 학기의 특정 카테고리 업데이트
Map<String, Object> updates = new HashMap<>();
updates.put("rules.1학년 1학기.전공필수", newCourseList);

db.collection("graduation_requirements")
  .document("IT학부_인공지능_2020")
  .update(updates);

// 특정 카테고리 삭제
updates.put("rules.1학년 1학기.학부공통필수", FieldValue.delete());
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

## 데이터 마이그레이션 히스토리

### 2024-10-19: 학부공통필수 → 학부공통 병합
- **문제**: "학부공통필수"와 "학부공통" 카테고리가 분리되어 있어 학점 계산 오류 발생
- **해결**: AdminActivity의 fixDepartmentCommonCategory() 메서드로 "학부공통필수" 카테고리의 모든 과목을 "학부공통"으로 병합
- **영향 범위**: IT학부_인공지능_2020 문서
