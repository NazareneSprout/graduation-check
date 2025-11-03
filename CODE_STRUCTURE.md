# 코드 구조 가이드

나사렛대학교 학사 관리 앱의 전체 Java 파일 구조와 각 파일의 역할을 정리한 문서입니다.

---

## 📱 1. 메인 화면 및 네비게이션

### MainActivityNew.java
- **역할**: 앱의 메인 컨테이너 Activity
- **기능**:
  - 하단 네비게이션 바를 통한 3개 탭 전환 (홈, 시간표, 내 정보)
  - 관리자 권한 확인 시 관리자 페이지 접근
  - Fragment 기반 화면 전환 관리
- **포함 Fragment**:
  - HomeFragment: 홈 화면
  - TimeTableFragment: 시간표 화면
  - UserProfileFragment: 사용자 프로필 화면

---

## 🏠 2. 홈 화면 (HomeFragment)

### HomeFragment.java
- **역할**: 앱의 메인 홈 화면
- **주요 기능**:
  - 공지사항 배너 슬라이더 표시
  - 주요 기능 버튼 제공:
    - 졸업요건 검사
    - 자격증 게시판
    - 수강과목 추천
    - 필수 서류 안내
    - 학사일정
    - 학칙
    - 학식 메뉴
  - Firebase에서 배너 데이터 로드
- **관련 파일**:
  - Banner.java: 배너 데이터 모델

---

## 🎓 3. 로그인 및 회원가입 (Login 패키지)

### LoginActivity.java
- **역할**: 사용자 로그인 화면
- **기능**:
  - Firebase Authentication을 통한 이메일/비밀번호 로그인
  - 자동 로그인 (SharedPreferences)
  - 관리자/일반 사용자 구분
  - 테스트 계정 접근 제어
- **흐름**: LoginActivity → MainActivityNew (또는 AdminActivity)

### SignUpActivity.java
- **역할**: 신규 회원가입 화면
- **기능**:
  - 이메일/비밀번호 기반 회원가입
  - Firebase Authentication 계정 생성
  - 입력 검증 (이메일 형식, 비밀번호 일치 등)

---

## 👤 4. 사용자 정보 관리

### UserInfoActivity.java
- **역할**: 사용자 학적 정보 입력/수정 화면
- **기능**:
  - 학부, 전공/트랙, 학번, 현재 학기 입력
  - Firebase Firestore에 사용자 정보 저장
  - Student 모델 사용
- **흐름**: 신규 사용자 → UserInfoActivity → LoadingUserInfoActivity

### LoadingUserInfoActivity.java
- **역할**: 사용자 데이터 로딩 중간 화면
- **기능**:
  - Firebase에서 졸업요건 데이터 로드
  - 사용자가 입력한 학적 정보에 맞는 졸업요건 매칭
  - 로딩 완료 후 AdditionalRequirementsActivity 또는 GraduationAnalysisActivity로 이동

### UserProfileActivity.java
- **역할**: 사용자 프로필 상세 화면 (독립 Activity)
- **기능**:
  - 사용자 정보 표시
  - 학적 정보 수정
  - 시간표 관리
  - 로그아웃

### UserProfileFragment.java
- **역할**: 메인 화면의 프로필 탭 (Fragment)
- **기능**:
  - 사용자 정보 요약 표시
  - 졸업요건 재분석
  - 학사일정, 학칙 링크
  - 로그아웃

---

## 📊 5. 졸업요건 분석

### GraduationAnalysisActivity.java
- **역할**: 졸업요건 분석 메인 화면
- **기능**:
  - 사용자가 이수한 과목 정보 확인
  - 졸업요건 분석 시작
  - 분석 결과를 GraduationAnalysisResultActivity로 전달

