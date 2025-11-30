# SakMvp1 아키텍처 요약

## 프로젝트 개요

**SakMvp1**은 대학생의 졸업 요건 분석 및 학사 관리를 지원하는 Android 네이티브 애플리케이션입니다.

- **언어**: Java 11
- **플랫폼**: Android (API 26-35)
- **빌드 시스템**: Gradle (Kotlin DSL)
- **백엔드**: Firebase (Firestore, Auth, Storage)
- **아키텍처 패턴**: 계층형 아키텍처 + Single Activity Pattern

---

## 아키텍처 다이어그램

### 전체 시스템 구조

```
┌───────────────────────────────────────────────────────────────┐
│                        Android Application                     │
│                           (SakMvp1)                           │
└───────────────────────────────────────────────────────────────┘
                                │
                ┌───────────────┼───────────────┐
                │               │               │
    ┌───────────▼───────┐ ┌────▼─────┐ ┌──────▼────────┐
    │ Presentation      │ │ Business │ │ Data Access   │
    │ Layer             │ │ Logic    │ │ Layer         │
    │                   │ │ Layer    │ │               │
    │ • Activities      │ │ • Models │ │ • Firestore   │
    │ • Fragments       │ │ • Rules  │ │ • Local       │
    │ • Adapters        │ │ • Utils  │ │   Storage     │
    └───────────────────┘ └──────────┘ └───────────────┘
                                │
                    ┌───────────┼───────────┐
                    │           │           │
            ┌───────▼────┐ ┌───▼────┐ ┌───▼────────┐
            │ Firebase   │ │Firebase│ │ Firebase   │
            │ Firestore  │ │ Auth   │ │ Storage    │
            └────────────┘ └────────┘ └────────────┘
```

### 계층별 상세 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER (UI)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  BaseActivity (공통 기능: 색약 모드, 접근성)                       │
│      │                                                           │
│      ├── LoginActivity ─────────────┐                            │
│      │                               │                           │
│      ├── MainActivityNew (컨테이너)  │  인증 플로우                │
│      │    └── BottomNavigationView  │                           │
│      │        ├── HomeFragment ──────┘                           │
│      │        ├── TimeTableFragment                              │
│      │        ├── CampusMapFragment                              │
│      │        └── UserProfileFragment                            │
│      │                                                           │
│      ├── GraduationAnalysisActivity ──┐                          │
│      │                                 │                         │
│      ├── AdditionalRequirementsActivity│  졸업 요건 분석 플로우     │
│      │                                 │                         │
│      ├── CourseInputActivity ──────────┤                         │
│      │                                 │                         │
│      └── GraduationAnalysisResultActivity                        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│               BUSINESS LOGIC LAYER (Domain Logic)                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  models/                                                         │
│    ├── GraduationRules                                           │
│    │    └── analyze(List<Course>): GraduationAnalysisResult     │
│    │                                                             │
│    ├── RequirementCategory                                       │
│    │    └── analyze(List<Course>): CategoryAnalysisResult       │
│    │                                                             │
│    ├── ReplacementRule                                           │
│    │    └── 대체과목 규칙 데이터 모델                              │
│    │                                                             │
│    └── [기타 도메인 모델들]                                        │
│                                                                  │
│  managers/                                                       │
│    └── CustomizedRequirementsManager                             │
│         └── 사용자 맞춤 요건 관리                                  │
│                                                                  │
│  utils/                                                          │
│    ├── GraduationRequirementUtils                                │
│    └── UiUtils                                                   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  DATA ACCESS LAYER (Data Management)             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  FirebaseDataManager (싱글톤)                                    │
│    ├── loadStudentYears()                                        │
│    ├── loadDepartments()                                         │
│    ├── loadTracksForDepartment()                                 │
│    ├── loadCoursesForCategory()                                  │
│    ├── loadGraduationRequirements()                              │
│    └── 캐싱 시스템:                                               │
│        ├── studentYearsCache                                     │
│        ├── departmentsCache                                      │
│        ├── tracksCache                                           │
│        ├── coursesCache                                          │
│        ├── graduationCache                                       │
│        └── docSnapshotCache (N+1 쿼리 방지)                       │
│                                                                  │
│  로컬 스토리지                                                     │
│    ├── UserDataManager (SharedPreferences)                       │
│    ├── CurrentTimetableStorage                                   │
│    └── TimetableLocalStorage                                     │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    EXTERNAL SERVICES (Cloud)                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Firebase Platform                                               │
│    ├── Cloud Firestore                                           │
│    │    ├── graduation_requirements                              │
│    │    ├── graduation_meta                                      │
│    │    ├── student_progress                                     │
│    │    ├── users                                                │
│    │    ├── banners                                              │
│    │    ├── certificates                                         │
│    │    └── documents                                            │
│    │                                                             │
│    ├── Firebase Authentication                                   │
│    │    ├── 이메일/비밀번호 인증                                   │
│    │    └── 세션 관리                                             │
│    │                                                             │
│    └── Firebase Storage                                          │
│         ├── 프로필 이미지                                          │
│         ├── 배너 이미지                                            │
│         └── 문서 파일                                              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 핵심 기능 플로우

