# 🎓 SakMvp1 애플리케이션 아키텍처

> **졸업 요건 분석 및 학점 관리 Android 애플리케이션**
> 학생들의 수강 정보를 기반으로 졸업 요건 충족 여부를 분석하고 시각적으로 표현하는 앱

## 📱 애플리케이션 개요

### 핵심 기능
- 📊 **졸업 요건 분석**: 학번/학부/트랙 선택하여 졸업 요건 확인
- 📝 **수강 강의 입력**: 전공/교양 과목별 이수 강의 등록
- 📈 **진도율 시각화**: 도넛 차트를 통한 졸업 진행 상태 표시
- 💾 **데이터 관리**: Firebase Firestore를 통한 클라우드 데이터 저장
- 🔐 **사용자 인증**: Firebase Authentication 기반 로그인/회원가입

### 기술 스택
- **언어**: Java
- **플랫폼**: Android (API 24-35)
- **데이터베이스**: Firebase Firestore
- **인증**: Firebase Authentication
- **UI**: Android View System (전통적인 View 기반, Compose 미사용)
- **아키텍처**: MVC 패턴 + 싱글톤 데이터 매니저

---

## 🏗️ 클래스 구조 및 역할

### 🎯 **핵심 데이터 계층**

#### `FirebaseDataManager.java` (싱글톤)
**역할**: Firestore 데이터 조회/관리의 중앙 집중식 매니저
```java
public class FirebaseDataManager {
    private static FirebaseDataManager instance;
    private FirebaseFirestore db;
}
```

**주요 기능**:
- 📚 **졸업 요건 데이터 로드**: 학부/트랙/학번별 졸업 요건 조회
- 🏫 **학부/트랙 정보 관리**: 전체 학부, 학부별 트랙 목록 제공
- 📖 **강의 데이터 조회**: 전공필수/선택, 교양필수/선택, 전공심화 강의 목록
- 🎯 **학점 요건 분석**: 졸업이수학점 요건 계산 및 검증
- ⚡ **캐싱 시스템**: 성능 최적화를 위한 메모리 캐시 (학부, 트랙, 강의 데이터)
- 🔄 **N+1 쿼리 해결**: DocumentSnapshot 캐싱으로 중복 쿼리 방지

**성능 최적화**:
- 📈 Single-flight 패턴으로 중복 요청 방지
- 🚀 동시 로딩: 학번/학부/트랙 데이터를 한 번에 로드
- 💾 5분 유효성 캐시로 네트워크 요청 최소화

---

#### `UserDataManager.java`
**역할**: 사용자 인증 및 개인 데이터 관리
```java
public class UserDataManager {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
}
```

**주요 기능**:
- 👤 사용자 프로필 정보 관리
- 📊 개인 학습 진도 데이터 저장/조회
- 🔐 Firebase Authentication 연동

---

#### `DepartmentConfig.java`
**역할**: 학부별 설정 관리 (전공심화/학부공통 사용 여부)
```java
public class DepartmentConfig {
    private static final Map<String, Boolean> CACHED_DEPARTMENT_CONFIG;
}
```

**주요 기능**:
- 🏫 학부별 전공심화 과목 사용 여부 설정
- 📚 학부공통 과목 적용 여부 관리
- 💾 Firebase 동적 설정 로드 및 캐싱

---

### 🖥️ **UI 계층 (Activity)**

#### `MainActivity.java`
**역할**: 앱의 메인 허브 화면
```java
public class MainActivity extends AppCompatActivity {
    private ViewPager2 viewPagerBanner;
    private BottomNavigationView bottomNavigation;
}
```

**주요 기능**:
- 🎠 **배너 슬라이드**: 8초 간격 자동 슬라이드 (3개 배너)
- 🧭 **내비게이션**: 하단 내비게이션으로 화면 이동
- 📊 **Firebase Analytics**: 사용자 행동 트래킹
- 🎨 **Edge-to-Edge UI**: 현대적인 전면 화면 레이아웃
- ♿ **접근성 지원**: 고대비 테마 옵션 (현재 ANR 디버깅을 위해 임시 비활성화)
- 📅 **시간표 연동**: 하단 네비게이션을 통한 시간표 화면 이동

---

#### `GraduationAnalysisActivity.java`
**역할**: 졸업 요건 분석 진입점
```java
public class GraduationAnalysisActivity extends AppCompatActivity {
    private Spinner spinnerStudentId, spinnerDepartment, spinnerTrack;
}
```