### GraduationAnalysisResultActivity.java (3,600+ 라인)
- **역할**: 졸업요건 분석 결과 표시 (앱의 핵심 화면)
- **주요 기능**:
  - 3개 탭으로 구성:
    - **개요(Overview)**: 졸업 진행률, 도넛 차트, 카테고리별 요약
    - **상세(Details)**: 카테고리별 이수 과목 및 부족 학점 상세 정보
    - **기타(Others)**: 추가 정보
  - GraduationRules 모델을 통한 졸업요건 검증
  - 대체과목 규칙 적용
  - 학점 초과 이수 처리
  - Firebase에 분석 결과 저장
- **사용 모델**:
  - GraduationAnalysisResult
  - CategoryAnalysisResult
  - GraduationRules

### AdditionalRequirementsActivity.java
- **역할**: 추가 이수요건 입력 화면
- **기능**:
  - 편입생, 복수전공 등 특수 요건 입력
  - 이수한 과목 목록 입력
  - CourseInputActivity로 이동

### CourseInputActivity.java
- **역할**: 사용자가 이수한 과목 입력 화면
- **기능**:
  - 학기별 이수 과목 입력/수정
  - Firebase에 과목 데이터 저장
  - 졸업요건 분석 실행
  - 분석 완료 후 GraduationAnalysisResultActivity로 이동

---

## 📚 6. 수강과목 추천

### CourseRecommendationActivity.java
- **역할**: 수강과목 추천 메인 화면
- **기능**:
  - 사용자 정보 확인
  - 추천 알고리즘 실행
  - RecommendationResultActivity로 결과 전달

### RecommendationResultActivity.java
- **역할**: 추천 과목 결과 표시
- **주요 기능**:
  - 2개 탭 구성:
    - **우선순위별**: 과목군 우선순위에 따른 추천 (교양필수 > 전공필수 > ... > 교양선택)
    - **학기별**: 각 학기에 적합한 과목 추천
  - 교양선택: 5개 역량 중 이수한 역량 수 표시
  - 소양: 2학년 1학기부터 표시
  - 카테고리별 필요 학점 및 추천 과목 목록
- **사용 모델**:
  - RecommendedCourse
  - GraduationAnalysisResult

### RecommendedCourse.java
- **역할**: 추천 과목 데이터 모델
- **기능**:
  - 과목명, 학점, 카테고리, 학기 정보
  - 카테고리별 우선순위 정의

### RecommendedCourseAdapter.java
- **역할**: 추천 과목 목록 RecyclerView 어댑터
- **기능**: 추천 과목을 카드 형태로 표시

---

## 🗓️ 7. 시간표 관리 (timetable 패키지)

### TimeTableFragment.java
- **역할**: 시간표 메인 화면 (Fragment)
- **기능**:
  - 주간 시간표 표시
  - 수업 추가/삭제
  - 시간표 저장 및 불러오기
  - 겹침 방지 기능

### TimeTableActivity.java
- **역할**: 시간표 독립 Activity (전체화면)
- **기능**: TimeTableFragment와 동일하지만 독립 화면

### AddScheduleActivity.java
- **역할**: 수업 추가 화면 (현재 미통합)
- **기능**:
  - 수업명, 교수, 장소, 요일, 시간 입력
  - 시간 선택 다이얼로그
  - 겹침 검사
- **상태**: 구현 완료, 앱에 통합되지 않음

### SavedTimetablesActivity.java
- **역할**: 저장된 시간표 목록 화면
- **기능**:
  - Firebase에 저장된 시간표 목록 로드
  - 시간표 선택 및 불러오기
  - 시간표 삭제

### SavedTimetable.java
- **역할**: 저장된 시간표 데이터 모델
- **기능**: 시간표 이름, 학기, 수업 목록 정보

### SavedTimetableAdapter.java
- **역할**: 저장된 시간표 목록 어댑터

### ScheduleItem.java
- **역할**: 개별 수업 데이터 모델
- **기능**: 수업명, 시간, 장소 등 정보

### CurrentTimetableStorage.java / TimetableLocalStorage.java
- **역할**: 시간표 로컬 저장 관리
- **기능**: SharedPreferences를 통한 현재 시간표 임시 저장

---

## 👔 8. 관리자 기능