### 1. 졸업 요건 분석 플로우

```
[사용자] → [GraduationAnalysisActivity]
              ↓ (학번, 학부, 트랙 선택)
         [AdditionalRequirementsActivity]
              ↓ (추가 요건 선택)
         [CourseInputActivity]
              ↓ (수강 과목 입력)
         [GraduationRules.analyze()]
              ├─ applyReplacementRules()
              ├─ category.analyze() (각 카테고리)
              ├─ calculateTotalCredits()
              ├─ handleOverflowCredits()
              └─ calculateGraduationReadiness()
              ↓
         [GraduationAnalysisResultActivity]
              └─ 결과 표시 (도넛 차트, 상세 분석)
```

### 2. 인증 및 권한 플로우

```
[앱 시작]
    ↓
[LoginActivity]
    ├─ 자동 로그인 체크
    │   └─ FirebaseAuth.getCurrentUser() != null?
    │       ├─ Yes → [checkUserRoleAndNavigate()]
    │       └─ No → 로그인 화면 표시
    ↓
[사용자 로그인]
    ├─ Firebase Authentication
    └─ Firestore users 조회
         ├─ isAdmin == true → [AdminActivity]
         └─ isAdmin == false → [MainActivityNew]
```

### 3. 데이터 로딩 플로우 (캐싱 포함)

```
[Activity] → [FirebaseDataManager.loadCourses()]
                 ↓
            [캐시 확인]
                 ├─ Cache HIT
                 │   └─ 즉시 반환 (0ms)
                 │
                 └─ Cache MISS
                     ↓
                 [Firestore 조회]
                     ↓
                 [문서 존재?]
                     ├─ Yes → [파싱 & 캐싱]
                     │         └─ callback.onSuccess()
                     │
                     └─ No → [Fallback 문서 시도]
                               └─ callback.onFailure()
```

---

## 주요 디자인 패턴

### 1. 싱글톤 패턴 (Singleton)

**FirebaseDataManager**가 싱글톤으로 구현되어 전역에서 동일한 인스턴스를 공유합니다.

```java
public class FirebaseDataManager {
    private static FirebaseDataManager instance;

    public static FirebaseDataManager getInstance() {
        if (instance == null) {
            synchronized (FirebaseDataManager.class) {
                if (instance == null) {
                    instance = new FirebaseDataManager();
                }
            }
        }
        return instance;
    }
}
```

**장점**:
- 메모리 효율성 (단일 인스턴스)
- 캐시 공유로 네트워크 요청 최소화
- 일관된 데이터 접근 보장

### 2. 콜백 패턴 (Callback)

비동기 작업의 결과를 처리하기 위해 콜백 인터페이스를 사용합니다.

```java
public interface LoadCallback {
    void onSuccess(Object data);
    void onFailure(String error);
}

// 사용 예시
dataManager.loadCourses(new LoadCallback() {
    @Override
    public void onSuccess(Object data) {
        // UI 업데이트
    }

    @Override
    public void onFailure(String error) {
        // 에러 처리
    }
});
```

### 3. 어댑터 패턴 (Adapter)

RecyclerView와 데이터를 연결하는 어댑터 패턴을 사용합니다.

```
데이터 모델 ──┐
             ├─→ [Adapter] ─→ RecyclerView ─→ 화면 표시
ViewHolder  ──┘
```

### 4. 템플릿 메서드 패턴 (Template Method)

**BaseActivity**가 공통 로직을 제공하고, 하위 Activity가 이를 상속받아 사용합니다.

