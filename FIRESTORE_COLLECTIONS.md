# Firestore 컬렉션 사용 현황

> **작성일**: 2025-01-13
> **목적**: 앱에서 사용하는 모든 Firestore 컬렉션을 정리하여 불필요한 컬렉션 삭제 가능

---

## 🎯 빠른 요약

### ✅ 필수 컬렉션 (총 9개)
```
users                             - 사용자 정보
├─ current_graduation_analysis    - 최신 졸업 분석 결과 (단일 문서)
└─ timetables                     - 시간표 목록

graduation_requirements      - 졸업 요건 (메인)
graduation_meta             - 메타데이터
└─ departments              - 학과 정보

banners                     - 홈 배너
document_folders            - 서류 폴더
└─ files                    - 서류 파일

replacement_courses         - 대체 과목
```

### 🗑️ 삭제 가능 컬렉션 (총 13개)
```
즉시 삭제 가능:
- graduation_requirements_v2  (미사용)
- test                        (테스트용)
- connection_test             (테스트용)
- user_academic_info          (미구현)
- user_course_history         (미구현)
- user_graduation_analysis    (미구현)
- graduation_check_history    (current_graduation_analysis로 대체됨)
- courses                     (current_graduation_analysis로 통합됨)
- timetables (최상위)         (users/{userId}/timetables로 이동됨)
- schedules (최상위)          (users/{userId}/timetables로 통합됨)
- student_progress            (실제 Firestore에 존재하지 않음)
- user_schedules              (실제 사용되지 않음)
- user_timetables             (실제 사용되지 않음)

확인 후 삭제:
- 학부                        (graduation_meta로 대체 확인 필요)
```

---

## 1. 사용자 관련 (User Management)

### `users` (최상위 컬렉션)
**용도**: 사용자 기본 정보 저장
**사용 위치**:
- `SignUpActivity.java` - 회원가입 시 사용자 정보 저장
- `LoadingUserInfoActivity.java` - 사용자 정보 로드
- `GraduationAnalysisActivity.java` - 졸업 분석 시 사용자 정보 조회
- `GraduationAnalysisResultActivity.java` - 졸업 분석 결과 저장
- `CourseRecommendationActivity.java` - 과목 추천 시 사용자 정보 조회
- `StudentListActivity.java` - 학생 목록 조회 (관리자)
- `StudentDetailActivity.java` - 학생 상세 정보 조회 (관리자)
- `RecommendationResultActivity.java` - 추천 결과 조회

**하위 컬렉션**:
- `users/{userId}/current_graduation_analysis` - 최신 졸업 분석 결과 (단일 문서 "latest") ✅
- `users/{userId}/timetables` - 사용자 시간표 목록 ✅

---

## 2. 졸업 요건 관련 (Graduation Requirements)

### `graduation_requirements` (최상위 컬렉션)
**용도**: 학과별/전공별 졸업 요건 데이터 (메인 버전)
**사용 위치**:
- `FirebaseDataManager.java` - 졸업 요건 데이터 로드/저장/분석
- `GraduationRequirementsActivity.java` - 졸업 요건 목록 조회
- `GraduationRequirementDetailActivity.java` - 졸업 요건 상세 조회
- `GraduationRequirementAddActivity.java` - 졸업 요건 추가
- `GraduationRequirementEditActivity.java` - 졸업 요건 수정
- `GeneralDocumentManageActivity.java` - 교양 졸업 요건 관리
- `MajorDocumentManageActivity.java` - 전공 졸업 요건 관리
- `DebugFirestoreActivity.java` - 디버그/마이그레이션
- `GeneralCoursesFragment.java` - 교양 과목 규칙 조회
- `MajorCoursesFragment.java` - 전공 과목 규칙 조회
- `ReplacementRulesFragment.java` - 대체 과목 규칙 조회

**문서 구조 예시**:
- `graduation_requirements/컴퓨터공학전공_2024`
- `graduation_requirements/교양_2024`
- `graduation_requirements/공통졸업요건_2024`

### `graduation_requirements_v2` (최상위 컬렉션)
**용도**: 졸업 요건 데이터 v2 (새 버전, 마이그레이션 테스트용?)
**사용 위치**:
- `DebugFirestoreActivity.java` - 디버그/테스트 전용

**상태**: ⚠️ **삭제 고려 대상** - 실제 앱에서 사용하지 않는 것으로 보임

---

## 3. 메타데이터 (Metadata)

### `graduation_meta` (최상위 컬렉션)
**용도**: 졸업 요건 카탈로그 및 메타 정보
**사용 위치**:
- `FirebaseDataManager.java` - 학과 정보 조회
- `BannerManagementActivity.java` - 배너 관리 시 학과 목록 조회