### AdminActivity.java
- **역할**: 관리자 메인 대시보드
- **기능**:
  - 졸업요건 통합 관리
  - 학생 데이터 조회
  - 서류 관리
  - 관리자 전용 기능 접근

### StudentDataActivity.java
- **역할**: 학생 데이터 목록 조회
- **기능**:
  - Firebase의 모든 학생 정보 로드
  - 학생 검색 및 필터링
  - 학생 상세 정보로 이동

### StudentDetailActivity.java
- **역할**: 학생 상세 정보 화면
- **기능**:
  - 학생 개인 정보 표시
  - 이수 과목 목록
  - 졸업요건 분석 결과

### StudentAdapter.java
- **역할**: 학생 목록 RecyclerView 어댑터

---

## 📋 9. 졸업요건 관리 (관리자)

### GraduationRequirementsActivity.java
- **역할**: 졸업요건 문서 목록 화면
- **기능**:
  - Firebase의 졸업요건 문서 목록 표시
  - 졸업요건 추가/수정/삭제
  - 전공/교양 문서 관리 화면으로 이동
  - 학부/트랙/학번별 필터링

### GraduationRequirementDetailActivity.java
- **역할**: 졸업요건 상세 정보 화면 (현재 미사용)
- **상태**: 코드상 호출 없음, 삭제 검토 필요

### GraduationRequirementEditActivity.java
- **역할**: 졸업요건 수정 화면
- **주요 기능**:
  - ViewPager2로 3개 탭 구성:
    - 학점 요건 (CreditRequirementsFragment)
    - 전공 과목 (MajorCoursesFragment)
    - 교양 과목 (GeneralCoursesFragment)
    - 대체과목 규칙 (ReplacementRulesFragment)
  - Firebase 실시간 동기화
  - 전공/교양 문서 참조 관리

### GraduationRequirementAddActivity.java
- **역할**: 신규 졸업요건 추가 화면
- **기능**: GraduationRequirementEditActivity와 유사하지만 신규 생성용

### GraduationRequirement.java
- **역할**: 졸업요건 문서 데이터 모델
- **주요 필드**:
  - 학부, 트랙, 학번
  - 총이수학점, 전공/교양 학점 요건
  - 전공/교양 문서 참조
  - 버전 정보

### GraduationRequirementAdapter.java
- **역할**: 졸업요건 목록 어댑터
- **기능**:
  - 졸업요건 카드 표시
  - 삭제 모드 지원
  - 체크박스 선택

### GraduationRequirementPagerAdapter.java
- **역할**: 졸업요건 수정 화면의 ViewPager2 어댑터
- **기능**: 4개 Fragment 페이지 관리

---

## 📑 10. 졸업요건 수정 Fragment들

### fragments/CreditRequirementsFragment.java
- **역할**: 학점 요건 탭
- **기능**:
  - 총이수학점, 전공/교양 필수/선택 학점 입력
  - 전공심화, 학부공통, 소양, 교양선택, 자율선택, 잔여학점 관리

### fragments/MajorCoursesFragment.java
- **역할**: 전공 과목 탭
- **기능**:
  - 전공 과목 추가/수정/삭제
  - 과목명, 학점, 카테고리, 대체 가능 여부 설정
  - 학기별 과목 분류

### fragments/GeneralCoursesFragment.java
- **역할**: 교양 과목 탭
- **기능**:
  - 교양 과목 그룹 관리 (교양필수, 소양, 교양선택 등)
  - 역량별 과목 분류 (교양선택의 경우)
  - 과목 추가/수정/삭제

### fragments/ReplacementRulesFragment.java
- **역할**: 대체과목 규칙 탭
- **기능**:
  - 대체과목 규칙 추가/수정/삭제
  - 원본 과목 → 대체 가능 과목 목록 관리
  - 다중 대체 과목 지원

---

## 📂 11. 서류 관리

### RequiredDocumentsActivity.java
- **역할**: 필수 서류 안내 화면 (사용자용)
- **기능**:
  - 학부별 필수 서류 폴더 목록 표시
  - DocumentFilesActivity로 이동

