# SakMvp1 소프트웨어 아키텍처 문서

## 프로젝트 개요

**프로젝트명**: SakMvp1 (Student Academic Knowledge Management MVP v1)
**애플리케이션 타입**: Android Native Application (Java)
**주요 기능**: 대학생 졸업요건 분석 및 학사 관리 시스템
**개발 환경**: Android Studio, Gradle, Firebase
**타겟 SDK**: Android 11 (API 26) ~ Android 15 (API 35)

---

## 1. 아키텍처 개요

### 1.1 아키텍처 패턴

SakMvp1은 **계층형 아키텍처(Layered Architecture)**와 **Single Activity + Multiple Fragments 패턴**을 결합한 구조입니다.

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│  (Activities, Fragments, Adapters, UI Components)           │
├─────────────────────────────────────────────────────────────┤
│                      Business Logic Layer                    │
│  (Managers, Models, Analyzers, Rules)                       │
├─────────────────────────────────────────────────────────────┤
│                      Data Access Layer                       │
│  (FirebaseDataManager, UserDataManager, LocalStorage)       │
├─────────────────────────────────────────────────────────────┤
│                      External Services                       │
│  (Firebase Firestore, Firebase Auth, Firebase Storage)      │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 핵심 설계 원칙

1. **단일 책임 원칙 (SRP)**: 각 클래스는 하나의 명확한 책임만 가짐
2. **싱글톤 패턴**: FirebaseDataManager는 전역 싱글톤으로 데이터 접근 통합
3. **캐싱 전략**: 5분 TTL 메모리 캐시로 네트워크 요청 최소화
4. **관심사 분리**: UI, 비즈니스 로직, 데이터 접근이 명확하게 분리
5. **BaseActivity 상속**: 공통 기능(색약 모드 등)을 BaseActivity에서 관리

---

## 2. 계층별 상세 아키텍처

### 2.1 Presentation Layer (프레젠테이션 계층)

사용자 인터페이스와 사용자 상호작용을 담당하는 계층입니다.

#### 2.1.1 Activity 구조

```
BaseActivity (추상 베이스)
    ├── LoginActivity (로그인)
    ├── SignUpActivity (회원가입)
    ├── MainActivityNew (메인 컨테이너)
    │   └── BottomNavigationView
    │       ├── HomeFragment
    │       ├── TimeTableFragment
    │       ├── CampusMapFragment
    │       └── UserProfileFragment
    ├── GraduationAnalysisActivity (졸업 분석 진입)
    ├── AdditionalRequirementsActivity (추가 요건 선택)
    ├── CourseInputActivity (과목 입력)
    ├── GraduationAnalysisResultActivity (분석 결과)
    ├── CourseRecommendationActivity (과목 추천)
    ├── AdminActivity (관리자 대시보드)
    │   ├── GraduationRequirementsActivity (졸업 요건 관리)
    │   ├── StudentDataActivity (학생 데이터 조회)
    │   ├── BannerManagementActivity (배너 관리)
    │   ├── CertificateManagementActivity (자격증 관리)
    │   └── DocumentManageActivity (서류 관리)
    └── [기타 유틸리티 Activity들]
```

#### 2.1.2 Fragment 구조

| Fragment | 역할 | 포함 화면 |
|---------|------|----------|
| **HomeFragment** | 홈 대시보드 | 배너, 바로가기, 공지사항 |
| **TimeTableFragment** | 시간표 관리 | 주간/월간 시간표, 일정 추가 |
| **CampusMapFragment** | 캠퍼스 지도 | OSM 기반 캠퍼스 맵 |
| **UserProfileFragment** | 내정보 | 프로필, 북마크, 설정 |
| **ChecklistFragment** | 체크리스트 | 할 일 목록 관리 |

#### 2.1.3 RecyclerView Adapter 구조

```
adapters/
    ├── BannerAdapter - 배너 슬라이더
    ├── CourseEditAdapter - 과목 편집 리스트
    ├── GeneralCourseGroupAdapter - 교양 과목 그룹
    ├── GraduationRequirementPagerAdapter - 졸업 요건 탭
    ├── ReplacementRuleEditAdapter - 대체 과목 규칙 편집
    ├── RecommendedCourseAdapter - 추천 과목 리스트
    ├── StudentAdapter - 학생 목록
    ├── CertificateAdapter - 자격증 목록
    └── [타임테이블 관련 Adapter들]
```