**주요 기능**:
- 🎓 **학번 선택**: UI는 2자리(25), 내부는 4자리(2025)로 변환
- 🏫 **학부/트랙 선택**: 동적 로드된 데이터로 스피너 구성
- ✅ **데이터 검증**: 선택된 조합의 졸업요건 존재 여부 확인
- ⚡ **동시 로딩**: 초기 로드 시 학번/학부/트랙 데이터 한번에 로드
- 🚀 **즉시 반응**: 캐시된 데이터로 트랙 선택 시 즉시 표시

**성능 개선**:
- 100ms 디바운스로 빠른 응답성
- 캐시 우선 전략으로 네트워크 지연 최소화

---

#### `AdditionalRequirementsActivity.java`
**역할**: 추가 졸업 요건 입력
```java
public class AdditionalRequirementsActivity extends AppCompatActivity {
    // TLC, 채플, 마일리지, 동적 추가요건 입력
}
```

**주요 기능**:
- 📋 **기본 요건 입력**: TLC 이수 횟수, 채플 이수 학기, 마일리지 달성 여부
- 🎯 **동적 추가 요건**: 학과별 특별 요구사항 (졸업작품 등)
- 🔄 **데이터 전달**: 이전 화면 데이터(학번/학과/트랙) 표시
- 🛡️ **2초 버튼 가드**: 중복 제출 방지 시스템

---

#### `CourseInputActivity.java`
**역할**: 수강 강의 입력 메인 화면
```java
public class CourseInputActivity extends AppCompatActivity {
    private static final int MIN_LOAD_INTERVAL = 500; // 최적화된 로드 간격
}
```

**주요 기능**:
- 📚 **과목 분류**: 전공/교양 그룹 전환
- 🏷️ **세부 탭 관리**: 전공필수/선택/심화, 교양필수/선택
- ➕ **강의 추가 다이얼로그**: 동적 스피너로 강의 선택
- 📊 **실시간 집계**: 학점 계산 및 진도율 표시
- ⚡ **성능 최적화**: 500ms 로드 간격, 중복 요청 방지, 캐시 우선 전략

**최적화 기능**:
- In-Flight 요청 병합으로 중복 네트워크 호출 제거
- 스피너 복원 지연 시간 제거 (0ms)
- 버튼 상태 관리 및 2초 가드

---

#### `GraduationAnalysisResultActivity.java`
**역할**: 졸업 요건 분석 결과 표시
```java
public class GraduationAnalysisResultActivity extends AppCompatActivity {
    // 졸업 진행률 시각화 및 상세 분석 결과
}
```

**주요 기능**:
- 📊 **도넛 차트**: 졸업 진행률 시각적 표현
- 📈 **상세 분석**: 카테고리별 이수 현황
- 💾 **결과 저장**: 분석 결과 Firestore 저장
- 🧭 **내비게이션**: 하단 네비게이션 연동

---

#### `TimeTableActivity.java`
**역할**: 주간 시간표 관리 및 수업 스케줄링
```java
public class TimeTableActivity extends AppCompatActivity {
    private RelativeLayout timetableLayout;
    private FloatingActionButton fabAddSchedule;
}
```

**주요 기능**:
- 📅 **주간 시간표 뷰**: 월~금요일 시간표 격자 레이아웃
- ➕ **수업 추가**: Floating Action Button으로 새 수업 등록
- 📝 **Bottom Sheet**: 수업 정보 입력을 위한 슬라이드업 다이얼로그
- 🕒 **시간 선택**: 시작시간/종료시간 선택 위젯
- 🎨 **Material Design**: CoordinatorLayout 기반 현대적 UI
- 🧭 **네비게이션**: 하단 네비게이션 바 연동
- 📱 **반응형 레이아웃**: 다양한 화면 크기 대응

**UI 구성 요소**:
- **AppBarLayout**: 상단 툴바 (제목: "시간표", 뒤로 가기 버튼)
- **NestedScrollView**: 스크롤 가능한 시간표 컨테이너
- **요일 헤더**: 월화수목금 표시 (Primary 색상 적용)
- **시간표 그리드**: RelativeLayout 기반 동적 레이아웃
- **FAB**: 우하단 고정 (+) 버튼 (improved positioning: 24dp margin, 120dp bottom)
- **BottomNavigationView**: 하단 네비게이션 (compact design)

---

### 🛠️ **유틸리티 및 도구**

#### `DataViewerActivity.java`
**역할**: Firestore 데이터 탐색 및 디버깅 도구
```java
public class DataViewerActivity extends AppCompatActivity {
    // 개발/디버깅용 Firestore 뷰어
}
```

**주요 기능**:
- 🔍 **컬렉션 탐색**: 사용 가능한 Firestore 컬렉션 조회
- 📄 **문서 뷰어**: 전체 문서 또는 특정 문서 ID 조회
- 🛠️ **디버깅 지원**: 개발자용 데이터 확인 도구