### DocumentFilesActivity.java
- **역할**: 서류 파일 목록 화면
- **기능**:
  - 특정 폴더의 서류 파일 목록 표시
  - 파일 클릭 시 웹뷰로 열기

### ManageDocumentFoldersActivity.java
- **역할**: 서류 폴더 관리 화면 (관리자용)
- **기능**:
  - 폴더 추가/수정/삭제
  - Firebase Storage 연동

### ManageDocumentFilesActivity.java
- **역할**: 서류 파일 관리 화면 (관리자용)
- **기능**:
  - 파일 업로드/수정/삭제
  - Firebase Storage 연동

### DocumentFolder.java / DocumentFile.java
- **역할**: 서류 폴더 및 파일 데이터 모델

### DocumentFolderAdapter.java / DocumentFileAdapter.java
- **역할**: 서류 폴더/파일 목록 어댑터

### ManageDocumentFolderAdapter.java / ManageDocumentFileAdapter.java
- **역할**: 관리자용 서류 폴더/파일 목록 어댑터 (편집 기능 포함)

---

## 📖 12. 전공/교양 문서 관리

### MajorDocumentManageActivity.java
- **역할**: 전공 과목 문서 관리 화면
- **기능**:
  - 특정 학부/트랙의 전공 과목 목록 관리
  - Firebase의 `graduation_requirements` 컬렉션 직접 수정
  - 신규 전공 문서 생성

### GeneralDocumentManageActivity.java
- **역할**: 교양 과목 문서 관리 화면
- **기능**:
  - 교양 공통 문서 관리
  - 교양필수, 소양, 교양선택 과목 관리
  - 역량별 교양선택 과목 분류

---

## 🍽️ 13. 학식 메뉴

### MealMenuActivity.java
- **역할**: 학식 메뉴 조회 화면
- **기능**:
  - 나사렛대학교 홈페이지에서 최신 식단표 스크래핑 (Jsoup)
  - OkHttp로 HTTP 요청
  - Glide로 식단 이미지 로드
  - PhotoView로 핀치 줌 지원
  - 이미지 클릭 시 ImageZoomActivity로 이동

### ImageZoomActivity.java
- **역할**: 이미지 전체화면 확대 화면
- **기능**:
  - PhotoView를 사용한 이미지 확대/축소
  - 더블탭 줌, 드래그 지원
  - static Bitmap을 통한 이미지 전달

---

## 🏆 14. 자격증 게시판

### CertificateBoardActivity.java
- **역할**: 자격증 게시판 화면
- **기능**:
  - Firebase에서 자격증 정보 로드
  - 자격증 카드 형태로 표시
  - 자격증 클릭 시 상세 정보

### Certificate.java
- **역할**: 자격증 데이터 모델
- **필드**: 자격증명, 발급기관, 설명, 이미지 URL 등

### CertificateAdapter.java
- **역할**: 자격증 목록 어댑터

---

## 🌐 15. 웹뷰

### WebViewActivity.java
- **역할**: 웹페이지 표시 화면
- **기능**:
  - 학사일정, 학칙 등 외부 링크 표시
  - WebView로 웹페이지 로드
  - 뒤로가기 버튼 지원

---

## 🔧 16. 유틸리티 및 헬퍼 클래스

### FirebaseDataManager.java (핵심 싱글톤 클래스, 3,800+ 라인)
- **역할**: Firebase Firestore 데이터 관리 중앙 클래스
- **주요 기능**:
  - 졸업요건 데이터 로드/저장
  - 학생 데이터 CRUD
  - 과목 데이터 관리
  - 대체과목 규칙 관리
  - 서류 데이터 관리
  - 배너 데이터 관리
  - 데이터 변환 (V1 → V2)
- **싱글톤 패턴**: `getInstance()`로 접근
- **콜백 인터페이스**: 비동기 데이터 로드를 위한 다양한 리스너 정의

### UserDataManager.java
- **역할**: 사용자 데이터 로컬 캐시 관리
- **기능**:
  - SharedPreferences를 통한 사용자 정보 저장/로드
  - 로그인 정보 관리

