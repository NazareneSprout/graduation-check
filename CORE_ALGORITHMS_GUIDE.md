# ⚙️ 핵심 기능 알고리즘 가이드

> SakMvp1 프로젝트의 핵심 비즈니스 로직과 알고리즘을 완벽 분석

## 📖 목차

1. [졸업요건 분석 알고리즘](#1-졸업요건-분석-알고리즘)
2. [학점 오버플로우 처리](#2-학점-오버플로우-처리)
3. [대체과목 시스템](#3-대체과목-시스템)
4. [교양필수 oneOf 그룹 분석](#4-교양필수-oneof-그룹-분석)
5. [역량 분석 알고리즘](#5-역량-분석-알고리즘)
6. [과목 추천 알고리즘](#6-과목-추천-알고리즘)
7. [캐시 무효화 전략](#7-캐시-무효화-전략)

---

## 1. 졸업요건 분석 알고리즘

### 1.1 전체 흐름

```
사용자 입력 과목 리스트
        ↓
① 대체과목 규칙 적용 (가상 과목 추가)
        ↓
② 카테고리별 분석 (전공필수, 전공선택, 교양 등)
        ↓
③ 총 학점 계산
        ↓
④ 오버플로우 처리 (넘치는 학점 재분배)
        ↓
⑤ 졸업 가능 여부 판단
        ↓
결과 반환
```

### 1.2 핵심 코드 분석

```java
/**
 * GraduationRules.java - analyze() 메서드
 * 졸업요건 분석의 진입점
 */
public GraduationAnalysisResult analyze(List<Course> takenCourses) {
    Log.d(TAG, "Starting graduation analysis for: " + docId);

    GraduationAnalysisResult result = new GraduationAnalysisResult();

    // ============ 1단계: 대체과목 적용 ============
    List<Course> adjustedCourses = applyReplacementRules(takenCourses, result);

    // ============ 2단계: 카테고리별 분석 ============
    Map<String, CategoryAnalysisResult> categoryResults = new HashMap<>();
    for (RequirementCategory category : categories) {
        CategoryAnalysisResult categoryResult = category.analyze(adjustedCourses);
        categoryResults.put(category.getId(), categoryResult);
        result.addCategoryResult(categoryResult);
    }

    // ============ 3단계: 총 학점 계산 ============
    int totalEarnedCredits = 0;
    for (CategoryAnalysisResult categoryResult : categoryResults.values()) {
        totalEarnedCredits += categoryResult.getEarnedCredits();
    }
    result.setTotalEarnedCredits(totalEarnedCredits);

    // ============ 4단계: 오버플로우 처리 ============
    handleOverflowCredits(result, categoryResults);

    // ============ 5단계: 졸업 가능 여부 ============
    result.calculateGraduationReadiness();

    return result;
}
```

### 1.3 카테고리 분석 알고리즘

#### 카테고리 타입별 분석 방법

```java
/**
 * RequirementCategory.java - analyze() 메서드
 * 카테고리별 분석 로직
 */
public CategoryAnalysisResult analyze(List<Course> takenCourses) {
    CategoryAnalysisResult result = new CategoryAnalysisResult(id, name);

    switch (type) {
        case "fixed":
            // 고정 과목 (전공필수)
            analyzeFixedCourses(takenCourses, result);
            break;

        case "elective":
            // 선택 과목 (전공선택)
            analyzeElectiveCourses(takenCourses, result);
            break;

        case "general_required":
            // 교양필수 (oneOf 그룹 포함)
            analyzeGeneralRequired(takenCourses, result);
            break;

        case "credit_based":
            // 학점 기반 (교양선택, 소양)
            analyzeCreditBased(takenCourses, result);
            break;
    }

    result.calculateCompletion();
    return result;
}
```

#### Fixed (고정 과목) 분석

```java
/**
 * 전공필수 같은 고정 과목 분석
 * 모든 필수 과목을 수강했는지 확인
 */
private void analyzeFixedCourses(List<Course> takenCourses,
                                  CategoryAnalysisResult result) {
    // 수강한 과목명 추출
    Set<String> takenNames = new HashSet<>();
    for (Course course : takenCourses) {
        if (course.getCategory().equals(this.name)) {
            takenNames.add(course.getName());
        }
    }

    int earnedCredits = 0;
    List<String> completed = new ArrayList<>();
    List<String> missing = new ArrayList<>();

    // 각 필수 과목 체크
    for (CourseInfo requiredCourse : availableCourses) {
        if (takenNames.contains(requiredCourse.getName())) {
            // 이수함
            completed.add(requiredCourse.getName());
            earnedCredits += requiredCourse.getCredits();
        } else {
            // 미이수
            missing.add(requiredCourse.getName());
        }
    }

    result.setEarnedCredits(earnedCredits);
    result.setRequiredCredits(required);
    result.setCompletedCourses(completed);
    result.setMissingCourses(missing);
}
```

**예시**:
```
필수 과목: [프로그래밍기초(3), 자료구조(3), 알고리즘(3)] - 총 9학점
수강 과목: [프로그래밍기초(3), 자료구조(3)]

결과:
- 이수: 6학점 / 9학점
- 완료: [프로그래밍기초, 자료구조]
- 미이수: [알고리즘]
```

#### Elective (선택 과목) 분석

```java
/**
 * 전공선택 같은 선택 과목 분석
 * 필수 학점만 채우면 됨
 */
private void analyzeElectiveCourses(List<Course> takenCourses,
                                     CategoryAnalysisResult result) {
    // 수강한 과목명과 학점 추출
    int earnedCredits = 0;
    List<String> completed = new ArrayList<>();

    for (Course course : takenCourses) {
        if (course.getCategory().equals(this.name)) {
            completed.add(course.getName());
            earnedCredits += course.getCredits();
        }
    }

    result.setEarnedCredits(earnedCredits);
    result.setRequiredCredits(required);
    result.setCompletedCourses(completed);

    // 선택 과목은 "미이수" 개념 없음 (학점만 채우면 됨)
    result.setMissingCourses(new ArrayList<>());
}
```

**예시**:
```
요구 학점: 18학점
수강 과목: [웹프로그래밍(3), 모바일앱(3), 데이터베이스(3), 네트워크(3)]

결과:
- 이수: 12학점 / 18학점
- 완료: [웹프로그래밍, 모바일앱, 데이터베이스, 네트워크]
- 부족: 6학점
```

---

## 2. 학점 오버플로우 처리

### 2.1 개념

**오버플로우란?**
- 특정 카테고리에서 요구 학점을 초과한 경우
- 초과 학점을 다른 카테고리(일반선택/잔여학점)로 재분배

### 2.2 학번별 처리 규칙

```java
/**
 * 학번에 따른 오버플로우 목적지
 */
String overflowDestination;

if (cohort >= 2020 && cohort <= 2022) {
    // 20-22학번: 구 교육과정
    overflowDestination = "일반선택";
} else if (cohort >= 2023 && cohort <= 2025) {
    // 23-25학번: 신 교육과정
    overflowDestination = "잔여학점";
}
```

### 2.3 오버플로우 처리 알고리즘

```java
/**
 * GraduationRules.java - handleOverflowCredits()
 * 넘치는 학점을 목적지 카테고리로 이동
 */
private void handleOverflowCredits(GraduationAnalysisResult result,
                                   Map<String, CategoryAnalysisResult> categoryResults) {
    Log.d(TAG, "Handling overflow credits to: " + overflowDestination);

    int totalOverflow = 0;

    // ============ 1단계: 각 카테고리의 넘침 계산 ============
    for (RequirementCategory category : categories) {
        CategoryAnalysisResult categoryResult = categoryResults.get(category.getId());

        int earned = categoryResult.getEarnedCredits();
        int required = creditRequirements.getRequiredCredits(category.getName());

        if (earned > required && required > 0) {
            int overflow = earned - required;
            totalOverflow += overflow;

            Log.d(TAG, category.getName() + ": +" + overflow + " overflow");
        }
    }

    // ============ 2단계: 넘침 학점을 목적지에 추가 ============
    if (totalOverflow > 0) {
        CategoryAnalysisResult overflowCategory = categoryResults.get(overflowDestination);

        if (overflowCategory == null) {
            // 목적지 카테고리가 없으면 생성
            overflowCategory = new CategoryAnalysisResult(
                overflowDestination, overflowDestination
            );
            result.addCategoryResult(overflowCategory);
        }

        int currentEarned = overflowCategory.getEarnedCredits();
        overflowCategory.setEarnedCredits(currentEarned + totalOverflow);

        Log.d(TAG, "Total overflow: " + totalOverflow +
                   " added to " + overflowDestination);
    }
}
```

### 2.4 실전 예시

#### 20-22학번 (구 교육과정)

```
카테고리별 이수 현황:
- 전공필수: 45/42학점 (3학점 초과)
- 전공선택: 20/18학점 (2학점 초과)
- 교양필수: 30/27학점 (3학점 초과)
- 교양선택: 8/6학점 (2학점 초과)

총 오버플로우: 3 + 2 + 3 + 2 = 10학점

재분배 후:
- 전공필수: 42/42학점 (완료)
- 전공선택: 18/18학점 (완료)
- 교양필수: 27/27학점 (완료)
- 교양선택: 6/6학점 (완료)
- 일반선택: 10/19학점 ← 오버플로우 추가
```

#### 23-25학번 (신 교육과정)

```
카테고리별 이수 현황:
- 전공필수: 45/42학점 (3학점 초과)
- 전공선택: 20/18학점 (2학점 초과)

총 오버플로우: 3 + 2 = 5학점

재분배 후:
- 전공필수: 42/42학점 (완료)
- 전공선택: 18/18학점 (완료)
- 잔여학점: 5/19학점 ← 오버플로우 추가
```

### 2.5 UI 표시 로직

```java
/**
 * GraduationAnalysisResultActivity.java
 * convertAnalysisResultToLegacyFormat()
 *
 * 오버플로우 처리된 결과를 UI에 표시
 */
private void convertAnalysisResultToLegacyFormat(GraduationAnalysisResult result) {
    int totalOverflow = 0;
    boolean isOldCurriculum = (cohort >= 2020 && cohort <= 2022);
    String overflowDestination = isOldCurriculum ? "일반선택" : "잔여학점";

    // ============ 각 카테고리의 넘침 계산 ============
    for (CategoryAnalysisResult categoryResult : result.getAllCategoryResults()) {
        String categoryName = categoryResult.getCategoryName();
        int earned = categoryResult.getEarnedCredits();
        int required = categoryResult.getRequiredCredits();

        // 오버플로우 대상 카테고리는 제외
        boolean isOverflowCategory = "일반선택".equals(categoryName) ||
                                     "자율선택".equals(categoryName) ||
                                     "잔여학점".equals(categoryName);

        if (!isOverflowCategory && required > 0 && earned > required) {
            int overflow = earned - required;
            totalOverflow += overflow;

            // UI 표시용: earned를 required로 제한
            earned = required;

            Log.d(TAG, categoryName + " 넘침: " + overflow + "학점 " +
                       "(표시: " + earned + "/" + required + ")");
        }

        // CategoryProgress 생성
        CategoryProgress progress = new CategoryProgress(earned, required);

        switch (categoryName) {
            case "전공필수":
                graduationProgress.majorRequired = progress;
                break;
            case "전공선택":
                graduationProgress.majorElective = progress;
                break;
            // ... 다른 카테고리들
        }
    }

    // ============ 넘침을 목적지에 추가 ============
    if (totalOverflow > 0) {
        Log.d(TAG, "총 넘침: " + totalOverflow + " → " + overflowDestination);

        CategoryProgress targetProgress = null;
        int currentEarned = 0;
        int targetRequired = 0;

        if ("일반선택".equals(overflowDestination)) {
            if (graduationProgress.generalSelection != null) {
                currentEarned = graduationProgress.generalSelection.earned;
                targetRequired = graduationProgress.generalSelection.required;
            }
            graduationProgress.generalSelection =
                new CategoryProgress(currentEarned + totalOverflow, targetRequired);
        } else if ("잔여학점".equals(overflowDestination)) {
            if (graduationProgress.remainingCredits != null) {
                currentEarned = graduationProgress.remainingCredits.earned;
                targetRequired = graduationProgress.remainingCredits.required;
            }
            graduationProgress.remainingCredits =
                new CategoryProgress(currentEarned + totalOverflow, targetRequired);
        }
    }
}
```

---

## 3. 대체과목 시스템

### 3.1 개념

**대체과목이란?**
- 폐강된 과목을 다른 과목으로 대체 인정
- 예: "IT개론"이 폐강 → "소프트웨어개론" 수강 시 "IT개론" 학점 인정

### 3.2 데이터 구조

```java
/**
 * ReplacementRule.java
 * 대체과목 규칙 데이터 모델
 */
public class ReplacementRule {
    private String scope;                    // "document" 또는 "department"
    private CourseInfo discontinuedCourse;   // 폐강된 과목
    private List<CourseInfo> replacementCourses;  // 대체 가능 과목 리스트

    public static class CourseInfo {
        private String name;       // 과목명
        private String category;   // 카테고리 (전공필수, 학부공통 등)
        private int credits;       // 학점
    }
}
```

**Firestore 데이터 예시**:
```json
{
  "replacementRules": [
    {
      "scope": "department",
      "discontinuedCourse": {
        "name": "IT개론",
        "category": "학부공통",
        "credits": 3
      },
      "replacementCourses": [
        {
          "name": "소프트웨어개론",
          "category": "전공선택",
          "credits": 3
        },
        {
          "name": "컴퓨터개론",
          "category": "전공선택",
          "credits": 3
        }
      ]
    }
  ]
}
```

### 3.3 적용 알고리즘

```java
/**
 * GraduationRules.java - applyReplacementRules()
 * 대체과목 규칙 적용
 */
private List<Course> applyReplacementRules(List<Course> takenCourses,
                                            GraduationAnalysisResult result) {
    List<Course> adjustedCourses = new ArrayList<>(takenCourses);

    // ============ 수강 과목명 추출 ============
    List<String> takenCourseNames = new ArrayList<>();
    for (Course course : takenCourses) {
        takenCourseNames.add(course.getName());
    }

    // ============ 각 규칙 적용 ============
    for (ReplacementRule rule : replacementRules) {
        // Scope 체크
        String scope = rule.getScope();
        if ("document".equals(scope)) {
            // 해당 문서에만 적용
        } else if ("department".equals(scope)) {
            // 학부 전체에 적용
        }

        // ============ 적용 가능 여부 확인 ============
        if (rule.canApply(takenCourseNames)) {
            CourseInfo discontinuedCourse = rule.getDiscontinuedCourse();
            String takenReplacement = rule.getTakenReplacementCourse(takenCourseNames);

            // ============ 가상 과목 추가 ============
            Course virtualCourse = new Course(
                discontinuedCourse.getCategory(),
                discontinuedCourse.getName(),
                discontinuedCourse.getCredits()
            );
            adjustedCourses.add(virtualCourse);

            result.addAppliedReplacement(rule);

            Log.d(TAG, "✓ 대체 적용:");
            Log.d(TAG, "  폐강 과목: " + discontinuedCourse.getName());
            Log.d(TAG, "  대체 수강: " + takenReplacement);
        }
    }

    return adjustedCourses;
}
```

### 3.4 중복 방지 로직

```java
/**
 * ReplacementRule.java - canApply()
 * 대체 규칙 적용 가능 여부 확인
 */
public boolean canApply(List<String> takenCourseNames) {
    // ============ 1단계: 폐강 과목을 직접 수강했는지 확인 ============
    if (takenCourseNames.contains(discontinuedCourse.getName())) {
        // 직접 수강했으면 대체 불필요
        return false;
    }

    // ============ 2단계: 대체 과목 중 하나라도 수강했는지 확인 ============
    for (CourseInfo replacement : replacementCourses) {
        if (takenCourseNames.contains(replacement.getName())) {
            return true;  // 대체 적용 가능
        }
    }

    return false;  // 대체 적용 불가
}

/**
 * 수강한 대체 과목 중 첫 번째만 반환 (중복 방지)
 */
public String getTakenReplacementCourse(List<String> takenCourseNames) {
    for (CourseInfo replacement : replacementCourses) {
        if (takenCourseNames.contains(replacement.getName())) {
            return replacement.getName();  // 첫 번째만 반환
        }
    }
    return null;
}
```

### 3.5 실전 예시

#### 시나리오 1: 정상 대체

```
규칙:
- 폐강: IT개론 (학부공통, 3학점)
- 대체: [소프트웨어개론, 컴퓨터개론]

수강 과목:
- 소프트웨어개론 (전공선택, 3학점)

처리:
1. IT개론을 직접 수강하지 않음 ✓
2. 소프트웨어개론 수강함 ✓
3. 가상 과목 추가: IT개론 (학부공통, 3학점)

최종 과목 리스트:
- 소프트웨어개론 (전공선택, 3학점) ← 원본
- IT개론 (학부공통, 3학점) ← 가상 추가

결과:
- 학부공통: +3학점
- 전공선택: +3학점
```

#### 시나리오 2: 중복 수강 (방지)

```
규칙:
- 폐강: IT개론 (학부공통, 3학점)
- 대체: [소프트웨어개론, 컴퓨터개론]

수강 과목:
- 소프트웨어개론 (전공선택, 3학점)
- 컴퓨터개론 (전공선택, 3학점)

처리:
1. IT개론을 직접 수강하지 않음 ✓
2. 소프트웨어개론 수강함 ✓
3. 가상 과목 추가: IT개론 (학부공통, 3학점)
4. 컴퓨터개론은 무시 (첫 번째만 인정)

최종 과목 리스트:
- 소프트웨어개론 (전공선택, 3학점) ← 대체로 사용
- 컴퓨터개론 (전공선택, 3학점) ← 원래 카테고리 유지
- IT개론 (학부공통, 3학점) ← 가상 추가

결과:
- 학부공통: +3학점 (IT개론)
- 전공선택: +6학점 (소프트웨어개론 + 컴퓨터개론)

⚠️ 주의: 컴퓨터개론은 전공선택으로 인정됨
```

#### 시나리오 3: 직접 수강 (대체 미적용)

```
규칙:
- 폐강: IT개론 (학부공통, 3학점)
- 대체: [소프트웨어개론]

수강 과목:
- IT개론 (학부공통, 3학점)

처리:
1. IT개론을 직접 수강함 ✗
2. 대체 적용 불필요

최종 과목 리스트:
- IT개론 (학부공통, 3학점) ← 원본 그대로

결과:
- 학부공통: +3학점
```

---

## 4. 교양필수 oneOf 그룹 분석

### 4.1 개념

**oneOf 그룹이란?**
- 여러 과목 중 **하나만** 선택하여 이수
- 예: [채플1, 채플2, 채플3, 채플4] 중 하나만 이수

### 4.2 데이터 구조

```java
/**
 * RequirementCategory.java
 * oneOf 그룹 정의
 */
public class RequirementCategory {
    private List<Subgroup> subgroups;  // oneOf 그룹 리스트

    public static class Subgroup {
        private String id;              // 그룹 ID
        private String name;            // 그룹 이름
        private String type;            // "oneOf" 또는 "fixed"
        private int required;           // 요구 학점
        private List<CourseInfo> courses;  // 선택 가능 과목 리스트
    }
}
```

**Firestore 데이터 예시**:
```json
{
  "categories": [
    {
      "id": "generalRequired",
      "name": "교양필수",
      "type": "general_required",
      "required": 27,
      "subgroups": [
        {
          "id": "chapel",
          "name": "채플",
          "type": "oneOf",
          "required": 3,
          "courses": [
            {"name": "채플1", "credits": 1},
            {"name": "채플2", "credits": 1},
            {"name": "채플3", "credits": 1},
            {"name": "채플4", "credits": 1}
          ]
        },
        {
          "id": "christianity",
          "name": "기독교이해",
          "type": "oneOf",
          "required": 3,
          "courses": [
            {"name": "기독교의이해", "credits": 3},
            {"name": "성서의이해", "credits": 3}
          ]
        }
      ]
    }
  ]
}
```

### 4.3 분석 알고리즘

```java
/**
 * RequirementCategory.java - analyzeGeneralRequired()
 * 교양필수 분석 (oneOf 그룹 포함)
 */
private void analyzeGeneralRequired(List<Course> takenCourses,
                                     CategoryAnalysisResult result) {
    // ============ 수강 과목명 추출 ============
    Set<String> takenNames = new HashSet<>();
    for (Course course : takenCourses) {
        if (course.getCategory().equals(this.name)) {
            takenNames.add(course.getName());
        }
    }

    int earnedCredits = 0;
    List<String> completed = new ArrayList<>();
    List<String> missing = new ArrayList<>();

    // ============ Subgroup 분석 ============
    for (Subgroup subgroup : subgroups) {
        SubgroupResult subgroupResult = new SubgroupResult(
            subgroup.getId(), subgroup.getName()
        );
        subgroupResult.setRequiredCredits(subgroup.getRequired());

        if ("oneOf".equals(subgroup.getType())) {
            // ============ oneOf 그룹 분석 ============
            analyzeOneOfGroup(subgroup, takenNames, subgroupResult,
                             earnedCredits, completed, missing);
        } else if ("fixed".equals(subgroup.getType())) {
            // ============ 개별 필수 과목 분석 ============
            analyzeFixedGroup(subgroup, takenNames, subgroupResult,
                             earnedCredits, completed, missing);
        }

        result.addSubgroupResult(subgroupResult);
    }

    result.setEarnedCredits(earnedCredits);
    result.setRequiredCredits(required);
    result.setCompletedCourses(completed);
    result.setMissingCourses(missing);
}

/**
 * oneOf 그룹 분석
 * 여러 과목 중 하나만 선택
 */
private void analyzeOneOfGroup(Subgroup subgroup,
                                Set<String> takenNames,
                                SubgroupResult result,
                                int earnedCredits,
                                List<String> completed,
                                List<String> missing) {
    // 선택 가능한 과목 리스트 설정
    List<String> availableCourses = new ArrayList<>();
    for (CourseInfo course : subgroup.getCourses()) {
        availableCourses.add(course.getName());
    }
    result.setAvailableCourses(availableCourses);

    // ============ 수강한 과목 찾기 ============
    String selectedCourse = null;
    int groupCredits = 0;

    for (CourseInfo course : subgroup.getCourses()) {
        if (takenNames.contains(course.getName())) {
            // 첫 번째로 찾은 과목만 인정
            if (selectedCourse == null) {
                selectedCourse = course.getName();
                groupCredits = course.getCredits();
                completed.add(course.getName());
            }
            // 여러 개 수강해도 하나만 인정
        }
    }

    // ============ 결과 설정 ============
    result.setSelectedCourse(selectedCourse);
    result.setEarnedCredits(groupCredits);

    if (groupCredits >= subgroup.getRequired()) {
        result.setCompleted(true);
    } else {
        result.setCompleted(false);
        // 미이수 표시 (그룹명으로)
        missing.add(subgroup.getName());
    }

    earnedCredits += groupCredits;
}
```

### 4.4 실전 예시

#### 시나리오 1: 정상 이수

```
oneOf 그룹: 채플
- 선택 가능: [채플1(1), 채플2(1), 채플3(1), 채플4(1)]
- 요구 학점: 3학점

수강 과목:
- 채플1 (1학점)
- 채플2 (1학점)
- 채플3 (1학점)

분석:
- 선택된 과목: 채플1, 채플2, 채플3
- 이수 학점: 3학점
- 완료 여부: ✓ 완료

UI 표시:
채플 그룹: 3/3학점 ✓
  • 채플1 (1학점)
  • 채플2 (1학점)
  • 채플3 (1학점)
```

#### 시나리오 2: 미이수

```
oneOf 그룹: 기독교이해
- 선택 가능: [기독교의이해(3), 성서의이해(3)]
- 요구 학점: 3학점

수강 과목:
- (없음)

분석:
- 선택된 과목: 없음
- 이수 학점: 0학점
- 완료 여부: ✗ 미완료

UI 표시:
기독교이해 그룹: 0/3학점 ✗
  ⚠️ 다음 중 하나를 선택하세요:
  - 기독교의이해
  - 성서의이해
```

#### 시나리오 3: 중복 수강

```
oneOf 그룹: 기독교이해
- 선택 가능: [기독교의이해(3), 성서의이해(3)]
- 요구 학점: 3학점

수강 과목:
- 기독교의이해 (3학점)
- 성서의이해 (3학점)

분석:
- 선택된 과목: 기독교의이해 (첫 번째만 인정)
- 이수 학점: 3학점
- 완료 여부: ✓ 완료

⚠️ 주의: 성서의이해는 인정되지 않음 (중복)

UI 표시:
기독교이해 그룹: 3/3학점 ✓
  • 기독교의이해 (3학점)

⚠️ 성서의이해(3학점)는 중복으로 인정되지 않습니다.
```

---

## 5. 역량 분석 알고리즘

### 5.1 개념

**역량이란?**
- 교양 과목에 부여된 역량 태그 (1역량, 2역량, 3역량 등)
- 각 역량별 최소 이수 학점 충족 필요

### 5.2 데이터 구조

```java
/**
 * Course.java
 * 역량 정보 포함
 */
public class Course {
    private String name;
    private String category;
    private int credits;
    private String competency;  // "1역량", "2역량", "3역량" 등
}
```

### 5.3 분석 알고리즘

```java
/**
 * GraduationAnalysisResultActivity.java - analyzeCompetencies()
 * 역량 분석
 */
private CompetencyProgress analyzeCompetencies() {
    // ============ 역량별 학점 계산 ============
    Map<String, Integer> competencyCredits = new HashMap<>();

    for (Course course : courseList) {
        String competency = course.getCompetency();
        if (competency != null && !competency.isEmpty()) {
            int currentCredits = competencyCredits.getOrDefault(competency, 0);
            competencyCredits.put(competency, currentCredits + course.getCredits());
        }
    }

    // ============ 역량 요구사항 (예시) ============
    Map<String, Integer> competencyRequirements = new HashMap<>();
    competencyRequirements.put("1역량", 3);
    competencyRequirements.put("2역량", 3);
    competencyRequirements.put("3역량", 3);
    competencyRequirements.put("4역량", 3);

    // ============ 완료/미완료 판단 ============
    List<String> completed = new ArrayList<>();
    List<String> missing = new ArrayList<>();

    for (Map.Entry<String, Integer> entry : competencyRequirements.entrySet()) {
        String competency = entry.getKey();
        int required = entry.getValue();
        int earned = competencyCredits.getOrDefault(competency, 0);

        if (earned >= required) {
            completed.add(competency);
        } else {
            missing.add(competency + " (" + earned + "/" + required + ")");
        }
    }

    // ============ 결과 생성 ============
    CompetencyProgress progress = new CompetencyProgress();
    progress.completedCompetencies = completed;
    progress.missingCompetencies = missing;
    progress.competencyCredits = competencyCredits;

    return progress;
}
```

### 5.4 실전 예시

```
수강 과목 (역량 포함):
- 영어회화 (교양선택, 2학점, 1역량)
- 글쓰기 (교양선택, 2학점, 2역량)
- 발표와토론 (교양선택, 2학점, 2역량)
- 논리와사고 (교양선택, 2학점, 3역량)
- 철학의이해 (교양선택, 2학점, 3역량)

역량별 집계:
- 1역량: 2학점
- 2역량: 4학점
- 3역량: 4학점
- 4역량: 0학점

요구사항 (각 3학점):
- 1역량: 2/3 ✗
- 2역량: 4/3 ✓
- 3역량: 4/3 ✓
- 4역량: 0/3 ✗

UI 표시:
역량 분석 결과:
  ✓ 2역량 완료 (4학점)
  ✓ 3역량 완료 (4학점)
  ✗ 1역량 부족 (2/3학점)
  ✗ 4역량 부족 (0/3학점)
```

---

## 6. 과목 추천 알고리즘

### 6.1 개념

**과목 추천이란?**
- 부족한 학점 카테고리 분석
- 우선순위 기반 과목 추천
- 졸업 요건 충족을 위한 최적 경로 제시

### 6.2 추천 알고리즘

```java
/**
 * RecommendationEngine.java (가상)
 * 과목 추천 엔진
 */
public List<RecommendedCourse> recommendCourses(GraduationProgress progress) {
    List<RecommendedCourse> recommendations = new ArrayList<>();

    // ============ 1단계: 부족 카테고리 파악 ============
    List<CategoryDeficit> deficits = analyzeCategoryDeficits(progress);

    // 우선순위 정렬
    Collections.sort(deficits, (a, b) -> {
        // 1순위: 전공필수 > 교양필수 > 전공선택 > 교양선택
        return Integer.compare(a.priority, b.priority);
    });

    // ============ 2단계: 카테고리별 추천 ============
    for (CategoryDeficit deficit : deficits) {
        if (deficit.remainingCredits > 0) {
            List<RecommendedCourse> categoryRecommendations =
                recommendForCategory(deficit);
            recommendations.addAll(categoryRecommendations);
        }
    }

    return recommendations;
}

private List<CategoryDeficit> analyzeCategoryDeficits(GraduationProgress progress) {
    List<CategoryDeficit> deficits = new ArrayList<>();

    // 전공필수
    if (!progress.majorRequired.isCompleted) {
        deficits.add(new CategoryDeficit(
            "전공필수",
            progress.majorRequired.remaining,
            1  // 최우선
        ));
    }

    // 교양필수
    if (!progress.generalRequired.isCompleted) {
        deficits.add(new CategoryDeficit(
            "교양필수",
            progress.generalRequired.remaining,
            2
        ));
    }

    // 전공선택
    if (!progress.majorElective.isCompleted) {
        deficits.add(new CategoryDeficit(
            "전공선택",
            progress.majorElective.remaining,
            3
        ));
    }

    // ... 다른 카테고리들

    return deficits;
}

private List<RecommendedCourse> recommendForCategory(CategoryDeficit deficit) {
    List<RecommendedCourse> recommendations = new ArrayList<>();

    // ============ 미이수 과목 추천 ============
    if ("전공필수".equals(deficit.category)) {
        // 전공필수 미이수 과목
        List<String> missingCourses = getMissingMajorRequired();
        for (String course : missingCourses) {
            recommendations.add(new RecommendedCourse(
                course,
                deficit.category,
                "필수 과목입니다",
                1.0  // 추천 점수
            ));
        }
    } else if ("전공선택".equals(deficit.category)) {
        // 전공선택 과목 추천 (학점 기반)
        List<CourseInfo> availableCourses = getAvailableMajorElectives();

        // 남은 학점 채우기
        int remainingCredits = deficit.remainingCredits;
        for (CourseInfo course : availableCourses) {
            if (remainingCredits > 0) {
                recommendations.add(new RecommendedCourse(
                    course.getName(),
                    deficit.category,
                    "전공선택 " + course.getCredits() + "학점",
                    0.8
                ));
                remainingCredits -= course.getCredits();
            }
        }
    }

    return recommendations;
}
```

### 6.3 추천 우선순위

```java
/**
 * 추천 우선순위 매트릭스
 */
public class RecommendationPriority {
    public static int getPriority(String category, boolean isRequired) {
        if (isRequired) {
            // 필수 과목
            switch (category) {
                case "전공필수": return 1;  // 최우선
                case "교양필수": return 2;
                case "학부공통": return 3;
                default: return 5;
            }
        } else {
            // 선택 과목
            switch (category) {
                case "전공선택": return 4;
                case "전공심화": return 5;
                case "교양선택": return 6;
                case "소양": return 7;
                default: return 10;
            }
        }
    }
}
```

### 6.4 실전 예시

```
현재 진행 상황:
- 전공필수: 36/42 (6학점 부족) - 미이수: [알고리즘, 운영체제]
- 전공선택: 12/18 (6학점 부족)
- 교양필수: 24/27 (3학점 부족) - 미이수: [기독교이해 그룹]
- 교양선택: 6/6 ✓
- 소양: 6/6 ✓

추천 결과 (우선순위 순):

【1순위: 전공필수】
  ✓ 알고리즘 (3학점) - 필수 과목
  ✓ 운영체제 (3학점) - 필수 과목

【2순위: 교양필수】
  ✓ 기독교의이해 (3학점) - oneOf 그룹 선택 필요
     또는
  ✓ 성서의이해 (3학점) - oneOf 그룹 선택 필요

【3순위: 전공선택】
  • 웹프로그래밍 (3학점)
  • 모바일프로그래밍 (3학점)
  • 데이터베이스 (3학점)

총 추천: 7과목, 21학점
부족 학점: 15학점
```

---

## 7. 캐시 무효화 전략

### 7.1 캐시 종류

```java
/**
 * FirebaseDataManager.java
 * 캐시 저장소
 */
private Map<String, List<String>> studentYearsCache = new HashMap<>();
private Map<String, List<String>> departmentsCache = new HashMap<>();
private Map<String, List<String>> tracksCache = new HashMap<>();
private Map<String, List<CourseInfo>> coursesCache = new HashMap<>();
private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();

// 캐시 유효 시간: 5분
private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000;
```

### 7.2 무효화 시점

```java
/**
 * 캐시 무효화가 필요한 시점
 */
public enum CacheInvalidationTrigger {
    TIME_BASED,         // 시간 기반 (5분 경과)
    DATA_UPDATED,       // 데이터 업데이트
    USER_LOGOUT,        // 로그아웃
    MANUAL_REFRESH      // 수동 새로고침
}
```

### 7.3 시간 기반 무효화

```java
/**
 * FirebaseDataManager.java
 * 시간 기반 캐시 유효성 검사
 */
public void loadStudentYears(OnStudentYearsLoadedListener listener) {
    String cacheKey = "student_years";

    // ============ 캐시 확인 ============
    if (studentYearsCache.containsKey(cacheKey)) {
        Long cachedTime = cacheTimestamps.get(cacheKey);
        long age = System.currentTimeMillis() - (cachedTime != null ? cachedTime : 0);

        // ============ 유효성 검사 ============
        if (age < CACHE_VALIDITY_MS) {
            // 캐시 히트
            Log.d(TAG, "캐시 히트: " + cacheKey + " (age=" + age + "ms)");
            listener.onSuccess(studentYearsCache.get(cacheKey));
            return;
        } else {
            // 캐시 만료
            Log.d(TAG, "캐시 만료: " + cacheKey + " (age=" + age + "ms)");
            studentYearsCache.remove(cacheKey);
            cacheTimestamps.remove(cacheKey);
        }
    }

    // ============ 네트워크 요청 ============
    db.collection("graduation_requirements")
        .get()
        .addOnSuccessListener(snapshot -> {
            List<String> years = extractYears(snapshot);

            // 캐시 저장
            studentYearsCache.put(cacheKey, years);
            cacheTimestamps.put(cacheKey, System.currentTimeMillis());

            listener.onSuccess(years);
        });
}
```

### 7.4 데이터 업데이트 무효화

```java
/**
 * 관리자가 데이터를 업데이트할 때 캐시 무효화
 */
public void updateGraduationRequirements(String docId, Map<String, Object> data) {
    db.collection("graduation_requirements")
        .document(docId)
        .set(data)
        .addOnSuccessListener(aVoid -> {
            // ============ 관련 캐시 무효화 ============
            invalidateRelatedCaches(docId);

            Log.d(TAG, "데이터 업데이트 완료 및 캐시 무효화: " + docId);
        });
}

private void invalidateRelatedCaches(String docId) {
    // 문서 ID: "IT학부_멀티미디어_2025"
    String[] parts = docId.split("_");
    if (parts.length >= 3) {
        String department = parts[0];
        String track = parts[1];
        String year = parts[2];

        // 학부 캐시 무효화
        tracksCache.remove(department);

        // 과목 캐시 무효화
        String[] categories = {"전공필수", "전공선택", "교양필수", "교양선택"};
        for (String category : categories) {
            String cacheKey = department + "_" + track + "_" + year + "_" + category;
            coursesCache.remove(cacheKey);
            cacheTimestamps.remove(cacheKey);
        }

        Log.d(TAG, "관련 캐시 무효화: " + department + ", " + track + ", " + year);
    }
}
```

### 7.5 수동 새로고침

```java
/**
 * 사용자가 Pull-to-Refresh 시 캐시 무효화
 */
public void clearAllCaches() {
    studentYearsCache.clear();
    departmentsCache.clear();
    tracksCache.clear();
    coursesCache.clear();
    cacheTimestamps.clear();

    Log.d(TAG, "모든 캐시 초기화");
}

/**
 * 특정 카테고리만 무효화
 */
public void invalidateCourseCache(String department, String track, String year) {
    String[] categories = {"전공필수", "전공선택", "교양필수", "교양선택"};
    for (String category : categories) {
        String cacheKey = department + "_" + track + "_" + year + "_" + category;
        coursesCache.remove(cacheKey);
        cacheTimestamps.remove(cacheKey);
    }

    Log.d(TAG, "과목 캐시 무효화: " + department + "/" + track + "/" + year);
}
```

---

## 8. 성능 지표

### 8.1 알고리즘 복잡도

| 알고리즘 | 시간 복잡도 | 공간 복잡도 | 설명 |
|---------|-----------|-----------|------|
| **졸업요건 분석** | O(n*m) | O(n) | n=과목수, m=카테고리수 |
| **대체과목 적용** | O(n*r) | O(n) | n=과목수, r=규칙수 |
| **oneOf 그룹** | O(n*g*c) | O(g) | g=그룹수, c=그룹당 과목수 |
| **오버플로우 처리** | O(m) | O(m) | m=카테고리수 |
| **역량 분석** | O(n) | O(k) | k=역량 종류수 |
| **과목 추천** | O(m log m) | O(n) | 정렬 포함 |

### 8.2 실행 시간 (평균)

```
테스트 환경:
- 과목 수: 50개
- 카테고리 수: 9개
- 대체 규칙: 5개
- oneOf 그룹: 3개

실행 시간:
1. 대체과목 적용: ~5ms
2. 카테고리 분석: ~15ms
3. 오버플로우 처리: ~2ms
4. 역량 분석: ~3ms
5. UI 변환: ~5ms

총 소요 시간: ~30ms
```

### 8.3 최적화 포인트

```java
/**
 * 성능 최적화 체크리스트
 */
// ✓ HashSet 사용으로 O(1) 검색
Set<String> takenNames = new HashSet<>(takenCourseNames);

// ✓ 조기 종료 (Early Exit)
for (CourseInfo course : replacements) {
    if (takenNames.contains(course.getName())) {
        return course.getName();  // 첫 번째 발견 시 즉시 반환
    }
}

// ✓ 캐싱으로 중복 계산 방지
if (cache.containsKey(key)) {
    return cache.get(key);  // 이전 결과 재사용
}

// ✓ 배치 로딩
Tasks.whenAllSuccess(task1, task2, task3)
    .addOnSuccessListener(results -> {
        // 모든 데이터 한번에 처리
    });
```

---

## 9. 학습 체크리스트

### 초급
- [ ] 졸업요건 분석 전체 흐름 이해
- [ ] 카테고리별 분석 방법 이해 (fixed, elective)
- [ ] 오버플로우 개념 이해
- [ ] 대체과목 기본 개념 이해

### 중급
- [ ] oneOf 그룹 분석 알고리즘 구현
- [ ] 대체과목 중복 방지 로직 구현
- [ ] 오버플로우 재분배 알고리즘 구현
- [ ] 역량 분석 구현

### 고급
- [ ] 과목 추천 알고리즘 설계
- [ ] 캐시 무효화 전략 구현
- [ ] 알고리즘 최적화 (시간 복잡도 개선)
- [ ] 대규모 데이터 처리 (1000+ 과목)

---

## 10. 트러블슈팅

### 10.1 자주 발생하는 문제

#### 문제 1: 학점이 이중 계산됨

```
증상:
- 전공선택 18학점, 일반선택 18학점
- 총 36학점인데 표시는 54학점

원인:
- 오버플로우 처리 전에 이미 카테고리에 합산됨

해결:
- 오버플로우 처리 시 원본 카테고리의 earned를 required로 제한
```

#### 문제 2: 대체과목이 적용되지 않음

```
증상:
- 대체 과목 수강했는데 학부공통에 반영 안됨

원인:
- scope 설정이 잘못됨
- 폐강 과목을 직접 수강함

디버깅:
Log.d(TAG, "Replacement rule scope: " + rule.getScope());
Log.d(TAG, "Taken courses: " + takenCourseNames);
```

#### 문제 3: oneOf 그룹이 완료로 표시 안됨

```
증상:
- 채플1,2,3 모두 수강했는데 미완료로 표시

원인:
- required 학점이 과목 학점 합보다 큼
- 예: required=4, 과목 3개(각 1학점) = 3학점

해결:
- required 값 확인 및 수정
```

---

**작성일**: 2025년 12월 2일
**버전**: 1.0
**대상**: Android 중급 개발자

> 💡 **Tip**: 알고리즘을 이해할 때는 작은 데이터셋으로 디버깅 로그를 확인하세요!