**하위 컬렉션**:
- `graduation_meta/catalog/departments` - 학과 목록 정보

---

## 4. 배너 관리 (Banner Management)

### `banners` (최상위 컬렉션)
**용도**: 홈 화면 배너 이미지 및 정보
**사용 위치**:
- `HomeFragment.java` - 홈 화면 배너 표시
- `BannerManagementActivity.java` - 배너 추가/수정/삭제 (관리자)

**필드 예시**:
- imageUrl: 배너 이미지 URL
- targetUrl: 클릭 시 이동 URL
- departments: 표시할 학과 목록
- order: 배너 순서
- status: 활성화 상태

---

## 5. 서류/문서 관리 (Document Management)

### `document_folders` (최상위 컬렉션)
**용도**: 서류 폴더 목록
**사용 위치**:
- `DocumentFolderActivity.java` - 서류 폴더 목록 조회
- `DocumentFolderManageActivity.java` - 폴더 관리 (관리자)
- `DocumentFilesActivity.java` - 폴더 내 파일 조회

**하위 컬렉션**:
- `document_folders/{folderId}/files` - 폴더 내 파일 목록

**필드 예시**:
- name: 폴더명
- description: 설명
- order: 정렬 순서
- visibleDepartments: 표시할 학과

---

## 6. 시간표 관련 (Timetable)

### `timetables` (최상위 컬렉션)
**용도**: 시간표 데이터
**사용 위치**:
- `TimetableFragment.java` - 시간표 표시 및 관리
- `SavedTimetableActivity.java` - 저장된 시간표 조회

### `schedules` (최상위 컬렉션)
**용도**: 일정 데이터
**사용 위치**:
- `TimetableFragment.java` - 일정 관리

---

## 7. 학생 진행 상황 (Student Progress)

### `student_progress` (최상위 컬렉션)
**용도**: 학생별 진행 상황 추적
**사용 위치**:
- `FirebaseDataManager.java` - 학생 진행 상황 저장/조회

**필드 예시**:
- userId: 사용자 ID
- department: 학과
- completedCourses: 완료한 과목
- progressData: 진행 상황 데이터

---

## 8. 대체 과목 (Replacement Courses)

### `replacement_courses` (최상위 컬렉션)
**용도**: 과목 대체 규칙 (폐지 과목 → 대체 과목)
**사용 위치**:
- `GraduationAnalysisResultActivity.java` - 졸업 분석 시 대체 과목 확인

**필드 예시**:
- discontinuedCourse: 폐지된 과목 정보
- replacementCourses: 대체 가능한 과목 목록

---

## 9. 기타 컬렉션

### `학부` (최상위 컬렉션)
**용도**: 학부 정보 (구버전?)
**사용 위치**:
- `FirebaseDataManager.java`

**상태**: ⚠️ **삭제 고려 대상** - `graduation_meta/catalog/departments`로 대체된 것으로 보임

### `test` (최상위 컬렉션)
**용도**: 테스트 데이터
**사용 위치**:
- `FirebaseDataManager.java` - Firestore 쓰기 테스트

**상태**: ⚠️ **삭제 고려 대상** - 개발/테스트 전용

### `connection_test` (최상위 컬렉션)
**용도**: Firestore 연결 테스트
**사용 위치**:
- `FirebaseDataManager.java` - 연결 상태 확인

**상태**: ⚠️ **삭제 고려 대상** - 개발/테스트 전용

---

## 컬렉션 계층 구조 요약

```
📁 Firestore Database
├── 🟢 users (필수)
│   └── {userId}
│       ├── 🟢 current_graduation_analysis (필수)
│       │   └── latest (단일 문서 - 최신 졸업 분석 결과)
│       └── 🟢 timetables (필수 - 시간표 목록)
│
├── 🟢 graduation_requirements (필수 - 메인 졸업 요건)
├── 🔴 graduation_requirements_v2 (삭제 - 사용 안함)
│
├── 🟢 graduation_meta (필수)
│   └── catalog
│       └── 🟢 departments (필수)
│
├── 🟢 banners (필수 - 홈 배너)
│
├── 🟢 document_folders (필수 - 서류함)
│   └── {folderId}
│       └── 🟢 files (필수)
│
├── 🟢 replacement_courses (필수 - 대체 과목)
│
├── 🔴 graduation_check_history (삭제 - current_graduation_analysis로 대체됨)
├── 🔴 courses (삭제 - current_graduation_analysis로 통합됨)
├── 🔴 timetables (최상위) (삭제 - users/{userId}/timetables로 이동됨)
├── 🔴 schedules (최상위) (삭제 - users/{userId}/timetables로 통합됨)
├── 🔴 student_progress (삭제 - 코드에만 존재, 실제 미사용)
├── 🔴 학부 (삭제 - graduation_meta로 대체됨)
├── 🔴 test (삭제 - 테스트 전용)
└── 🔴 connection_test (삭제 - 테스트 전용)
```

