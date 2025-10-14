# 교양선택 파일 조회 네임스페이스 차트

## 전체 아키텍처 개요
```
[사용자] → [CourseInputActivity] → [FirebaseDataManager] → [Firebase Firestore]
```

## 네임스페이스 구조

### 1. UI Layer (sprout.app.sakmvp1)
```
CourseInputActivity
├── 탭 시스템
│   ├── tabGeneralElective ("교양선택" 탭)
│   └── switchTab("교양선택", tabGeneralElective)
│
├── 카테고리 선택 로직
│   ├── setupCategorySpinner()
│   │   └── categoryAdapter.add("교양선택")
│   │
│   └── 선택 처리
│       ├── "교양선택" 감지
│       ├── layoutGeneralManualInput.setVisibility(VISIBLE)
│       └── 일반 강의 목록 숨김
│
└── 수동 입력 시스템
    ├── addGeneralCourseFromManualInput()
    ├── setupCompetencySpinner() (역량 선택: 1-6역량)
    └── Course 객체 생성 (competency 필드 포함)
```

### 2. Data Layer (sprout.app.sakmvp1)
```
FirebaseDataManager
├── 교양선택 특수 처리
│   ├── loadCoursesForCategory()
│   │   └── "교양선택" → 로딩 생략 (수동 입력만)
│   │
│   └── CreditRequirements
│       ├── generalElective 필드
│       └── toString() 포함
│
└── Course 데이터 구조
    ├── category: "교양선택"
    ├── name: 사용자 입력
    ├── credits: 사용자 입력
    └── competency: "1역량"~"6역량"
```

### 3. Firebase Collection 구조
```
graduation_requirements/{department}_{track}_{year}
├── rules
│   ├── requirements[]
│   │   └── options[] (일반 강의 목록)
│   │
│   └── creditRequirements
│       └── generalElective: number (필요 학점)
│
└── graduation_meta/{department}_{year}
    └── 교양선택: number (기본값: 8학점)
```

## 교양선택 처리 플로우

### A. 탭 선택 플로우
```
1. 사용자가 "교양선택" 탭 클릭
   ↓
2. switchTab("교양선택", tabGeneralElective) 호출
   ↓
3. UI 상태 변경
   - currentSelectedTab = "교양선택"
   - 해당 탭 활성화
   ↓
4. 카테고리 스피너 업데이트
   - isMajorGroupSelected = false
   - categoryAdapter에 "교양선택" 추가
```

### B. 카테고리 선택 플로우
```
1. 스피너에서 "교양선택" 선택
   ↓
2. updateUIForCategorySelection() 호출
   ↓
3. "교양선택" 감지
   - layoutMajorCourses.setVisibility(GONE)
   - layoutGeneralManualInput.setVisibility(VISIBLE)
   - layoutManualInput.setVisibility(GONE)
   ↓
4. 일반 강의 자동 로딩 건너뛰기
   - loadCoursesForCategory() 호출되지 않음
   - 수동 입력 모드로 전환
```

### C. 강의 입력 플로우
```
1. 사용자 수동 입력
   - 강의명: EditText 입력
   - 학점: EditText 입력
   - 역량: Spinner 선택 (1-6역량)
   ↓
2. addGeneralCourseFromManualInput() 호출
   ↓
3. Course 객체 생성
   - category: "교양선택"
   - name: 사용자 입력값
   - credits: 사용자 입력값
   - competency: 선택된 역량
   ↓
4. courseList에 추가
   ↓
5. UI 업데이트
   - updateCourseDisplay()
   - updateAnalyzeButtonState()
   ↓
6. 성공 메시지 표시
   - "교양선택 강의가 추가되었습니다."
```

### D. 파일 조회 특수 처리
```
loadCoursesForCategory("교양선택")
├── 일반 카테고리와 달리 Firebase 조회 건너뛰기
├── Log: "교양선택/일반선택은 수동 입력으로 처리 — 로딩 생략"
├── hideLoadingMessage()
└── isLoadingCourses = false
```

## 주요 특징

### 1. 수동 입력 전용
- **일반 강의**: Firebase에서 자동 로딩
- **교양선택**: 수동 입력만 지원
- 이유: 교양선택은 다양한 강의 중 자유롭게 선택 가능

### 2. 역량 시스템
- 교양선택 전용 역량 분류 (1역량~6역량)
- setupCompetencySpinner()에서 관리
- Course 객체의 competency 필드에 저장

### 3. UI 분리
- **전공 강의**: layoutMajorCourses + 자동 로딩
- **교양선택**: layoutGeneralManualInput + 수동 입력
- 명확한 UI 상태 분리로 사용자 경험 최적화

### 4. 성능 최적화
- 교양선택 선택 시 불필요한 Firebase 조회 생략
- 즉시 수동 입력 모드로 전환
- In-Flight 요청 관리에서도 제외

## 데이터 흐름 요약

```
[사용자 입력]
    ↓
[CourseInputActivity.addGeneralCourseFromManualInput()]
    ↓
[Course 객체 생성 (category="교양선택", competency=선택값)]
    ↓
[courseList.add() + UI 업데이트]
    ↓
[졸업 분석에서 교양선택 학점으로 계산]
```

---

*생성일: 2025-09-29*
*교양선택 시스템은 수동 입력 기반으로 설계되어 사용자가 자유롭게 강의를 등록할 수 있습니다.*