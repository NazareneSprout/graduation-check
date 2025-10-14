# 통합 졸업요건 시스템 스키마 설계

## 목표
- 모든 하드코딩 제거
- 관리자가 Firebase에서 모든 규칙을 수정 가능
- 사용자 졸업요건 검사가 Firebase 데이터만으로 완전 동작

## Firestore 컬렉션 구조

### 1. `graduation_requirements_v2` (통합 졸업요건 문서)

각 문서는 특정 학번/학과/트랙의 졸업요건을 정의합니다.

```json
{
  "docId": "2020_IT학부_인공지능",
  "cohort": "2020",
  "department": "IT학부",
  "track": "인공지능",
  "version": "v2.0",
  "updatedAt": "2025-10-14T10:00:00Z",

  // 총 이수학점 요건
  "creditRequirements": {
    "total": 127,
    "전공필수": 24,
    "전공선택": 21,
    "교양필수": 18,
    "교양선택": 15,
    "소양": 3,
    "학부공통": 36,  // 20-22학번만
    "일반선택": 10,  // 20-22학번만 (자율선택)
    "전공심화": 0,   // 23-25학번만
    "잔여학점": 0    // 23-25학번만 (자율선택)
  },

  // 넘치는 학점의 목적지
  "overflowDestination": "일반선택",  // 또는 "잔여학점"

  // 카테고리별 요구사항
  "categories": [
    {
      "id": "major_required",
      "name": "전공필수",
      "displayName": "전공필수",
      "type": "list",  // list, oneOf, group
      "required": 24,
      "requiredType": "credits",  // credits, courses, any
      "courses": [
        {
          "name": "이산구조",
          "credits": 3,
          "semester": "1-2",
          "mandatory": true
        },
        {
          "name": "객체지향프로그래밍",
          "credits": 3,
          "semester": "1-2",
          "mandatory": true
        }
        // ... 더 많은 과목
      ]
    },
    {
      "id": "major_elective",
      "name": "전공선택",
      "displayName": "전공선택",
      "type": "list",
      "required": 21,
      "requiredType": "credits",
      "courses": [
        {
          "name": "멀티미디어이론및실습",
          "credits": 3,
          "semester": "2-1"
        }
        // ... 더 많은 과목
      ]
    },
    {
      "id": "department_common",
      "name": "학부공통",
      "displayName": "학부공통",
      "type": "list",
      "required": 36,
      "requiredType": "credits",
      "courses": [
        {
          "name": "IT개론",
          "credits": 3,
          "semester": "1-1",
          "mandatory": true
        },
        {
          "name": "컴퓨터프로그래밍1",
          "credits": 3,
          "semester": "1-1",
          "mandatory": true
        }
        // ... 더 많은 과목
      ]
    },
    {
      "id": "general_required",
      "name": "교양필수",
      "displayName": "교양필수",
      "type": "group",  // 하위에 여러 그룹 포함
      "required": 18,
      "requiredType": "credits",
      "subgroups": [
        {
          "id": "career_planning",
          "name": "생애설계와진로",
          "type": "oneOf",  // 중 하나만 선택
          "required": 1,
          "requiredType": "courses",
          "courses": [
            {"name": "생애설계와직업진로탐색", "credits": 1},
            {"name": "생애설계와직업진로1", "credits": 1},
            {"name": "자기주도취업과창업", "credits": 1}
          ]
        },
        {
          "id": "english",
          "name": "영어",
          "type": "list",
          "required": 4,
          "requiredType": "credits",
          "courses": [
            {"name": "Practical English1", "credits": 2, "mandatory": true},
            {"name": "Practical English2", "credits": 2, "mandatory": true}
          ]
        },
        {
          "id": "basic_skills",
          "name": "기본소양",
          "type": "oneOf",
          "required": 2,
          "requiredType": "credits",
          "courses": [
            {"name": "발표와토론", "credits": 2},
            {"name": "논리적사고와글쓰기", "credits": 2},
            {"name": "논리와비판적사고", "credits": 2}
          ]
        },
        {
          "id": "computer_literacy",
          "name": "컴퓨터활용",
          "type": "oneOf",
          "required": 2,
          "requiredType": "credits",
          "courses": [
            {"name": "정보사회와컴퓨터", "credits": 2},
            {"name": "컴퓨터코딩이해하기", "credits": 2}
          ]
        },
        {
          "id": "christianity",
          "name": "기독교",
          "type": "oneOf",
          "required": 2,
          "requiredType": "credits",
          "courses": [
            {"name": "성서와인간", "credits": 2},
            {"name": "하나님과세상", "credits": 2},
            {"name": "기독교와사회", "credits": 2},
            {"name": "기독교역사", "credits": 2},
            {"name": "인물로 보는 기독교", "credits": 2}
          ]
        },
        {
          "id": "etc_required",
          "name": "기타필수",
          "type": "list",
          "required": 4,
          "requiredType": "credits",
          "courses": [
            {"name": "나눔리더십", "credits": 1, "mandatory": true},
            {"name": "나눔실천", "credits": 1, "mandatory": true},
            {"name": "장애인의이해", "credits": 2, "mandatory": true}
          ]
        }
      ]
    },
    {
      "id": "general_elective",
      "name": "교양선택",
      "displayName": "교양선택",
      "type": "competency",  // 역량 기반 선택
      "required": 15,
      "requiredType": "credits",
      "competencies": [
        "창의융합",
        "글로벌소통",
        "공감나눔",
        "문제해결",
        "자기주도"
      ]
    },
    {
      "id": "liberal_arts",
      "name": "소양",
      "displayName": "소양",
      "type": "list",
      "required": 3,
      "requiredType": "credits",
      "courses": []  // 학생이 자유롭게 수강
    }
  ],

  // 대체과목 규칙
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
        },
        {
          "name": "멀티미디어이론및실습",
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

## Java 모델 클래스 구조

### 1. GraduationRules.java (최상위)
```java
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
}
```

### 2. RequirementCategory.java
```java
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

    // 분석 메서드
    public CategoryAnalysisResult analyze(List<CourseInputActivity.Course> takenCourses);
}
```

### 3. CourseRequirement.java
```java
public class CourseRequirement {
    private String name;
    private int credits;
    private String semester;
    private boolean mandatory;
}
```

### 4. ReplacementRule.java
```java
public class ReplacementRule {
    private CourseInfo discontinuedCourse;
    private List<CourseInfo> replacementCourses;
    private String note;
    private Timestamp createdAt;

