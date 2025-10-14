# 졸업요건 관리 시스템 초기화 문서

## Firestore 데이터 구조

### Collection: `graduation_requirements`

#### 문서 ID 형식
```
{학과}_{트랙}_{학번}
예: "IT학부_멀티미디어_2020"
```

#### 문서 필드 구조
```javascript
{
  // 메타데이터
  "department": "IT학부",
  "track": "멀티미디어",
  "cohort": 2020,
  "version": "v1.0",
  "updatedAt": Timestamp,

  // 학점 요구사항 (숫자만 저장)
  "전공필수": 24,
  "전공선택": 21,
  "전공심화": 0,
  "학부공통": 36,
  "교양필수": 18,
  "교양선택": 15,
  "소양": 3,
  "자율선택": 10,
  "잔여학점": 0,  // 127 - (모든 필드 합계)

  // 실제 과목 데이터 (학기별로 저장)
  "rules": {
    "1학년 1학기": {
      "학부공통필수": [
        {"과목명": "IT개론", "학점": 3},
        {"과목명": "컴퓨터프로그래밍1", "학점": 3}
      ],
      "전공필수": [
        {"과목명": "일러스트레이션", "학점": 3}
      ],
      "전공선택": [
        {"과목명": "대학수학", "학점": 3}
      ]
    },
    "1학년 2학기": {
      // ... 동일한 구조
    },
    // ... 8개 학기
  }
}
```

**중요 포인트:**
- `전공필수`, `전공선택` 등의 필드는 **학점 숫자(Long)**만 저장
- **실제 과목 목록**은 `rules` 필드 안에 **학기별**로 저장
- 과목 데이터는 `{"과목명": "...", "학점": ...}` 형식의 Map

## 관련 Activity 및 기능

### 1. GraduationRequirementsActivity (목록 조회)
**경로:** `app/src/main/java/sprout/app/sakmvp1/GraduationRequirementsActivity.java`

**기능:**
- 학번, 학과, 트랙을 스피너로 선택
- Firestore에서 해당하는 문서 조회
- 학점 요구사항을 카드 형태로 표시
- 조건부 필드 표시:
  - `전공심화` 또는 `학부공통` 중 0이 아닌 것만 표시
  - `자율선택` 또는 `잔여학점` 중 0이 아닌 것만 표시

**주요 로직:**
```java
// 직접 필드 읽기
Long majorRequired = document.getLong("전공필수");
Long majorElective = document.getLong("전공선택");

// 총 이수학점 계산
int totalCredits = sumOfAllFields + remainingCredits; // 항상 127
```

### 2. GraduationRequirementDetailActivity (상세 조회)
**경로:** `app/src/main/java/sprout/app/sakmvp1/GraduationRequirementDetailActivity.java`

**기능:**
- 선택한 졸업요건의 상세 정보 표시
- 학점 요구사항 상세 표시
- 카테고리별 과목 목록 표시
- FAB 버튼으로 편집 화면 이동

**과목 조회 로직:**
```java
private void addCourseCategory(DocumentSnapshot document, String categoryName, String colorHex) {
    // "전공필수Courses" → "전공필수courses" → "전공필수" 순서로 시도
    Object coursesObj = document.get(categoryName + "Courses");
    if (coursesObj == null) {
        coursesObj = document.get(categoryName + "courses");
    }
    if (coursesObj == null) {
        coursesObj = document.get(categoryName);
    }

    if (coursesObj instanceof List) {
        // 과목 목록 표시
    }
}
```

### 3. GraduationRequirementEditActivity (편집)
**경로:** `app/src/main/java/sprout/app/sakmvp1/GraduationRequirementEditActivity.java`

**기능:**
- 관리자용 과목 편집 화면
- 5개 카테고리만 표시: **학부공통필수, 전공필수, 전공선택, 교양필수, 소양**
- 과목 추가/수정/삭제
- Firestore에 변경사항 저장

**중요 구현 사항:**