**참고**: `user_academic_info`, `user_course_history`, `user_graduation_analysis`는
UserDataManager.java에 정의되어 있으나 앱에서 실제 사용하지 않음 (미사용 코드)

## 삭제 권장 컬렉션 (🔴)

### 최상위 컬렉션

1. **`graduation_requirements_v2`**
   - 사유: DebugFirestoreActivity에서만 사용, 실제 앱 기능에서 미사용
   - 영향: 없음 (디버그 전용)

2. **`학부`**
   - 사유: `graduation_meta/catalog/departments`로 대체됨
   - 영향: FirebaseDataManager에서 제거 후 테스트 필요

3. **`test`**
   - 사유: 개발/테스트 전용 컬렉션
   - 영향: 없음 (프로덕션에서 불필요)

4. **`connection_test`**
   - 사유: 연결 테스트 전용
   - 영향: 없음 (프로덕션에서 불필요)

5. **`user_academic_info`**
   - 사유: UserDataManager에 정의되어 있으나 앱에서 호출 안함
   - 영향: 없음 (미사용 코드)

6. **`user_course_history`**
   - 사유: UserDataManager에 정의되어 있으나 앱에서 호출 안함
   - 영향: 없음 (미사용 코드)

7. **`user_graduation_analysis`**
   - 사유: UserDataManager에 정의되어 있으나 앱에서 호출 안함
   - 영향: 없음 (미사용 코드)

8. **`graduation_check_history`**
   - 사유: `current_graduation_analysis`로 대체됨 (단일 문서 방식으로 개선)
   - 영향: 없음 (코드에서 이미 마이그레이션 완료)

9. **`courses`**
   - 사유: `current_graduation_analysis`에 통합됨 (중복 제거)
   - 영향: 없음 (코드에서 이미 마이그레이션 완료)

## 삭제 가능한 컬렉션 빠른 요약

### 즉시 삭제 가능 (앱에서 미사용)
```
🗑️ graduation_requirements_v2
🗑️ test
🗑️ connection_test
🗑️ user_academic_info
🗑️ user_course_history
🗑️ user_graduation_analysis
🗑️ graduation_check_history (current_graduation_analysis로 대체)
🗑️ courses (current_graduation_analysis로 통합)
```

### 확인 후 삭제 (대체 컬렉션 확인 필요)
```
⚠️ 학부 → graduation_meta/catalog/departments 로 대체 확인 필요
```

## 주요 기능별 필수 컬렉션

### 👤 사용자 관리
- `users`
- `users/{userId}/current_graduation_analysis`

### 🎓 졸업 요건 분석
- `graduation_requirements`
- `graduation_meta`
- `replacement_courses`
- `student_progress`
- `users/{userId}/current_graduation_analysis`

### 🏠 홈 화면
- `banners`

### 📄 서류함
- `document_folders`
- `document_folders/{folderId}/files`

### 📅 시간표/일정
- `timetables`
- `schedules`

---

## 📋 체크리스트: Firestore 정리 작업

### 1단계: 백업 (필수)
- [ ] Firestore 데이터 전체 백업 완료
- [ ] 백업 파일 위치 확인: _______________

### 2단계: 즉시 삭제 가능 컬렉션
- [ ] `graduation_requirements_v2` 삭제
- [ ] `test` 삭제
- [ ] `connection_test` 삭제
- [ ] `user_academic_info` 삭제
- [ ] `user_course_history` 삭제
- [ ] `user_graduation_analysis` 삭제
- [ ] `graduation_check_history` 삭제 (current_graduation_analysis로 대체)
- [ ] `courses` 삭제 (current_graduation_analysis로 통합)

### 3단계: 확인 후 삭제
- [ ] `학부` 컬렉션과 `graduation_meta/catalog/departments` 비교
- [ ] 데이터 이전 완료 확인
- [ ] `학부` 컬렉션 삭제

### 4단계: 검증
- [ ] 앱 기능 전체 테스트
- [ ] 졸업 요건 분석 정상 작동 확인
- [ ] 시간표/일정 기능 정상 작동 확인
- [ ] 서류함 기능 정상 작동 확인

---

**최종 업데이트**: 2025-01-13
**문서 위치**: `FIRESTORE_COLLECTIONS.md`
**관련 파일**: FirebaseDataManager.java, UserDataManager.java