---

### 2.2 Business Logic Layer (비즈니스 로직 계층)

애플리케이션의 핵심 로직을 담당하는 계층입니다.

#### 2.2.1 모델 클래스 (models/)

```
models/
    ├── GraduationRules.java
    │   └── analyze() - 졸업 요건 분석 엔진
    ├── GraduationAnalysisResult.java
    │   └── 분석 결과 데이터 클래스
    ├── CategoryAnalysisResult.java
    │   └── 카테고리별 분석 결과
    ├── RequirementCategory.java
    │   └── 요건 카테고리 (전공필수, 교양 등)
    ├── ReplacementRule.java
    │   └── 대체과목 규칙
    ├── CreditRequirements.java
    │   └── 학점 요구사항
    ├── CourseRequirement.java
    │   └── 과목별 요구사항
    ├── Student.java
    │   └── 학생 정보 모델
    └── UserCustomizedRequirements.java
        └── 사용자 맞춤 요건
```

#### 2.2.2 매니저 클래스 (managers/)

```
managers/
    └── CustomizedRequirementsManager.java
        ├── loadCustomizedRequirements() - 맞춤 요건 로드
        ├── saveCustomizedRequirements() - 맞춤 요건 저장
        └── clearCustomizedRequirements() - 맞춤 요건 초기화
```

#### 2.2.3 유틸리티 클래스 (utils/)

```
utils/
    ├── GraduationRequirementUtils.java
    │   └── 졸업 요건 관련 유틸리티 함수
    └── UiUtils.java
        └── UI 관련 유틸리티 함수
```

---

### 2.3 Data Access Layer (데이터 접근 계층)

데이터베이스 및 로컬 스토리지 접근을 담당하는 계층입니다.

#### 2.3.1 FirebaseDataManager (싱글톤)

**역할**: Firestore 데이터 접근 중앙 관리

**주요 메서드**:
```java
// 싱글톤 인스턴스
public static FirebaseDataManager getInstance()

// 학번/학부/트랙 데이터 로드
public void loadStudentYears(LoadCallback callback)
public void loadDepartments(LoadCallback callback)
public void loadTracksForDepartment(String department, LoadCallback callback)

// 강의 데이터 로드
public void loadCoursesForCategory(String dept, String track, String year,
                                   String category, LoadCallback callback)

// 졸업 요건 데이터 로드
public void loadGraduationRequirements(String dept, String track, String year,
                                       LoadCallback callback)

// 캐시 관리
public void clearCache()
public void clearDocSnapshotCache()
```

**캐싱 전략**:
- `studentYearsCache`: 학번 목록 캐시
- `departmentsCache`: 학부 목록 캐시
- `tracksCache`: 학부별 트랙 캐시
- `coursesCache`: 강의 정보 캐시 (Key: "학부_트랙_연도_카테고리")
- `graduationCache`: 졸업 요건 원시 데이터 캐시
- `docSnapshotCache`: DocumentSnapshot 캐시 (N+1 쿼리 방지)

**성능 최적화**:
- 5분 TTL 메모리 캐시
- N+1 쿼리 해결 (DocumentSnapshot 캐싱)
- Single-flight 패턴 (중복 요청 병합)
- 학번/학부/트랙 병렬 로딩

#### 2.3.2 로컬 데이터 매니저

```
├── UserDataManager.java
│   └── SharedPreferences 기반 사용자 데이터 관리
├── CurrentTimetableStorage.java
│   └── 현재 시간표 로컬 저장소
└── TimetableLocalStorage.java
    └── 저장된 시간표 목록 관리
```

---

### 2.4 External Services Layer (외부 서비스 계층)

외부 클라우드 서비스와의 연동을 담당합니다.

#### 2.4.1 Firebase Services

