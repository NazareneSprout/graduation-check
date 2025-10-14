# 졸업요건 시스템 완전 리팩토링 계획

## 📋 목차
1. [프로젝트 개요](#프로젝트-개요)
2. [현재 문제점](#현재-문제점)
3. [목표 아키텍처](#목표-아키텍처)
4. [세부 실행 계획](#세부-실행-계획)
5. [일정 및 마일스톤](#일정-및-마일스톤)
6. [위험 요소 및 대응](#위험-요소-및-대응)

---

## 프로젝트 개요

### 프로젝트 명
졸업요건 시스템 통합 리팩토링 (Unified Graduation Requirements System)

### 목적
- **하드코딩 완전 제거**: 모든 졸업요건 로직을 Firebase 데이터 기반으로 전환
- **데이터 통합**: 분산된 컬렉션을 단일 스키마로 통합
- **관리자-사용자 연동**: 관리자가 수정한 내용이 실시간으로 사용자 분석에 반영
- **유지보수성 향상**: 새 학번/학과 추가 시 코드 수정 없이 데이터만 추가

### 예상 기간
**3-4주** (총 15-20 작업일)

### 핵심 산출물
1. `graduation_requirements_v2` Firestore 컬렉션
2. 통합 Java 모델 클래스 (`models/` 패키지)
3. 리팩토링된 `GraduationAnalysisResultActivity`
4. 통합 관리자 편집 UI
5. 데이터 마이그레이션 스크립트
6. 테스트 시나리오 및 검증 문서

---

## 현재 문제점

### 1. 데이터 구조 분산
현재 4개의 독립적인 Firestore 컬렉션이 존재:
- `graduation_requirements`: 학점 요건만 저장
- `major_documents`: 전공 과목 정보
- `education_documents`: 교양 과목 정보
- `replacement_courses`: 대체과목 규칙

**문제점:**
- 단일 졸업요건 조회를 위해 최소 4번의 Firebase 쿼리 필요
- 데이터 일관성 보장 어려움 (트랜잭션 불가)
- 관리자가 여러 화면에서 각각 편집해야 함

### 2. 하드코딩된 로직

#### 2.1 교양필수 oneOf 그룹
**위치:** `GraduationAnalysisResultActivity.java` 라인 800-950
```java
// 하드코딩 예시
if (courseName.equals("생애설계와직업진로탐색") ||
    courseName.equals("생애설계와직업진로1") ||
    courseName.equals("자기주도취업과창업")) {
    // 생애설계와진로 그룹으로 인정
}
```

**문제점:**
- 새로운 대체 과목 추가 시 코드 수정 필요
- 배포 없이는 변경 불가능
- 테스트 어려움 (매번 앱 재빌드 필요)

#### 2.2 학기별 과목 배치
**위치:** `GraduationAnalysisResultActivity.java` 라인 1100-1300
```java
// 하드코딩 예시
if (courseName.equals("이산구조")) {
    semester = "1-2";
}
```

**문제점:**
- 과목의 학기 정보가 코드에 박혀있음
- 교과과정 변경 시 앱 업데이트 필수

#### 2.3 카테고리 판단 로직
**위치:** `FirebaseDataManager.java`, `GraduationAnalysisResultActivity.java`
```java
// 분산된 카테고리 판단 로직
if (department.equals("IT학부")) {
    if (track.equals("인공지능")) {
        // 전공필수 목록 하드코딩
    }
}
```

**문제점:**
- 학과/트랙별 분기가 코드 전체에 산재
- 새 트랙 추가 시 다수 파일 수정 필요

### 3. 데이터-로직 분리 부재

**현재 상황:**
- 관리자가 Firestore에서 과목을 추가/삭제해도
- 앱의 분석 로직은 여전히 하드코딩된 규칙 사용
- 예: oneOf 그룹, 필수과목 여부, 학기 배치 등

**구체적 예시:**
1. 관리자가 "기독교" 그룹에 새 과목 "기독교와현대사회" 추가
2. Firestore `education_documents`에는 저장됨
3. 하지만 `analyzeGeneralEducationCourses()`는 여전히 5개 과목만 인식
4. 사용자 졸업요건 분석에서 누락

### 4. 통합 상태 평가

| 기능 | 통합 수준 | 문제점 |
|------|-----------|--------|
| 대체과목 | ✅ 완전 통합 | Firebase 데이터만으로 동작 |
| 전공 과목 목록 | ⚠️ 부분 통합 | 목록은 Firebase, 하지만 학기/필수 여부는 하드코딩 |
| 교양 과목 목록 | ⚠️ 부분 통합 | 목록은 Firebase, 하지만 oneOf 그룹은 하드코딩 |
| 학점 요건 | ✅ 완전 통합 | Firebase creditRequirements로 관리 |
| oneOf 그룹 | ❌ 완전 하드코딩 | 코드에만 존재 |
| 학기 배치 | ❌ 완전 하드코딩 | 코드에만 존재 |
| 필수 과목 플래그 | ❌ 완전 하드코딩 | 코드에만 존재 |

---

## 목표 아키텍처

### 핵심 원칙

1. **Single Source of Truth**: 모든 졸업요건 정보는 Firestore `graduation_requirements_v2`에만 존재
2. **Data-Driven Logic**: 앱 로직은 데이터 구조를 따르며, 데이터가 변경되면 로직도 자동 적응
3. **Zero Hardcoding**: 학과/트랙/학번에 특화된 분기문 제거
4. **Real-time Sync**: 관리자 변경사항이 즉시 사용자에게 반영

### 통합 Firestore 스키마

#### 컬렉션: `graduation_requirements_v2`

**문서 ID 규칙:** `{cohort}_{department}_{track}`
- 예: `2020_IT학부_인공지능`

**문서 구조:**
```json
{
  "docId": "2020_IT학부_인공지능",
  "cohort": "2020",
  "department": "IT학부",
  "track": "인공지능",
  "version": "v2.0",
  "updatedAt": "2025-10-14T10:00:00Z",

  "creditRequirements": {
    "total": 127,
    "전공필수": 24,
    "전공선택": 21,
    "교양필수": 18,
    "교양선택": 15,
    "소양": 3,
    "학부공통": 36,
    "일반선택": 10
  },

  "overflowDestination": "일반선택",

  "categories": [
    {
      "id": "major_required",
      "name": "전공필수",
      "displayName": "전공필수",
      "type": "list",
      "required": 24,
      "requiredType": "credits",
      "courses": [
        {
          "name": "이산구조",
          "credits": 3,
          "semester": "1-2",
          "mandatory": true
        }
      ]
    },
    {
      "id": "general_required",
      "name": "교양필수",
      "displayName": "교양필수",
      "type": "group",
      "required": 18,
      "requiredType": "credits",
      "subgroups": [
        {
          "id": "christianity",
          "name": "기독교",
          "type": "oneOf",
          "required": 2,
          "requiredType": "credits",
          "courses": [
            {"name": "성서와인간", "credits": 2},
            {"name": "하나님과세상", "credits": 2}
          ]
        }
      ]
    }
  ],

  "replacementRules": [
    {
      "discontinuedCourse": {
        "name": "IT개론",
        "category": "학부공통",
        "credits": 3
      },
      "replacementCourses": [
        {
          "name": "JAVA프레임워크",
          "category": "전공선택",
          "credits": 3
        }
      ],
      "note": "2023년부터 폐강",
      "createdAt": "2025-01-15T10:00:00Z"
    }
  ]
}
```

### Java 모델 클래스 구조

#### 1. GraduationRules.java
```java
package sprout.app.sakmvp1.models;

public class GraduationRules {
    private String docId;
    private String cohort;
    private String department;
    private String track;
    private String version;
    private Timestamp updatedAt;

    private CreditRequirements creditRequirements;
    private String overflowDestination;
    private List<RequirementCategory> categories;
    private List<ReplacementRule> replacementRules;

    // 졸업요건 분석 메서드
    public GraduationAnalysisResult analyze(List<Course> takenCourses) {
        GraduationAnalysisResult result = new GraduationAnalysisResult();

        // 각 카테고리 분석
        for (RequirementCategory category : categories) {
            CategoryAnalysisResult categoryResult = category.analyze(takenCourses);
            result.addCategoryResult(categoryResult);
        }

        // 대체과목 적용
        applyReplacementRules(result, takenCourses);

        // 넘치는 학점 처리
        handleOverflowCredits(result);

        return result;
    }
}
```

#### 2. RequirementCategory.java
```java
package sprout.app.sakmvp1.models;

public class RequirementCategory {
    private String id;
    private String name;
    private String displayName;
    private String type;  // "list", "oneOf", "group", "competency"
    private int required;
    private String requiredType;  // "credits", "courses", "any"

    private List<CourseRequirement> courses;
    private List<RequirementCategory> subgroups;
    private List<String> competencies;

    /**
     * 수강한 과목을 기반으로 이 카테고리의 충족 여부 분석
     */
    public CategoryAnalysisResult analyze(List<Course> takenCourses) {
        CategoryAnalysisResult result = new CategoryAnalysisResult(id, name);

        switch (type) {
            case "list":
                return analyzeList(takenCourses);
            case "oneOf":
                return analyzeOneOf(takenCourses);
            case "group":
                return analyzeGroup(takenCourses);
            case "competency":
                return analyzeCompetency(takenCourses);
            default:
                return result;
        }
    }

    private CategoryAnalysisResult analyzeList(List<Course> takenCourses) {
        // 목록의 모든 과목을 체크
        // mandatory=true인 과목은 필수
    }

    private CategoryAnalysisResult analyzeOneOf(List<Course> takenCourses) {
        // 목록 중 하나만 수강하면 충족
    }

    private CategoryAnalysisResult analyzeGroup(List<Course> takenCourses) {
        // 하위 subgroups를 재귀적으로 분석
        for (RequirementCategory subgroup : subgroups) {
            CategoryAnalysisResult subResult = subgroup.analyze(takenCourses);
            result.addSubgroupResult(subResult);
        }
    }
}
```

#### 3. CreditRequirements.java
```java
package sprout.app.sakmvp1.models;

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

    // Getters and Setters

    public int getRequiredCredits(String categoryName) {
        switch (categoryName) {
            case "전공필수": return 전공필수;
            case "전공선택": return 전공선택;
            case "교양필수": return 교양필수;
            case "교양선택": return 교양선택;
            case "소양": return 소양;
            case "학부공통": return 학부공통;
            case "일반선택": return 일반선택;
            case "전공심화": return 전공심화;
            case "잔여학점": return 잔여학점;
            default: return 0;
        }
    }
}
```

#### 4. ReplacementRule.java
```java
package sprout.app.sakmvp1.models;

public class ReplacementRule {
    private CourseInfo discontinuedCourse;
    private List<CourseInfo> replacementCourses;
    private String note;
    private Timestamp createdAt;

    public static class CourseInfo {
        private String name;
        private String category;
        private int credits;

        // Getters and Setters
    }

    /**
     * 수강 과목 목록에서 대체과목 규칙 적용 가능 여부 확인
     */
    public boolean canApply(List<String> takenCourseNames) {
        // 폐강된 과목을 직접 듣지 않았고
        if (takenCourseNames.contains(discontinuedCourse.getName())) {
            return false;
        }

        // 대체 과목 중 하나라도 수강했으면 적용 가능
        for (CourseInfo replacement : replacementCourses) {
            if (takenCourseNames.contains(replacement.getName())) {
                return true;
            }
        }

        return false;
    }
}
```

#### 5. GraduationAnalysisResult.java
```java
package sprout.app.sakmvp1.models;

public class GraduationAnalysisResult {
    private int totalEarnedCredits;
    private int totalRequiredCredits;
    private boolean isGraduationReady;

    private Map<String, CategoryAnalysisResult> categoryResults;
    private List<String> warnings;
    private List<String> recommendations;

    public void addCategoryResult(CategoryAnalysisResult result) {
        categoryResults.put(result.getCategoryId(), result);
        totalEarnedCredits += result.getEarnedCredits();
    }

    public void calculateGraduationReadiness() {
        // 모든 카테고리가 충족되었는지 확인
        boolean allCategoriesComplete = categoryResults.values().stream()
            .allMatch(CategoryAnalysisResult::isCompleted);

        // 총 학점 충족 여부 확인
        boolean totalCreditsComplete = totalEarnedCredits >= totalRequiredCredits;

        isGraduationReady = allCategoriesComplete && totalCreditsComplete;
    }
}
```

### 데이터 흐름도

```
┌─────────────────────┐
│  Firestore          │
│  graduation_        │
│  requirements_v2    │
│  (단일 문서)        │
└──────────┬──────────┘
           │ 1회 쿼리
           ↓
┌──────────────────────┐
│ FirebaseDataManager  │
│ .loadGraduationRules()│
└──────────┬───────────┘
           │ 역직렬화
           ↓
┌──────────────────────┐
│  GraduationRules     │
│  (Java 모델)         │
└──────────┬───────────┘
           │
           ↓
┌──────────────────────┐
│  .analyze()          │
│  수강 과목 목록 입력 │
└──────────┬───────────┘
           │ 자동 분석
           ↓
┌──────────────────────┐
│ GraduationAnalysis   │
│ Result               │
│ (분석 결과)          │
└──────────┬───────────┘
           │
           ↓
┌──────────────────────┐
│ UI 표시              │
│ (Activity/Fragment)  │
└──────────────────────┘
```

**장점:**
- 단일 쿼리로 모든 정보 획득 (4회 → 1회)
- 하드코딩 완전 제거
- 분석 로직이 데이터 구조를 따름
- 새 규칙 추가 시 코드 수정 불필요

---

## 세부 실행 계획

### Phase 1: 스키마 설계 및 모델 클래스 구현 (3-4일)

#### ✅ 완료된 작업
- `UNIFIED_SCHEMA_DESIGN.md` 작성
- `models/CourseRequirement.java` 구현
- `models/CategoryAnalysisResult.java` 구현

#### 📋 남은 작업

**Day 1-2: 핵심 모델 클래스**
- [ ] `models/GraduationRules.java` 구현
  - 필드 정의
  - Firestore 역직렬화 지원 (빈 생성자, getter/setter)
  - `analyze()` 메서드 스켈레톤

- [ ] `models/RequirementCategory.java` 구현
  - 필드 정의
  - `analyze()` 메서드 및 타입별 분석 로직
    - `analyzeList()`
    - `analyzeOneOf()`
    - `analyzeGroup()`
    - `analyzeCompetency()`

- [ ] `models/CreditRequirements.java` 구현
  - 카테고리별 학점 요건 관리
  - `getRequiredCredits(categoryName)` 헬퍼

**Day 3: 대체과목 및 결과 모델**
- [ ] `models/ReplacementRule.java` 구현
  - `canApply()` 로직
  - `CourseInfo` 내부 클래스

- [ ] `models/GraduationAnalysisResult.java` 확장
  - 경고/추천사항 생성 로직
  - `calculateGraduationReadiness()` 구현

**Day 4: 단위 테스트**
- [ ] 모델 클래스 단위 테스트 작성
  - `RequirementCategoryTest.java`
  - `ReplacementRuleTest.java`
  - `GraduationRulesTest.java`

**검증 기준:**
- 모든 모델 클래스가 Firestore 역직렬화 가능
- oneOf, list, group 타입 분석 로직 정상 동작
- 대체과목 적용 로직 테스트 통과

---

### Phase 2: FirebaseDataManager 리팩토링 (2-3일)

#### 목표
기존의 분산된 로딩 메서드를 통합 API로 교체

#### 작업 내역

**Day 5: 통합 로딩 메서드 구현**
- [ ] `FirebaseDataManager.java`에 새 메서드 추가
  ```java
  public void loadGraduationRules(
      String cohort,
      String department,
      String track,
      OnGraduationRulesLoadedListener listener
  ) {
      String docId = cohort + "_" + department + "_" + track;

      db.collection("graduation_requirements_v2")
        .document(docId)
        .get()
        .addOnSuccessListener(documentSnapshot -> {
            GraduationRules rules = documentSnapshot.toObject(GraduationRules.class);
            listener.onSuccess(rules);
        })
        .addOnFailureListener(e -> {
            listener.onFailure(e);
        });
  }
  ```

- [ ] 리스너 인터페이스 정의
  ```java
  public interface OnGraduationRulesLoadedListener {
      void onSuccess(GraduationRules rules);
      void onFailure(Exception e);
  }
  ```

**Day 6: 레거시 메서드 유지 및 병행 운영**
- [ ] 기존 메서드는 `@Deprecated` 마킹
  - `loadMajorCourses()`
  - `loadGeneralEducationCourses()`
  - `loadCreditRequirements()`
  - `loadReplacementCourses()`

- [ ] 새 메서드와 병행 운영 (호환성 유지)

**Day 7: 캐싱 및 오프라인 지원**
- [ ] Firestore 캐시 설정
  ```java
  FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
      .setPersistenceEnabled(true)
      .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
      .build();
  db.setFirestoreSettings(settings);
  ```

- [ ] 오프라인 상태 감지 및 알림

**검증 기준:**
- 단일 쿼리로 모든 졸업요건 데이터 로드
- 기존 앱 기능에 영향 없음
- 오프라인 모드에서도 캐시 데이터 사용 가능

---

### Phase 3: GraduationAnalysisResultActivity 리팩토링 (4-5일)

#### 목표
1,300줄의 하드코딩된 분석 로직을 데이터 기반으로 전환

#### 작업 내역

**Day 8-9: 초기화 로직 리팩토링**
- [ ] `onCreate()`에서 통합 로딩으로 변경
  ```java
  @Override
  protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      // 기존: 4개 메서드 호출
      // loadCreditRequirements();
      // loadMajorCourses();
      // loadGeneralEducationCourses();
      // loadReplacementCourses();

      // 신규: 1개 메서드 호출
      loadGraduationRules();
  }

  private void loadGraduationRules() {
      FirebaseDataManager.getInstance().loadGraduationRules(
          selectedYear, selectedDepartment, selectedTrack,
          new OnGraduationRulesLoadedListener() {
              @Override
              public void onSuccess(GraduationRules rules) {
                  graduationRules = rules;
                  analyzeGraduationRequirements();
              }

              @Override
              public void onFailure(Exception e) {
                  showErrorDialog();
              }
          }
      );
  }
  ```

**Day 10-11: 분석 로직 단순화**
- [ ] 기존 `analyzeMajorRequiredCourses()` 제거
- [ ] 기존 `analyzeGeneralEducationCourses()` 제거
- [ ] 새로운 단일 분석 메서드
  ```java
  private void analyzeGraduationRequirements() {
      // 수강한 과목 목록 준비
      List<Course> takenCourses = courseList;

      // GraduationRules 모델의 analyze() 메서드 호출
      GraduationAnalysisResult result = graduationRules.analyze(takenCourses);

      // 결과를 UI에 표시
      displayAnalysisResult(result);
  }
  ```

**Day 12: UI 업데이트 로직**
- [ ] `displayAnalysisResult()` 구현
  - 카테고리별 결과 표시
  - 서브그룹(oneOf) 결과 표시
  - 경고/추천사항 표시

- [ ] RecyclerView 어댑터 수정
  - `CategoryAnalysisResult`를 표시하도록 변경

**검증 기준:**
- 코드 라인 수 1,300줄 → 500줄 이하로 감소
- 하드코딩된 분기문 완전 제거
- 모든 기존 테스트 시나리오 통과

---

### Phase 4: 관리자 UI 통합 (3-4일)

#### 목표
여러 개의 관리 화면을 단일 편집 화면으로 통합

#### 현재 관리자 화면
- `GraduationRequirementsActivity`: 학점 요건 관리
- `MajorDocumentManagementActivity`: 전공 과목 관리
- `EducationDocumentManagementActivity`: 교양 과목 관리
- `ReplacementCourseManagementActivity`: 대체과목 관리

#### 신규 통합 화면

**Day 13: 통합 편집 액티비티 생성**
- [ ] `UnifiedGraduationRuleEditorActivity.java` 생성
  - 학번/학과/트랙 선택
  - 기존 문서 로드 또는 새 문서 생성

- [ ] `activity_unified_rule_editor.xml` 레이아웃
  - 탭 구조:
    - Tab 1: 기본정보 (cohort, department, track)
    - Tab 2: 학점 요건 (creditRequirements)
    - Tab 3: 카테고리 관리 (categories)
    - Tab 4: 대체과목 (replacementRules)

**Day 14: 카테고리 편집 UI**
- [ ] RecyclerView로 카테고리 목록 표시
- [ ] 카테고리 추가/수정/삭제
- [ ] 하위 서브그룹 편집 (oneOf, group)
- [ ] 과목 검색 및 추가

**Day 15-16: 저장 및 검증**
- [ ] 입력 데이터 검증
  - 필수 필드 체크
  - 학점 합계 검증
  - 중복 과목 체크

- [ ] Firestore 저장
  ```java
  private void saveGraduationRules() {
      String docId = cohort + "_" + department + "_" + track;

      db.collection("graduation_requirements_v2")
        .document(docId)
        .set(graduationRules)
        .addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "저장 완료", Toast.LENGTH_SHORT).show();
        });
  }
  ```

**검증 기준:**
- 관리자가 단일 화면에서 모든 졸업요건 편집 가능
- 저장 후 사용자 앱에서 즉시 반영 확인
- 데이터 무결성 검증 통과

---

### Phase 5: 데이터 마이그레이션 (2일)

#### 목표
기존 4개 컬렉션 데이터를 `graduation_requirements_v2`로 통합

#### 마이그레이션 전략

**Day 17: 마이그레이션 스크립트 작성**
- [ ] Node.js 스크립트 생성: `migrate_to_v2.js`
  ```javascript
  const admin = require('firebase-admin');
  const serviceAccount = require('./serviceAccountKey.json');

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });

  const db = admin.firestore();

  async function migrateToV2() {
    // 1. graduation_requirements 문서 읽기
    const reqSnapshot = await db.collection('graduation_requirements').get();

    for (const reqDoc of reqSnapshot.docs) {
      const data = reqDoc.data();
      const docId = `${data.year}_${data.department}_${data.track}`;

      // 2. major_documents 읽기
      const majorSnapshot = await db.collection('major_documents')
        .where('department', '==', data.department)
        .where('track', '==', data.track)
        .where('year', '==', data.year)
        .get();

      // 3. education_documents 읽기
      const eduSnapshot = await db.collection('education_documents')
        .where('year', '==', data.year)
        .get();

      // 4. replacement_courses 읽기
      const replSnapshot = await db.collection('replacement_courses')
        .where('department', '==', data.department)
        .get();

      // 5. 통합 문서 생성
      const unifiedDoc = {
        docId: docId,
        cohort: data.year,
        department: data.department,
        track: data.track,
        version: 'v2.0',
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        creditRequirements: data.creditRequirements,
        categories: buildCategories(majorSnapshot, eduSnapshot),
        replacementRules: buildReplacementRules(replSnapshot)
      };

      // 6. graduation_requirements_v2에 저장
      await db.collection('graduation_requirements_v2')
        .document(docId)
        .set(unifiedDoc);

      console.log(`✓ Migrated: ${docId}`);
    }
  }

  function buildCategories(majorSnapshot, eduSnapshot) {
    // 전공 과목 → categories 배열 변환
    // 교양 과목 → subgroups 구조 변환
  }

  function buildReplacementRules(replSnapshot) {
    // replacement_courses → replacementRules 배열 변환
  }

  migrateToV2().then(() => {
    console.log('Migration completed!');
    process.exit(0);
  }).catch(error => {
    console.error('Migration failed:', error);
    process.exit(1);
  });
  ```

**Day 18: 마이그레이션 실행 및 검증**
- [ ] 테스트 환경에서 먼저 실행
- [ ] 데이터 무결성 검증
  - 학점 합계 일치 확인
  - 과목 누락 확인
  - 카테고리 구조 확인

- [ ] 프로덕션 마이그레이션
- [ ] 백업 생성

**검증 기준:**
- 모든 기존 데이터가 새 구조로 정확히 변환
- 데이터 손실 없음
- 역변환 가능 (롤백 준비)

---

### Phase 6: 통합 테스트 및 버그 수정 (2-3일)

#### 목표
전체 시스템 통합 테스트 및 안정화

#### 테스트 시나리오

**Day 19: 기능 테스트**
- [ ] 시나리오 1: 2020학번 IT학부 인공지능 졸업요건 분석
  - 전공필수 24학점 충족 확인
  - 교양필수 oneOf 그룹 인정 확인
  - 대체과목 적용 확인

- [ ] 시나리오 2: 관리자 수정 반영 테스트
  - 관리자: "기독교" 그룹에 새 과목 추가
  - 사용자: 앱 재시작 없이 새 과목 인식

- [ ] 시나리오 3: 새 학번 추가
  - 관리자: 2026학번 졸업요건 신규 생성
  - 사용자: 2026학번 선택 시 정상 분석

**Day 20: 성능 테스트**
- [ ] 로딩 속도 측정
  - 기존: 4회 쿼리 평균 시간
  - 신규: 1회 쿼리 평균 시간
  - 목표: 50% 이상 개선

- [ ] 오프라인 모드 테스트
  - 네트워크 차단 후 캐시 데이터 사용 확인

**Day 21: 버그 수정 및 안정화**
- [ ] 발견된 버그 수정
- [ ] 에러 핸들링 강화
- [ ] 로깅 추가

**검증 기준:**
- 모든 테스트 시나리오 통과
- 성능 목표 달성
- 크래시 없음

---

### Phase 7: 문서화 및 배포 (1일)

#### 목표
최종 문서 작성 및 프로덕션 배포

#### 작업 내역

**Day 22: 문서 작성**
- [ ] `README_REFACTORING.md` 작성
  - 변경사항 요약
  - 마이그레이션 가이드
  - 관리자 매뉴얼

- [ ] API 문서 업데이트
  - `FirebaseDataManager` 새 메서드
  - `GraduationRules` 모델 사용법

- [ ] 릴리스 노트 작성
  ```markdown
  ## v2.0.0 - Unified Graduation Requirements System

  ### 주요 변경사항
  - 졸업요건 데이터 구조 통합
  - 하드코딩 제거 (1,300줄 → 500줄)
  - 관리자 UI 통합
  - 성능 개선 (로딩 속도 60% 향상)

  ### 마이그레이션
  - 기존 데이터는 자동으로 마이그레이션됩니다
  - 관리자는 새로운 통합 편집 화면을 사용하세요
  ```

**배포**
- [ ] APK 빌드
- [ ] Google Play 스토어 업로드
- [ ] 사용자 공지

---

## 일정 및 마일스톤

### 전체 일정 (4주)

```
Week 1: 모델 및 인프라
├─ Day 1-4: Phase 1 (모델 클래스)
└─ Day 5-7: Phase 2 (FirebaseDataManager)

Week 2: 핵심 로직 리팩토링
├─ Day 8-12: Phase 3 (GraduationAnalysisResultActivity)
└─ Day 13: Phase 4 시작 (관리자 UI)

Week 3: 관리자 UI 및 마이그레이션
├─ Day 14-16: Phase 4 완료
└─ Day 17-18: Phase 5 (데이터 마이그레이션)

Week 4: 테스트 및 배포
├─ Day 19-21: Phase 6 (통합 테스트)
└─ Day 22: Phase 7 (문서화 및 배포)
```

### 마일스톤

| 마일스톤 | 완료 예정일 | 산출물 | 검증 기준 |
|----------|-------------|--------|-----------|
| M1: 모델 완성 | Day 4 | models/ 패키지 | 단위 테스트 통과 |
| M2: 데이터 로딩 통합 | Day 7 | FirebaseDataManager 리팩토링 | 단일 쿼리로 전체 로딩 |
| M3: 분석 로직 리팩토링 | Day 12 | GraduationAnalysisResultActivity | 하드코딩 제거 완료 |
| M4: 관리자 UI 통합 | Day 16 | UnifiedGraduationRuleEditorActivity | 단일 화면 편집 가능 |
| M5: 데이터 마이그레이션 | Day 18 | v2 컬렉션 완성 | 데이터 무결성 확인 |
| M6: 통합 테스트 완료 | Day 21 | 테스트 리포트 | 모든 시나리오 통과 |
| M7: 프로덕션 배포 | Day 22 | APK + 문서 | 사용자 피드백 정상 |

---

## 위험 요소 및 대응

### 위험 요소 1: 데이터 마이그레이션 실패

**위험도:** 🔴 High

**증상:**
- 기존 데이터가 새 구조로 변환 중 손실
- 카테고리 구조 불일치
- 학점 합계 오류

**영향:**
- 사용자 졸업요건 분석 불가능
- 데이터 복구 필요
- 서비스 중단

**대응 방안:**
1. **사전 예방**
   - 마이그레이션 전 전체 백업
   - 테스트 환경에서 충분한 검증
   - Dry-run 모드로 먼저 실행

2. **발생 시 조치**
   - 즉시 롤백
   - 백업 데이터 복원
   - 마이그레이션 스크립트 수정 후 재시도

3. **롤백 계획**
   ```javascript
   async function rollback() {
     // graduation_requirements_v2 삭제
     await db.collection('graduation_requirements_v2').get().then(snapshot => {
       snapshot.docs.forEach(doc => doc.ref.delete());
     });

     // 백업에서 기존 컬렉션 복원
     // (백업 파일: backup_YYYY-MM-DD.json)
   }
   ```

---

### 위험 요소 2: 성능 저하

**위험도:** 🟡 Medium

**증상:**
- 단일 문서 크기 증가로 인한 로딩 지연
- Firestore 읽기 비용 증가
- 앱 응답 속도 저하

**영향:**
- 사용자 경험 악화
- Firebase 비용 증가

**대응 방안:**
1. **사전 예방**
   - Firestore 캐시 활성화
   - 필요한 필드만 선택적으로 로드
   - 압축 및 최적화

2. **모니터링**
   - Firebase Performance Monitoring 설정
   - 로딩 시간 측정 (목표: 2초 이하)
   - 문서 크기 모니터링 (목표: 1MB 이하)

3. **최적화**
   ```java
   // 필요한 필드만 로드
   db.collection("graduation_requirements_v2")
     .document(docId)
     .get(Source.CACHE)  // 캐시 우선
     .addOnSuccessListener(...)
   ```

---

### 위험 요소 3: 하위 호환성 문제

**위험도:** 🟡 Medium

**증상:**
- 기존 코드가 새 구조를 인식하지 못함
- 구버전 앱에서 오류 발생
- 데이터 형식 불일치

**영향:**
- 구버전 사용자 기능 마비
- 강제 업데이트 필요

**대응 방안:**
1. **사전 예방**
   - 기존 컬렉션 유지 (읽기 전용)
   - 새 컬렉션과 병행 운영
   - 최소 지원 버전 설정

2. **버전 관리**
   ```java
   // 앱 버전 체크
   int minSupportedVersion = 20;  // v2.0
   if (currentVersion < minSupportedVersion) {
     showUpdateDialog();
     return;
   }
   ```

3. **점진적 마이그레이션**
   - Phase 1: 신규 사용자만 v2 사용
   - Phase 2: 기존 사용자 선택적 마이그레이션
   - Phase 3: 전체 전환

---

### 위험 요소 4: 일정 지연

**위험도:** 🟡 Medium

**증상:**
- 예상보다 복잡한 로직
- 버그 수정 시간 초과
- 테스트 실패

**영향:**
- 배포 일정 지연
- 리소스 추가 투입 필요

**대응 방안:**
1. **사전 예방**
   - 충분한 버퍼 시간 확보 (20%)
   - 일일 진행상황 체크
   - 우선순위 명확화

2. **일정 단축 옵션**
   - MVP 기능만 먼저 구현
   - 비핵심 기능 후순위
   - 자동화 도구 활용

3. **비상 계획**
   - Critical Path: Phase 1-3 (핵심 기능)
   - Optional: Phase 4 (관리자 UI는 기존 유지 가능)

---

### 위험 요소 5: Firebase 비용 증가

**위험도:** 🟢 Low

**증상:**
- 단일 문서 크기 증가
- 읽기 횟수 증가 (캐시 미스)

**영향:**
- 운영 비용 상승

**대응 방안:**
1. **비용 모니터링**
   - Firebase Console에서 일일 비용 체크
   - 읽기/쓰기 횟수 추적

2. **최적화**
   - 캐시 적극 활용
   - 불필요한 쿼리 제거
   - 배치 작업 활용

3. **예상 비용 계산**
   ```
   기존:
   - 사용자 1명당 4회 읽기 × 30일 = 120회/월
   - 1,000명 = 120,000회/월
   - 비용: $0.36/월 (무료 범위 내)

   신규:
   - 사용자 1명당 1회 읽기 × 30일 = 30회/월
   - 1,000명 = 30,000회/월
   - 비용: $0.09/월 (75% 절감)
   ```

---

## 성공 지표

### 정량적 지표

| 지표 | 현재 | 목표 | 측정 방법 |
|------|------|------|-----------|
| 코드 라인 수 | 1,300줄 | 500줄 이하 | Gradle task |
| Firebase 쿼리 횟수 | 4회 | 1회 | 로그 분석 |
| 로딩 시간 | 3-4초 | 1-2초 | Performance Monitoring |
| 하드코딩 분기문 | 50+ | 0 | 코드 리뷰 |
| 관리자 편집 화면 수 | 4개 | 1개 | UI 카운트 |

### 정성적 지표

- [ ] 새 학번/학과 추가 시 코드 수정 불필요
- [ ] 관리자 변경사항이 즉시 사용자에게 반영
- [ ] oneOf 그룹을 관리자가 자유롭게 편집 가능
- [ ] 대체과목 규칙을 앱 재배포 없이 추가 가능
- [ ] 데이터 구조가 명확하고 이해하기 쉬움

---

## 후속 작업 (Optional)

리팩토링 완료 후 추가로 고려할 사항:

### 1. 실시간 동기화
- Firestore 리스너로 실시간 업데이트
- 관리자 수정 시 사용자 화면 자동 갱신

### 2. 버전 관리
- 졸업요건 변경 이력 추적
- 특정 시점으로 롤백 기능

### 3. AI 기반 추천
- 수강 과목 기반 다음 학기 추천
- 졸업 최적 경로 제안

### 4. 다국어 지원
- 영어/한국어 전환
- 국제 학생 지원

---

## 참고 문서

- [UNIFIED_SCHEMA_DESIGN.md](./UNIFIED_SCHEMA_DESIGN.md): 통합 스키마 상세 설계
- [Firestore 문서조회 시나리오.md](./Firestore_문서조회_시나리오.md): 현재 데이터 조회 로직
- [Firebase Firestore Best Practices](https://firebase.google.com/docs/firestore/best-practices)
- [Android MVVM Architecture Guide](https://developer.android.com/topic/architecture)

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 | 작성자 |
|------|------|-----------|--------|
| 2025-10-14 | 1.0 | 초안 작성 | Claude |

---

**문서 상태:** 🟢 Active
**마지막 업데이트:** 2025-10-14
**다음 리뷰 예정:** 리팩토링 시작 전