#### 1) 과목 로드 (rules 구조에서 추출)
```java
private void displayEditableContent(DocumentSnapshot document) {
    // rules 필드 가져오기
    Object rulesObj = document.get("rules");
    if (rulesObj instanceof Map) {
        Map<String, Object> rules = (Map<String, Object>) rulesObj;

        // 5개 카테고리만 표시
        addEditableCourseCategory(rules, "학부공통필수", "#7B1FA2");
        addEditableCourseCategory(rules, "전공필수", "#1976D2");
        addEditableCourseCategory(rules, "전공선택", "#1976D2");
        addEditableCourseCategory(rules, "교양필수", "#F57C00");
        addEditableCourseCategory(rules, "소양", "#F57C00");
    }
}

private void addEditableCourseCategory(Map<String, Object> rules, String categoryName, String colorHex) {
    List<Map<String, Object>> coursesList = new ArrayList<>();

    // 모든 학기를 순회하면서 해당 카테고리의 과목 수집
    for (Map.Entry<String, Object> semesterEntry : rules.entrySet()) {
        String semester = semesterEntry.getKey(); // "1학년 1학기"
        Map<String, Object> semesterData = (Map<String, Object>) semesterEntry.getValue();

        Object categoryCoursesObj = semesterData.get(categoryName);
        if (categoryCoursesObj instanceof List) {
            List<Object> categoryCourses = (List<Object>) categoryCoursesObj;

            for (Object courseObj : categoryCourses) {
                Map<String, Object> course = (Map<String, Object>) courseObj;

                // 과목명, 학점을 name, credit으로 변환
                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("name", course.get("과목명"));
                courseMap.put("credit", course.get("학점"));
                courseMap.put("semester", semester); // 학기 정보 보존
                coursesList.add(courseMap);
            }
        }
    }

    // 편집 중인 데이터에 저장
    editingCourses.put(categoryName, coursesList);
}
```

#### 2) 과목 추가
```java
private void showAddCourseDialog(String categoryName, String colorHex) {
    // 1. 학기 선택
    final String[] semesters = {"1학년 1학기", "1학년 2학기", ...};

    new AlertDialog.Builder(this)
        .setTitle("학기 선택")
        .setItems(semesters, (dialog, which) -> {
            String selectedSemester = semesters[which];
            // 2. 과목명/학점 입력
            showCourseInputDialog(categoryName, selectedSemester, null, -1);
        })
        .show();
}
```

#### 3) 저장 (rules 구조로 다시 변환)
```java
private void saveChanges() {
    // 기존 rules 가져오기
    Map<String, Object> rules = (Map<String, Object>) currentDocument.get("rules");

    // 편집된 과목들을 학기별로 다시 분류
    for (Map.Entry<String, List<Map<String, Object>>> entry : editingCourses.entrySet()) {
        String categoryName = entry.getKey();
        List<Map<String, Object>> courses = entry.getValue();

        // 학기별로 과목 분류
        Map<String, List<Map<String, Object>>> coursesBySemester = new HashMap<>();

        for (Map<String, Object> course : courses) {
            String semester = (String) course.get("semester");

            if (!coursesBySemester.containsKey(semester)) {
                coursesBySemester.put(semester, new ArrayList<>());
            }

            // name, credit → 과목명, 학점으로 변환
            Map<String, Object> firestoreCourse = new HashMap<>();
            firestoreCourse.put("과목명", course.get("name"));
            firestoreCourse.put("학점", course.get("credit"));

            coursesBySemester.get(semester).add(firestoreCourse);
        }

        // rules에 업데이트
        for (Map.Entry<String, List<Map<String, Object>>> semesterEntry : coursesBySemester.entrySet()) {
            String semester = semesterEntry.getKey();
            List<Map<String, Object>> semesterCourses = semesterEntry.getValue();

            Map<String, Object> semesterData = (Map<String, Object>) rules.get(semester);
            semesterData.put(categoryName, semesterCourses);
        }
    }

    // Firestore에 저장
    db.collection("graduation_requirements")
        .document(documentId)
        .update("rules", rules);
}
```

## 데이터 흐름