```
Firebase Platform
    ├── Firebase Authentication
    │   ├── 이메일/비밀번호 인증
    │   ├── 자동 로그인 세션 관리
    │   └── 관리자 권한 관리
    ├── Cloud Firestore
    │   ├── graduation_requirements (졸업 요건 데이터)
    │   ├── graduation_meta (학부 메타데이터)
    │   ├── student_progress (학생 진행 상황)
    │   ├── users (사용자 정보)
    │   ├── banners (배너 관리)
    │   ├── certificates (자격증 정보)
    │   ├── documents (서류 관리)
    │   └── timetables (시간표 데이터)
    └── Firebase Storage
        ├── 프로필 이미지
        ├── 배너 이미지
        └── 서류 파일
```

#### 2.4.2 Firestore 데이터 구조

**graduation_requirements 컬렉션**:
```
graduation_requirements/{docId}
    ├── cohort: Long (학번, 예: 2025)
    ├── department: String (학부명)
    ├── track: String (트랙명)
    ├── totalCredits: Int (총 졸업 학점)
    ├── version: String
    ├── updatedAt: Timestamp
    ├── categories: Array<CategoryData>
    │   ├── id: String (예: "major_required")
    │   ├── name: String (예: "전공필수")
    │   ├── type: String ("major" | "general")
    │   ├── required: Int (필수 학점)
    │   ├── courses: Array<Course>
    │   └── groups: Array<CourseGroup>
    └── replacementRules: Array<ReplacementRule>
```

---

## 3. 주요 기능 흐름도

### 3.1 졸업 요건 분석 프로세스

```
[사용자]
    ↓
[GraduationAnalysisActivity] - 학번/학부/트랙 선택
    ↓
[AdditionalRequirementsActivity] - 추가 졸업 요건 선택
    ↓
[CourseInputActivity] - 수강 과목 입력
    ↓ (분석 시작 버튼)
[GraduationRules.analyze()]
    ├── 1. applyReplacementRules() - 대체과목 규칙 적용
    ├── 2. category.analyze() - 각 카테고리 분석
    ├── 3. calculateTotalCredits() - 총 학점 계산
    ├── 4. handleOverflowCredits() - 넘치는 학점 처리
    └── 5. calculateGraduationReadiness() - 졸업 가능 여부 판정
    ↓
[GraduationAnalysisResultActivity] - 분석 결과 표시
    ├── 도넛 차트 시각화
    ├── 카테고리별 상세 결과
    ├── 경고 및 추천사항
    └── PDF 내보내기 옵션
```

### 3.2 인증 및 권한 관리 흐름

```
[앱 시작]
    ↓
[LoginActivity]
    ├── FirebaseAuth.getCurrentUser() != null?
    │   ├── Yes + 자동 로그인 ON
    │   │   └── → [MainActivityNew] / [AdminActivity]
    │   └── No
    │       └── → 로그인 화면 표시
    ↓ (로그인 성공)
[Firestore users 조회]
    ├── isAdmin == true?
    │   ├── Yes → [AdminActivity]
    │   └── No → [MainActivityNew]
    └── SharedPreferences 저장
        └── "is_admin" 플래그 저장
```

### 3.3 과목 추천 알고리즘 흐름

```
[CourseRecommendationActivity]
    ↓
1. 사용자 정보 로드 (학번, 학부, 트랙)
    ↓
2. 수강 이력 분석
    ├── 이수한 카테고리별 학점 계산
    └── 부족한 카테고리 식별
    ↓
3. 우선순위 결정
    ├── 부족한 카테고리 우선 (추천)
    ├── 남은 학점이 많은 카테고리 우선
    └── 사용자 지정 카테고리
    ↓
4. 필터링 및 추천
    ├── 선택한 학년/학기에 개설된 과목
    ├── 아직 수강하지 않은 과목
    └── 선이수 조건 충족 과목
    ↓
[RecommendationResultActivity]
    └── 추천 과목 리스트 표시
```

---

## 4. 데이터 모델 관계도

### 4.1 졸업 요건 분석 도메인 모델