    public static class CourseInfo {
        private String name;
        private String category;
        private int credits;
    }
}
```

### 5. CategoryAnalysisResult.java
```java
public class CategoryAnalysisResult {
    private String categoryId;
    private String categoryName;
    private int earnedCredits;
    private int requiredCredits;
    private int earnedCourses;
    private int requiredCourses;
    private boolean isCompleted;

    private List<String> completedCourses;
    private List<String> missingCourses;
    private List<SubgroupResult> subgroupResults;
}
```

## 데이터 마이그레이션 전략

### 기존 → 새로운 스키마

1. **graduation_requirements** → **graduation_requirements_v2**
   - creditRequirements 필드 그대로 복사
   - categories 필드 새로 생성

2. **major_documents** → **graduation_requirements_v2.categories[]**
   - rules 필드를 categories 배열로 변환

3. **education_documents** → **graduation_requirements_v2.categories[]**
   - 교양 그룹 구조를 subgroups로 변환

4. **replacement_courses** → **graduation_requirements_v2.replacementRules[]**
   - 독립 컬렉션 → 졸업요건 문서 내부로 통합

## 장점

1. ✅ **단일 문서 조회**로 모든 정보 획득
2. ✅ **하드코딩 완전 제거** - 모든 로직이 데이터 기반
3. ✅ **관리자 UI 단순화** - 하나의 문서만 편집
4. ✅ **일관성 보장** - 트랜잭션으로 원자적 업데이트
5. ✅ **버전 관리** - version 필드로 스키마 변경 추적
6. ✅ **확장성** - 새로운 학번/학과 추가가 문서 추가만으로 가능
7. ✅ **실시간 동기화** - Firestore 리스너로 즉시 반영

## 구현 우선순위

### High Priority
1. GraduationRules 클래스 구현
2. RequirementCategory 클래스 구현
3. FirebaseDataManager.loadGraduationRules() 구현
4. GraduationAnalysisResultActivity 리팩토링

### Medium Priority
5. 관리자 통합 편집 UI
6. 데이터 마이그레이션 스크립트

### Low Priority
7. 학기별 과목 추천 (선택 사항)