### 조회 흐름
```
1. 사용자가 학번/학과/트랙 선택
2. Firestore 쿼리: graduation_requirements/{학과}_{트랙}_{학번}
3. 문서에서 필드 직접 읽기:
   - 학점: document.getLong("전공필수") → Long 타입
   - 과목: document.get("rules") → Map 타입
4. 화면에 표시
```

### 편집 흐름
```
1. DetailActivity에서 FAB 버튼 클릭
2. EditActivity로 documentId 전달
3. rules 필드에서 학기별 과목 추출
   - 학기 순회 → 카테고리별 과목 수집
4. 메모리에서 편집 (editingCourses Map)
5. 저장 버튼 클릭
6. 편집된 과목을 다시 학기별로 분류
7. rules 필드 업데이트
8. Firestore에 저장
```

## 주의사항

### 1. 필드명 혼동 방지
- **학점 필드**: `전공필수`, `전공선택` (Long 타입)
- **과목 필드**: `rules` → 학기 → `전공필수`, `전공선택` (List<Map> 타입)

### 2. 카테고리 이름 불일치
- DetailActivity: `전공필수` (과목 조회 시 "Courses" 접미사 시도)
- EditActivity: `학부공통필수` (rules 내부의 실제 필드명)
- **차이점**: EditActivity는 `학부공통` 대신 `학부공통필수` 사용

### 3. 데이터 변환
- Firestore → 앱: `{"과목명": "...", "학점": ...}` → `{"name": "...", "credit": ..., "semester": "..."}`
- 앱 → Firestore: `{"name": "...", "credit": ...}` → `{"과목명": "...", "학점": ...}`

### 4. 표시 카테고리
- **리스트/상세 화면**: 모든 카테고리 (조건부 표시)
- **편집 화면**: 5개만 (학부공통필수, 전공필수, 전공선택, 교양필수, 소양)

## 레이아웃 파일

### 1. activity_graduation_requirements.xml
- 스피너 3개 (학번, 학과, 트랙)
- 조회 버튼
- RecyclerView (결과 목록)

### 2. activity_graduation_requirement_detail.xml
- NestedScrollView (스크롤 가능한 상세 정보)
- 학점 카드들
- 과목 목록 컨테이너
- FAB (편집 버튼)

### 3. activity_graduation_requirement_edit.xml
- NestedScrollView (스크롤 가능한 편집 화면)
- 과목 목록 컨테이너 (동적 생성)
- 저장 버튼 (하단 고정)

### 4. dialog_course_input.xml
- TextInputLayout (과목명)
- TextInputLayout (학점)

## AndroidManifest.xml 등록

```xml
<activity android:name=".GraduationRequirementsActivity"
    android:theme="@style/Theme.SakMvp1"
    android:exported="false" />

<activity android:name=".GraduationRequirementDetailActivity"
    android:theme="@style/Theme.SakMvp1"
    android:exported="false" />

<activity android:name=".GraduationRequirementEditActivity"
    android:theme="@style/Theme.SakMvp1"
    android:exported="false" />
```

## 트러블슈팅

### 문제 1: 과목이 조회되지 않음
**원인:** `전공필수` 필드가 학점 숫자(Long)를 반환
**해결:** `rules` 필드에서 학기별로 순회하여 과목 추출

### 문제 2: 과목 저장 후 사라짐
**원인:** 학기 정보 누락
**해결:** 과목 데이터에 `semester` 필드 추가하여 보존

### 문제 3: 카테고리가 너무 많이 표시됨
**원인:** DetailActivity와 동일하게 모든 카테고리 표시
**해결:** EditActivity는 5개 카테고리만 표시하도록 제한

## 참고 문서
- Firestore 한글 필드명 사용: `@PropertyName("한글명")`
- Direct field access: `document.get("필드명")` vs `document.toObject(Class)`
- 조건부 View 표시: `View.VISIBLE` / `View.GONE`
- 총 이수학점: 항상 127학점 (모든 필드 합계 + 잔여학점)

---

**작성일:** 2025-10-12
**최종 수정:** 졸업요건 편집 기능 완성 후
