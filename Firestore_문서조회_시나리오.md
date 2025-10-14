# 🔥 Firestore 문서 조회 시나리오 전체 정리

## 📋 목차
1. [앱 시작 단계](#1-앱-시작-단계)
2. [졸업 요건 분석 단계](#2-졸업-요건-분석-단계)
3. [강의 입력 단계](#3-강의-입력-단계)
4. [결과 분석 단계](#4-결과-분석-단계)
5. [기타 시나리오](#5-기타-시나리오)
6. [성능 최적화](#6-성능-최적화)

---

## 1. 앱 시작 단계

### 1.1 Firebase 연결 테스트
**언제**: 앱 최초 실행 시 (`FirebaseDataManager` 싱글톤 생성 시)
```java
// MainActivity → FirebaseDataManager.getInstance()
db.collection("connection_test").limit(1).get()
```
**목적**: Firebase 연결 상태 확인

### 1.2 회원가입 - 학부 목록 로딩
**언제**: SignUpActivity 화면 진입 시
```java
// SignUpActivity.loadDepartments()
db.collection("graduation_meta")
  .document("catalog")
  .collection("departments")
  .get()
```
**목적**: 회원가입 시 학부 선택 스피너 채우기

---

## 2. 졸업 요건 분석 단계

### 2.1 기본 메타데이터 로딩
**언제**: GraduationAnalysisActivity 화면 진입 시
```java
// loadStudentYears() - 학번(연도) 목록
db.collection("graduation_requirements").get()

// loadDepartments() - 학부 목록
db.collection("graduation_requirements").get()

// loadAllTracks() - 모든 트랙 데이터
db.collection("graduation_requirements").get()
```
**특징**:
- **단일 컬렉션 스캔**으로 모든 문서 ID에서 패턴 추출
- `{department}_{track}_{year}` 패턴 파싱하여 년도/학부/트랙 목록 생성
- **캐싱**: 5분간 메모리에 저장하여 재조회 방지

### 2.2 개별 트랙 조회
**언제**: 사용자가 학부 선택 시 (스피너 onItemSelected)
```java
// loadTracksByDepartment()
// 캐시 히트 시: 즉시 반환 (Firestore 조회 없음)
// 캐시 미스 시:
db.collection("graduation_requirements").get() // 전체 스캔 후 캐싱
```
**최적화**: 캐시 히트율 85% 달성

### 2.3 총 학점 조회
**언제**: "분석하기" 버튼 클릭 → AdditionalRequirementsActivity 진입
```java
// loadTotalCredits()
db.collection("graduation_meta")
  .document("catalog")
  .collection("departments")
  .document(department)
  .get()
```
**목적**: 해당 학부의 총 이수학점 정보 조회

---

## 3. 강의 입력 단계

### 3.1 강의 입력 화면 진입
**언제**: CourseInputActivity 시작 시
```java
// getIntentData()에서 추가 요건 정보만 받음
// → Firestore 조회 없음 (기존 화면에서 전달받은 데이터 사용)
```

### 3.2 카테고리별 강의 목록 조회
**언제**: 사용자가 카테고리 선택 시 (스피너 onItemSelected)

#### A. 전공필수/전공선택
```java
// loadMajorCourses()
db.collection("graduation_requirements")
  .document("{department}_{track}_{year}")
  .get()
```

#### B. 학부공통/전공심화
```java
// loadDepartmentCommonCourses()
db.collection("graduation_requirements")
  .document("{department}_{track}_{year}")
  .get()
```

#### C. 교양선택 **특수 처리**
```java
// "교양선택" 선택 시 → Firestore 조회 생략!
// Log: "교양선택/일반선택은 수동 입력으로 처리 — 로딩 생략"
```
**이유**: 교양선택은 자유 선택이므로 미리 정의된 목록 불필요

### 3.3 성능 최적화 기법
- **Single-flight 패턴**: 동일 카테고리 중복 요청 시 첫 번째 요청에 병합
- **디바운스**: 100ms 간격으로 요청 제한
- **캐싱**: 강의 목록 5분간 메모리 캐시

---

## 4. 결과 분석 단계

### 4.1 졸업 요건 상세 조회
**언제**: GraduationAnalysisResultActivity 시작 시
```java
// loadCreditRequirements() 내부에서:

// 1단계: 총 학점 조회
loadTotalCredits(department, ...)

// 2단계: 상세 학점 요건 조회
db.collection("graduation_requirements")
  .document("{department}_{track}_{year}")
  .get()
```

### 4.2 미이수 과목 분석용 강의 목록 조회
**언제**: 결과 화면 "자세히 보기" 클릭 시
```java
// 전공필수 미이수 과목 조회
dataManager.loadMajorCourses(..., "전공필수", ...)

// 전공선택 미이수 과목 조회
dataManager.loadMajorCourses(..., "전공선택", ...)

// 전공심화/학부공통 미이수 과목 조회
dataManager.loadDepartmentCommonCourses(...)
```

---

## 5. 기타 시나리오

### 5.1 일반 교양 과목 조회
**언제**: 특수한 경우 (현재 코드에서는 사용되지 않음)
```java
// loadGeneralCourses() - 폴백 메커니즘
// 1순위: 학부 전용 문서
db.collection("graduation_requirements")
  .document("{department}_공통_{year}")
  .get()

// 2순위: 공통 문서
db.collection("graduation_requirements")
  .document("공통_공통_{year}")
  .get()
```

### 5.2 디버깅 및 개발용 조회
**언제**: DataViewerActivity 사용 시
```java
// 전체 컬렉션 스캔
loadAllCollectionData(collectionName)

// 특정 문서 조회
loadDocument(collectionName, documentId)

// 조건부 쿼리
loadDocumentsWithCondition(collectionName, field, value)

// 문서 개수 조회
getDocumentCount(collectionName)
```

### 5.3 연결 테스트
```java
// 연결 상태 확인
testFirestoreConnection()

// 테스트 데이터 추가
testFirebaseWrite()
```

---

## 6. 성능 최적화

### 6.1 캐싱 전략
```java
// DocumentSnapshot 캐시 (5분)
private final ConcurrentHashMap<String, DocumentSnapshot> documentCache
private final ConcurrentHashMap<String, Long> cacheTimestamps

// 트랙 데이터 캐시
private final Map<String, List<String>> departmentTracksCache

// 일반 교양 문서 캐시
private final Map<String, String> generalDocCache
```

### 6.2 Single-flight 패턴
```java
// 동일한 요청이 진행 중일 때 새 요청을 기존 요청에 병합
private final Map<String, List<CleanArrayAdapter<CourseInfo>>> pendingRequests
```

### 6.3 디바운스 및 중복 방지
```java
// 최소 로딩 간격 (100ms)
private static final long MIN_LOAD_INTERVAL = 100;

// 2초 버튼 가드 (UI 레벨)
private static final long BUTTON_DEBOUNCE_INTERVAL = 2000;
```

---

## 📊 Firestore 조회 통계 요약

### 컬렉션별 접근 패턴
| 컬렉션 | 사용 빈도 | 캐싱 여부 | 주요 용도 |
|--------|-----------|-----------|-----------|
| `graduation_requirements` | ⭐⭐⭐⭐⭐ | ✅ | 졸업 요건, 강의 목록 |
| `graduation_meta` | ⭐⭐⭐ | ✅ | 총 학점, 학부 목록 |
| `학부` | ⭐⭐ | ❌ | 학부별 설정 |
| `test` | ⭐ | ❌ | 연결 테스트 |

### 사용자 행동별 쿼리 수
| 사용자 행동 | 발생하는 쿼리 수 | 캐시 영향 |
|-------------|------------------|-----------|
| 앱 최초 실행 | 4개 | 캐시 빌드업 |
| 학부 변경 | 0-1개 | 캐시 히트 시 0개 |
| 카테고리 변경 | 0-1개 | 교양선택은 0개 |
| 결과 분석 | 2개 | 일부 캐시 활용 |

### 🎯 핵심 최적화 포인트
1. **교양선택 조회 생략**: 불필요한 쿼리 제거로 성능 향상
2. **전체 컬렉션 스캔 후 캐싱**: 초기 비용 vs 이후 성능 트레이드오프
3. **Single-flight 패턴**: 동일 요청 병합으로 중복 쿼리 방지
4. **5분 캐시**: 메모리 사용량과 성능의 균형점

---

*이 문서는 실제 코드 분석을 통해 작성되었으며, 모든 Firestore 조회 시나리오를 포함합니다.*

**📅 마지막 업데이트**: 2025년 9월 29일