```java
public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyAccessibilitySettings(); // 공통 로직
    }

    protected void applyAccessibilitySettings() {
        // 색약 모드 등 접근성 설정 적용
    }
}
```

---

## 데이터 구조

### Firestore 데이터 스키마

#### graduation_requirements 컬렉션

```javascript
graduation_requirements/{docId} {
    cohort: 2025,                    // 학번 (Long)
    department: "IT학부",             // 학부명
    track: "멀티미디어",              // 트랙명
    totalCredits: 130,               // 총 졸업 학점
    version: "1.0",
    updatedAt: Timestamp,

    creditRequirements: {            // 학점 요구사항
        major: 60,
        general: 30,
        total: 130
    },

    categories: [                    // 요건 카테고리 배열
        {
            id: "major_required",
            name: "전공필수",
            type: "major",
            required: 30,
            courses: [
                {
                    code: "CS101",
                    name: "컴퓨터프로그래밍",
                    credits: 3,
                    required: true
                },
                // ...
            ]
        },
        // ...
    ],

    replacementRules: [              // 대체과목 규칙
        {
            discontinuedCourseCode: "OLD101",
            discontinuedCourseName: "폐강과목명",
            replacementCourseCode: "NEW101",
            replacementCourseName: "대체과목명",
            replacementCategory: "major_required",
            scope: "document"
        },
        // ...
    ]
}
```

#### users 컬렉션

```javascript
users/{uid} {
    email: "student@example.com",
    name: "홍길동",
    studentId: "2025123456",
    department: "IT학부",
    track: "멀티미디어",
    cohort: "2025",
    isAdmin: false,

    accessibilitySettings: {
        colorBlindMode: false
    },

    createdAt: Timestamp,
    lastLogin: Timestamp
}
```

---

## 성능 최적화 전략

### 1. 다층 캐싱 전략

```
┌─────────────────────────────────────────────────┐
│  Level 1: 메모리 캐시 (Map<String, Object>)      │
│  - 5분 TTL                                      │
│  - 가장 빠름 (0-1ms)                             │
└─────────────────────────────────────────────────┘
                    ↓ (Cache Miss)
┌─────────────────────────────────────────────────┐
│  Level 2: DocumentSnapshot 캐시                 │
│  - N+1 쿼리 방지                                 │
│  - 중간 속도 (5-10ms)                            │
└─────────────────────────────────────────────────┘
                    ↓ (Cache Miss)
┌─────────────────────────────────────────────────┐
│  Level 3: Firestore 네트워크 조회                │
│  - 가장 느림 (100-500ms)                         │
│  - 조회 후 상위 캐시에 저장                       │
└─────────────────────────────────────────────────┘
```

### 2. N+1 쿼리 문제 해결

**문제**: 동일한 문서를 여러 번 조회하면서 불필요한 네트워크 요청 발생

**해결책**: DocumentSnapshot 캐싱

```java
// Before (N+1 문제 발생)
for (String category : categories) {
    // 매번 Firestore 조회 (N회)
    firestore.document(docId).get()...
}

// After (캐시 사용)
DocumentSnapshot cached = docSnapshotCache.get(docId);
if (cached != null) {
    // 캐시에서 즉시 반환 (0회 네트워크 요청)
    return cached;
}
```

**효과**:
- 네트워크 요청: N회 → 1회
- 응답 시간: 500ms × N → 5ms × N
- 비용 절감: Firestore 읽기 비용 대폭 감소

### 3. 병렬 데이터 로딩

```java
// 학번, 학부, 트랙 데이터를 동시에 로드
CompletableFuture.allOf(
    loadStudentYearsAsync(),
    loadDepartmentsAsync(),
    loadAllTracksAsync()
).thenRun(() -> {
    hideLoadingDialog();
});
```

**효과**: 순차 로딩 대비 2-3배 빠른 초기 로딩

---

## 보안 고려사항