```
GraduationRules
    │
    ├──[1] CreditRequirements
    │       ├── major: Int
    │       ├── general: Int
    │       └── total: Int
    │
    ├──[*] RequirementCategory
    │       ├── id: String
    │       ├── name: String
    │       ├── type: String
    │       ├── required: Int
    │       ├──[*] CourseRequirement
    │       │       ├── code: String
    │       │       ├── name: String
    │       │       ├── credits: Int
    │       │       └── required: Boolean
    │       └──[*] GeneralCourseGroup
    │               ├── groupName: String
    │               ├── required: Int
    │               └──[*] CourseRequirement
    │
    └──[*] ReplacementRule
            ├── discontinuedCourseCode: String
            ├── discontinuedCourseName: String
            ├── replacementCourseCode: String
            ├── replacementCourseName: String
            ├── replacementCategory: String
            └── scope: String

GraduationAnalysisResult
    │
    ├──[*] CategoryAnalysisResult
    │       ├── categoryId: String
    │       ├── categoryName: String
    │       ├── earnedCredits: Int
    │       ├── requiredCredits: Int
    │       ├── isCompleted: Boolean
    │       ├──[*] completedCourses: List<Course>
    │       └──[*] missingCourses: List<String>
    │
    └──[*] appliedReplacements: List<ReplacementRule>
```

### 4.2 사용자 및 인증 모델

```
User (Firestore)
    ├── uid: String
    ├── email: String
    ├── name: String
    ├── studentId: String
    ├── department: String
    ├── track: String
    ├── cohort: String
    ├── isAdmin: Boolean
    └── accessibilitySettings: Map
        └── colorBlindMode: Boolean

Student (Admin View)
    ├── documentId: String
    ├── name: String
    ├── studentId: String
    ├── department: String
    ├── track: String
    ├── email: String
    └── progress: GraduationAnalysisResult
```

---

## 5. 컴포넌트 간 통신

### 5.1 Activity 간 데이터 전달

**Intent Extras를 통한 전달**:
```java
// GraduationAnalysisActivity → AdditionalRequirementsActivity
intent.putExtra("year", selectedYear);
intent.putExtra("department", selectedDepartment);
intent.putExtra("track", selectedTrack);

// CourseInputActivity → GraduationAnalysisResultActivity
intent.putExtra("analysis_result", gson.toJson(analysisResult));
```

### 5.2 Fragment 간 통신

**MainActivity의 FragmentManager 중개**:
```java
// BottomNavigationView를 통한 Fragment 전환
private void replaceFragment(Fragment fragment) {
    fragmentManager.beginTransaction()
        .replace(R.id.fragment_container, fragment)
        .commit();
}
```

### 5.3 콜백 패턴

**FirebaseDataManager의 비동기 콜백**:
```java
public interface LoadCallback {
    void onSuccess(Object data);
    void onFailure(String error);
}

// 사용 예시
dataManager.loadStudentYears(new LoadCallback() {
    @Override
    public void onSuccess(Object data) {
        List<String> years = (List<String>) data;
        updateUI(years);
    }

    @Override
    public void onFailure(String error) {
        showError(error);
    }
});
```

---

## 6. 보안 및 권한 관리

### 6.1 Firebase 보안 규칙

**Firestore Security Rules** (개념적 구조):
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // 졸업 요건: 모든 사용자 읽기 가능, 관리자만 쓰기
    match /graduation_requirements/{document} {
      allow read: if request.auth != null;
      allow write: if isAdmin();
    }

    // 학생 진행 상황: 본인 데이터만 읽기/쓰기
    match /student_progress/{userId} {
      allow read, write: if request.auth.uid == userId;
    }

    // 관리자 전용 컬렉션
    match /users/{document} {
      allow read, write: if isAdmin();
    }

    function isAdmin() {
      return get(/databases/$(database)/documents/users/$(request.auth.uid)).data.isAdmin == true;
    }
  }
}
```

### 6.2 앱 내 권한 체크

```java
// AdminActivity에서 관리자 권한 확인
private boolean isAdmin() {
    SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
    return prefs.getBoolean("is_admin", false);
}