### DepartmentConfig.java
- **역할**: 학부/트랙 설정 데이터
- **기능**: 학부별 트랙 목록 정의 (IT학부, 간호학부 등)

### utils/GraduationRequirementUtils.java
- **역할**: 졸업요건 관련 유틸리티 메소드
- **기능**:
  - Firestore 문서 데이터 변환
  - 졸업요건 검증 헬퍼

### utils/UiUtils.java
- **역할**: UI 관련 유틸리티 메소드
- **기능**: DP↔PX 변환, Toast 표시 등

### managers/CustomizedRequirementsManager.java
- **역할**: 사용자 맞춤 졸업요건 관리
- **기능**:
  - 사용자별 커스터마이징된 졸업요건 저장/로드
  - Firebase와 로컬 캐시 동기화

### DonutChartView.java
- **역할**: 커스텀 도넛 차트 뷰
- **기능**: 졸업 진행률을 도넛 차트로 시각화 (GraduationAnalysisResultActivity에서 사용)

---

## 📦 17. 데이터 모델 (models 패키지)

### models/Student.java
- **역할**: 학생 정보 데이터 모델
- **필드**: UID, 학부, 전공, 학번, 현재 학기, 이수 과목 등

### models/GraduationRules.java
- **역할**: 졸업요건 규칙 모델
- **주요 메소드**:
  - `analyze()`: 학생의 이수 과목을 분석하여 졸업요건 달성 여부 판단
  - `getCategoryByCourseName()`: 과목명으로 카테고리 찾기
  - 대체과목 규칙 적용
  - 학점 초과 이수 처리

### models/GraduationAnalysisResult.java
- **역할**: 졸업요건 분석 결과 모델
- **주요 필드**:
  - 총 이수/필요 학점
  - 졸업 가능 여부
  - 카테고리별 분석 결과 리스트
  - 부족 학점 정보

### models/CategoryAnalysisResult.java
- **역할**: 카테고리별 분석 결과 모델
- **주요 필드**:
  - 카테고리명 (전공필수, 교양선택 등)
  - 이수 학점 / 필요 학점
  - 이수 과목 목록
  - 미이수 과목 목록
  - 서브그룹 결과 (교양선택의 역량별 분석 등)

### models/RequirementCategory.java
- **역할**: 졸업요건 카테고리 모델
- **필드**: 카테고리명, 필요 학점, 과목 목록

### models/CourseRequirement.java
- **역할**: 과목 정보 모델
- **필드**: 과목명, 학점, 학기, 대체 가능 여부

### models/ReplacementRule.java
- **역할**: 대체과목 규칙 모델
- **필드**: 원본 과목명, 대체 가능 과목 목록

### models/GeneralCourseGroup.java
- **역할**: 교양 과목 그룹 모델
- **필드**: 그룹명 (교양필수, 소양 등), 과목 목록

### models/CreditRequirements.java
- **역할**: 학점 요건 모델
- **필드**: 총이수학점, 전공/교양 필수/선택 학점

### models/UserCustomizedRequirements.java
- **역할**: 사용자 맞춤 졸업요건 모델
- **용도**: 사용자가 자신의 졸업요건을 커스터마이징한 경우

---

## 🎨 18. 어댑터 (adapters 패키지)

### adapters/CourseEditAdapter.java
- **역할**: 과목 편집 RecyclerView 어댑터
- **기능**: 과목 추가/수정/삭제 UI

### adapters/GeneralCourseGroupAdapter.java
- **역할**: 교양 과목 그룹 어댑터
- **기능**: 교양 과목 그룹별 표시

### adapters/GraduationRequirementPagerAdapter.java
- **역할**: 졸업요건 수정 ViewPager2 어댑터

### adapters/ReplacementRuleEditAdapter.java
- **역할**: 대체과목 규칙 편집 어댑터
- **기능**: 대체과목 규칙 추가/수정/삭제

---

## 🛠️ 19. 개발/디버깅 도구 (프로덕션에서 제외 권장)