### 1. Firebase Security Rules (개념적)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // 인증된 사용자만 읽기
    match /graduation_requirements/{document} {
      allow read: if request.auth != null;
      allow write: if isAdmin();
    }

    // 본인 데이터만 접근
    match /student_progress/{userId} {
      allow read, write: if request.auth.uid == userId;
    }

    // 관리자 전용
    match /users/{document} {
      allow read, write: if isAdmin();
    }

    function isAdmin() {
      return get(/databases/$(database)/documents/users/$(request.auth.uid))
             .data.isAdmin == true;
    }
  }
}
```

### 2. 클라이언트 권한 검증

```java
// AdminActivity 진입 시 권한 확인
if (!isAdmin()) {
    Toast.makeText(this, "관리자 권한이 필요합니다", LENGTH_SHORT).show();
    finish();
    return;
}
```

### 3. 민감 정보 처리

- **ProGuard/R8**: Release 빌드 시 코드 난독화 활성화
- **API 키 보호**: google-services.json을 .gitignore에 추가
- **SharedPreferences**: 민감 정보는 EncryptedSharedPreferences 사용 권장

---

## 테스트 전략 (계획)

### 단위 테스트 (Unit Tests)

```
tests/
├── models/
│   ├── GraduationRulesTest.java
│   │   ├── testAnalyzeWithEmptyCourses()
│   │   ├── testAnalyzeWithCompleteCourses()
│   │   ├── testReplacementRuleApplication()
│   │   └── testOverflowCreditsHandling()
│   │
│   └── RequirementCategoryTest.java
│       ├── testAnalyzeRequiredCategory()
│       └── testAnalyzeElectiveCategory()
│
└── utils/
    └── GraduationRequirementUtilsTest.java
```

### 통합 테스트 (Integration Tests)

```
androidTest/
└── GraduationAnalysisFlowTest.java
    ├── testCompleteAnalysisFlow()
    ├── testCourseInputAndAnalysis()
    └── testRecommendationGeneration()
```

---

## 빌드 및 배포

### Gradle 빌드 설정

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true          // 코드 축소
        isShrinkResources = true        // 리소스 축소
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }

    debug {
        isMinifyEnabled = false
        isDebuggable = true
        applicationIdSuffix = ".debug"
    }
}
```

### 버전 관리

```kotlin
defaultConfig {
    applicationId = "sprout.app.sakmvp1"
    minSdk = 26
    targetSdk = 35
    versionCode = 3        // Google Play 내부 버전
    versionName = "1.0"    // 사용자에게 표시되는 버전
}
```

---

## 주요 의존성

| 카테고리 | 라이브러리 | 버전 | 용도 |
|---------|-----------|------|------|
| UI | Material Components | - | Material Design 구현 |
| 네트워크 | OkHttp | 4.12.0 | HTTP 클라이언트 |
| 네트워크 | Jsoup | 1.17.2 | HTML 파싱 (식단) |
| 데이터베이스 | Firebase Firestore | 33.6.0 | NoSQL DB |
| 인증 | Firebase Auth | 33.6.0 | 사용자 인증 |
| 스토리지 | Firebase Storage | 33.6.0 | 파일 저장 |
| 이미지 | Glide | 4.16.0 | 이미지 로딩 |
| JSON | Gson | 2.10.1 | JSON 처리 |
| PDF | iText7 | 7.2.5 | PDF 생성 |
| 지도 | OSMDroid | 6.1.18 | OpenStreetMap |

---

## 향후 개선 방향

### 아키텍처 현대화

1. **MVVM + Repository 패턴 도입**
   - ViewModel로 UI와 비즈니스 로직 분리
   - LiveData/StateFlow로 반응형 UI 구현
   - Repository 패턴으로 데이터 소스 추상화

2. **Dependency Injection**
   - Hilt/Dagger로 의존성 관리
   - 테스트 용이성 향상

3. **Kotlin 마이그레이션**
   - Null Safety
   - 간결한 문법
   - Coroutines/Flow

### 새로운 기능

1. **오프라인 지원**
   - Room Database로 로컬 캐싱
   - WorkManager로 백그라운드 동기화

2. **푸시 알림**
   - FCM으로 학사 일정 알림
   - 졸업 요건 변경 알림

3. **다국어 지원**
   - 한국어/영어 전환
   - 리소스 국제화

---

## 문서 참조

- **상세 아키텍처 문서**: [SOFTWARE_ARCHITECTURE.md](SOFTWARE_ARCHITECTURE.md)
- **NS 차트**: [NS_CHARTS.md](NS_CHARTS.md)
- **프로젝트 가이드**: [CLAUDE.md](CLAUDE.md)

---

## 연락처

- **프로젝트**: SakMvp1 (Student Academic Knowledge Management v1)
- **작성일**: 2025-11-25
- **버전**: 1.0