// 관리자가 아니면 접근 차단
if (!isAdmin()) {
    Toast.makeText(this, "관리자 권한이 필요합니다", Toast.LENGTH_SHORT).show();
    finish();
    return;
}
```

---

## 7. 성능 최적화 전략

### 7.1 네트워크 최적화

1. **5분 TTL 캐싱**: 동일한 데이터에 대한 반복적인 네트워크 요청 방지
2. **Single-flight 패턴**: 동일한 요청 병합
3. **병렬 로딩**: 학번/학부/트랙 데이터 동시 로드
4. **DocumentSnapshot 캐싱**: N+1 쿼리 문제 해결

### 7.2 UI 렌더링 최적화

1. **RecyclerView ViewHolder 패턴**: 뷰 재사용
2. **DiffUtil 사용**: 효율적인 리스트 갱신
3. **이미지 로딩**: Glide 라이브러리로 메모리 관리
4. **비동기 처리**: 메인 스레드 블로킹 방지

### 7.3 메모리 관리

1. **싱글톤 패턴**: FirebaseDataManager 인스턴스 재사용
2. **Fragment 재사용**: MainActivity에서 Fragment 인스턴스 유지
3. **캐시 클리어**: 메모리 부족 시 자동 캐시 정리
4. **리소스 해제**: Activity/Fragment 생명주기에 따른 리소스 정리

---

## 8. 에러 처리 전략

### 8.1 네트워크 에러 처리

```java
// FirebaseDataManager에서 표준화된 에러 처리
public void loadData(LoadCallback callback) {
    firestore.collection("data")
        .get()
        .addOnSuccessListener(querySnapshot -> {
            callback.onSuccess(querySnapshot);
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Error loading data", e);
            callback.onFailure("데이터를 불러오는데 실패했습니다: " + e.getMessage());
        });
}
```

### 8.2 사용자 입력 검증

```java
// CourseInputActivity에서 입력 검증
private boolean validateInput() {
    if (selectedCourses.isEmpty()) {
        Toast.makeText(this, "최소 1개 이상의 과목을 선택해주세요", LENGTH_SHORT).show();
        return false;
    }
    return true;
}
```

### 8.3 Null Safety

```java
// 방어적 프로그래밍
if (graduationRules != null && graduationRules.getCategories() != null) {
    for (RequirementCategory category : graduationRules.getCategories()) {
        // 안전한 처리
    }
}
```

---

## 9. 테스트 전략

### 9.1 유닛 테스트 (계획)

```
GraduationRulesTest
    ├── testAnalyzeWithEmptyCourses()
    ├── testAnalyzeWithCompleteCourses()
    ├── testReplacementRuleApplication()
    └── testOverflowCreditsHandling()

FirebaseDataManagerTest (Mock)
    ├── testLoadStudentYearsWithCache()
    ├── testLoadCoursesWithoutCache()
    └── testCacheClearance()
```

### 9.2 통합 테스트 (계획)

```
GraduationAnalysisFlowTest
    ├── 학번/학부/트랙 선택
    ├── 과목 입력
    ├── 분석 결과 확인
    └── PDF 내보내기
```

---

## 10. 확장성 및 유지보수성

### 10.1 모듈화 전략

현재 구조는 **단일 모듈** 구조이지만, 향후 다음과 같이 모듈 분리 가능:

```
:app (Application Module)
:core (Core Business Logic)
    ├── :core-models
    ├── :core-analyzers
    └── :core-utils
:data (Data Layer)
    ├── :data-firebase
    └── :data-local
:ui (UI Components)
    ├── :ui-graduation
    ├── :ui-timetable
    └── :ui-admin
```

### 10.2 버전 관리

```
versionCode = 3
versionName = "1.0"
```

- **versionCode**: Google Play 내부 버전 번호 (증가만 가능)
- **versionName**: 사용자에게 표시되는 버전 (Semantic Versioning 권장)

### 10.3 로깅 및 디버깅

```java
// 표준화된 로그 태그 사용
private static final String TAG = "ClassName";

// 로그 레벨 활용
Log.d(TAG, "Debug message");       // 개발용
Log.i(TAG, "Info message");        // 정보성
Log.w(TAG, "Warning message");     // 경고
Log.e(TAG, "Error message", e);    // 에러
```

---

## 11. 배포 및 빌드

### 11.1 빌드 타입

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true          // 코드 난독화 활성화
        isShrinkResources = true        // 미사용 리소스 제거
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
    debug {
        isMinifyEnabled = false         // 디버그 빌드는 난독화 비활성화
        isDebuggable = true
    }
}
```