---

#### `DonutChartView.java`
**역할**: 커스텀 도넛 차트 위젯
```java
public class DonutChartView extends View {
    private float progress = 75f;
    private float strokeWidth = 20f;
}
```

**주요 기능**:
- 📊 **진행률 시각화**: 졸업 진행률을 도넛 차트로 표현
- 🎨 **커스텀 드로잉**: Canvas를 이용한 직접 렌더링
- 📐 **동적 크기**: 다양한 화면 크기 대응

---

#### `HighContrastHelper.java`
**역할**: 접근성 지원 도구
```java
public class HighContrastHelper {
    // 고대비 테마 적용 헬퍼
}
```

**주요 기능**:
- ♿ **접근성 개선**: 시각 장애인을 위한 고대비 테마
- 💾 **설정 저장**: SharedPreferences로 사용자 선택 유지
- 🎨 **테마 적용**: 전체 앱에 일관된 접근성 테마

---

### 🔐 **인증 계층**

#### `Login/LoginActivity.java`
**역할**: 사용자 로그인
```java
public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
}
```

**주요 기능**:
- 🔐 **Firebase 인증**: 이메일/비밀번호 로그인
- 💾 **자동 로그인**: SharedPreferences로 로그인 상태 유지
- ✅ **입력 검증**: 이메일 형식 및 필수 필드 검사

---

#### `Login/SignUpActivity.java`
**역할**: 신규 사용자 회원가입
```java
public class SignUpActivity extends AppCompatActivity {
    // 이메일, 비밀번호, 개인정보 입력
}
```

**주요 기능**:
- 👤 **계정 생성**: Firebase Authentication 계정 생성
- 📝 **프로필 저장**: Firestore에 사용자 세부 정보 저장
- ✅ **유효성 검사**: 비밀번호 확인, 필수 필드 검증

---

## 🔄 **데이터 플로우**

### 졸업 요건 분석 플로우
```
1. GraduationAnalysisActivity (학번/학부/트랙 선택)
   ↓
2. AdditionalRequirementsActivity (추가 요건 입력)
   ↓
3. CourseInputActivity (수강 강의 입력)
   ↓
4. GraduationAnalysisResultActivity (결과 분석)
```

### 데이터 계층 구조
```
Firebase Firestore
├── graduation_requirements/    # 졸업 요건 데이터(강의군, 강의명, 학점, 조건)
├── graduation_meta/               # 모든 학부 데이터(졸업 학점, 추가졸업조건)
└── users/                    # 사용자 프로필
```

---

## ⚡ **성능 최적화**

### 캐싱 전략
- **메모리 캐시**: 학부/트랙/강의 데이터 5분 유효성 캐시
- **DocumentSnapshot 캐시**: N+1 쿼리 해결을 위한 문서 캐시
- **동시 로딩**: 초기 데이터 병렬 로드로 사용자 대기시간 단축

### 네트워크 최적화
- **Single-flight 패턴**: 중복 요청 방지
- **In-flight 요청 병합**: 동일 요청 자동 병합
- **디바운스**: 100ms 디바운스로 불필요한 요청 제거

### UI 성능
- **즉시 피드백**: 캐시 데이터 우선 표시
- **버튼 가드**: 2초 중복 클릭 방지
- **핸들러 정리**: onPause에서 메모리 누수 방지

---

## 🎯 **아키텍처 특징**

### 장점
- ✅ **단순하고 직관적**: 전통적인 MVC 패턴으로 이해하기 쉬움
- ✅ **싱글톤 데이터 매니저**: 중앙 집중식 데이터 관리
- ✅ **Firebase 완전 활용**: Firestore + Authentication 최적화
- ✅ **성능 최적화**: 캐싱, 디바운스, 병합 요청 등 다양한 최적화

### 개선 포인트
- 🔄 **MVVM 패턴 도입**: ViewModel과 LiveData 활용 고려
- 🧪 **테스트 가능성**: 의존성 주입을 통한 테스트 용이성 개선
- 📦 **모듈화**: 기능별 모듈 분리로 유지보수성 향상

---

## 📊 **주요 성능 지표**

- ⚡ **초기 로딩**: 3개 데이터 소스(학번/학부/트랙) 동시 로드
- 🚀 **캐시 히트율**: 재방문 시 네트워크 요청 80% 감소
- 📱 **UI 반응성**: 100ms 디바운스로 즉시 반응 구현
- 🔄 **메모리 효율**: 5분 캐시 유효성으로 메모리 사용량 최적화

---

*📝 이 문서는 2025년 9월 기준으로 작성되었으며, 지속적인 업데이트를 통해 최신 상태를 유지합니다.*