### DataViewerActivity.java
- **역할**: Firestore 데이터 탐색 도구
- **기능**:
  - 컬렉션 목록 조회
  - 문서 데이터 조회
  - 문서 개수 확인
  - 클립보드 복사
- **용도**: 개발/디버깅 전용
- **권장**: 프로덕션 빌드에서 제외

### DebugFirestoreActivity.java
- **역할**: Firestore 구조 분석 및 데이터 복사 도구
- **기능**:
  - 컬렉션 구조 로그 출력
  - 문서 복사 (cohort 변경)
  - 카테고리 병합 (학부공통필수 → 학부공통)
  - 전공/교양 연결 확인
- **용도**: 개발/디버깅 전용
- **보안**: AndroidManifest에서 `exported="false"`로 설정됨

---

## 📊 코드 통계

### 전체 Java 파일 수: **80개**

### 카테고리별 분류:
- **Activity**: 34개
- **Fragment**: 7개
- **Adapter**: 12개
- **Data Model**: 15개
- **Utility/Manager**: 6개
- **Custom View**: 2개
- **기타**: 4개

### 주요 대용량 파일:
1. **FirebaseDataManager.java**: ~3,800 라인 (핵심 데이터 관리)
2. **GraduationAnalysisResultActivity.java**: ~3,600 라인 (졸업요건 분석 UI)
3. **GraduationRules.java**: ~2,800 라인 (졸업요건 분석 로직)

---

## 🔄 주요 사용자 플로우

### 1. 신규 사용자 온보딩
```
LoginActivity (회원가입)
  → SignUpActivity
  → LoginActivity (자동 로그인)
  → MainActivityNew
  → UserInfoActivity (학적 정보 입력)
  → LoadingUserInfoActivity
  → AdditionalRequirementsActivity
  → CourseInputActivity (과목 입력)
  → GraduationAnalysisResultActivity (결과 확인)
```

### 2. 기존 사용자 로그인
```
LoginActivity (자동 로그인)
  → MainActivityNew (HomeFragment)
```

### 3. 졸업요건 재분석
```
UserProfileFragment (졸업요건 재분석 버튼)
  → LoadingUserInfoActivity
  → GraduationAnalysisActivity
  → GraduationAnalysisResultActivity
```

### 4. 수강과목 추천
```
HomeFragment (수강과목 추천 버튼)
  → CourseRecommendationActivity
  → RecommendationResultActivity
```

### 5. 관리자 졸업요건 관리
```
AdminActivity
  → GraduationRequirementsActivity
  → GraduationRequirementEditActivity
    - CreditRequirementsFragment
    - MajorCoursesFragment
    - GeneralCoursesFragment
    - ReplacementRulesFragment
```

---

## 🗂️ Firebase Firestore 컬렉션 구조

### 주요 컬렉션:
- **users**: 사용자 정보 (Student 모델)
- **graduation_requirements**: 졸업요건 문서 (V1 및 통합 문서)
- **graduation_requirements_v2**: V2 졸업요건 문서 (현재 미사용)
- **banners**: 홈 화면 배너 데이터
- **certificates**: 자격증 정보
- **document_folders**: 서류 폴더
- **document_files**: 서류 파일

---

## 📝 참고 문서

이 문서와 함께 다음 문서들을 참고하세요:
- **ARCHITECTURE.md**: 전체 앱 아키텍처 설명
- **FIRESTORE_STRUCTURE.md**: Firestore 데이터 구조 상세
- **CREDIT_OVERFLOW_GUIDE.md**: 학점 초과 이수 처리 가이드
- **README.md**: 프로젝트 개요 및 설정 방법

---

## ⚠️ 주의사항

### 삭제 검토 필요:
- **AddScheduleActivity**: 구현 완료, 앱에 통합되지 않음
- **GraduationRequirementDetailActivity**: 코드상 호출 없음

### 개발 전용 (프로덕션 제외 권장):
- **DataViewerActivity**
- **DebugFirestoreActivity**

---

마지막 업데이트: 2025-10-31