### 11.2 ProGuard 규칙

주요 ProGuard 규칙 (proguard-rules.pro):
```
# Firebase 관련
-keep class com.google.firebase.** { *; }

# 모델 클래스 (Firestore 직렬화용)
-keep class sprout.app.sakmvp1.models.** { *; }

# Gson 사용 클래스
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
```

---

## 12. 향후 개선 방향

### 12.1 아키텍처 개선

1. **MVVM 패턴 도입**: ViewModel + LiveData로 UI와 비즈니스 로직 분리
2. **Dependency Injection**: Hilt/Dagger로 의존성 관리
3. **Repository 패턴**: 데이터 소스 추상화
4. **UseCase 계층**: 비즈니스 로직을 재사용 가능한 UseCase로 분리

### 12.2 기술 스택 현대화

1. **Kotlin 마이그레이션**: Java → Kotlin 전환
2. **Jetpack Compose**: XML 레이아웃 → Compose UI
3. **Coroutines/Flow**: 비동기 처리 개선
4. **Room Database**: 로컬 DB 구조화

### 12.3 새로운 기능

1. **오프라인 모드**: Room + WorkManager로 오프라인 지원
2. **푸시 알림**: FCM으로 학사 일정 알림
3. **다국어 지원**: 영어/한국어 전환
4. **다크 모드**: 시스템 설정 연동

---

## 부록 A: 주요 클래스 책임 요약

| 클래스명 | 계층 | 주요 책임 |
|---------|------|----------|
| **FirebaseDataManager** | Data | Firestore 데이터 접근 중앙 관리, 캐싱 |
| **GraduationRules** | Business Logic | 졸업 요건 분석 엔진, 규칙 적용 |
| **RequirementCategory** | Business Logic | 카테고리별 분석 로직 |
| **GraduationAnalysisResult** | Business Logic | 분석 결과 데이터 모델 |
| **CourseInputActivity** | Presentation | 과목 입력 UI 및 사용자 상호작용 |
| **GraduationAnalysisResultActivity** | Presentation | 분석 결과 표시 및 시각화 |
| **BaseActivity** | Presentation | 공통 UI 기능 (색약 모드 등) |
| **MainActivityNew** | Presentation | 메인 컨테이너, Fragment 관리 |
| **CustomizedRequirementsManager** | Business Logic | 사용자 맞춤 요건 관리 |

---

## 부록 B: 기술 스택 상세

| 카테고리 | 기술/라이브러리 | 버전 | 용도 |
|---------|---------------|------|------|
| **언어** | Java | 11 | 주 개발 언어 |
| **UI** | AndroidX AppCompat | - | 하위 호환 UI 컴포넌트 |
| **UI** | Material Components | - | Material Design 구현 |
| **네트워크** | OkHttp | 4.12.0 | HTTP 클라이언트 |
| **네트워크** | Jsoup | 1.17.2 | HTML 파싱 (식단 크롤링) |
| **데이터베이스** | Firebase Firestore | 33.6.0 | NoSQL 클라우드 DB |
| **인증** | Firebase Auth | 33.6.0 | 사용자 인증 |
| **스토리지** | Firebase Storage | 33.6.0 | 파일 저장소 |
| **이미지** | Glide | 4.16.0 | 이미지 로딩/캐싱 |
| **JSON** | Gson | 2.10.1 | JSON 직렬화/역직렬화 |
| **PDF** | iText7 | 7.2.5 | PDF 생성 |
| **지도** | OSMDroid | 6.1.18 | OpenStreetMap |
| **이미지뷰** | PhotoView | 2.3.0 | 줌 가능한 이미지뷰 |
| **빌드** | Gradle | 8.x | 빌드 시스템 |

---

## 문서 작성 정보

- **작성일**: 2025-11-25
- **버전**: 1.0
- **작성자**: Claude Code Architecture Analyzer
- **프로젝트 상태**: Production (v1.0)
