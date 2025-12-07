# 📚 SakMvp1 프로젝트 학습 가이드

> 나사렛대학교 졸업 요건 분석 앱을 통해 배우는 Android 개발 완벽 가이드

## 📖 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [핵심 기능 상세 분석](#2-핵심-기능-상세-분석)
3. [아키텍처 패턴](#3-아키텍처-패턴)
4. [주요 기술 스택](#4-주요-기술-스택)
5. [데이터베이스 설계](#5-데이터베이스-설계)
6. [핵심 컴포넌트 분석](#6-핵심-컴포넌트-분석)
7. [성능 최적화 기법](#7-성능-최적화-기법)
8. [학습 로드맵](#8-학습-로드맵)
9. [실습 과제](#9-실습-과제)

---

## 1. 프로젝트 개요

### 1.1 프로젝트 목적
- 나사렛대학교 학생들의 졸업 요건 관리 및 분석
- 학점 이수 현황 시각화 및 진도 추적
- 수강 계획 수립 및 과목 추천

### 1.2 기술적 학습 목표
- Android Native 개발 (Java)
- Firebase 백엔드 서비스 활용
- Material Design 3 UI/UX 구현
- MVC 아키텍처 패턴 적용
- 성능 최적화 기법

### 1.3 프로젝트 규모
- **Activity 수**: 40+ 개
- **Fragment 수**: 10+ 개
- **데이터 모델**: 15+ 개
- **Firebase 컬렉션**: 5개

---

## 2. 핵심 기능 상세 분석

### 2.1 졸업 요건 분석 시스템

#### 개념
학생의 학번, 학부, 트랙 정보를 기반으로 개인별 졸업 요건을 계산하고 분석하는 시스템

#### 주요 컴포넌트
```java
GraduationAnalysisActivity.java
├── 사용자 입력 수집 (학번, 학부, 트랙)
├── FirebaseDataManager 통해 졸업 요건 데이터 조회
└── GraduationAnalysisResultActivity로 결과 전달

GraduationAnalysisResultActivity.java
├── 카테고리별 학점 계산
├── DonutChartView로 시각화
└── 세부 분석 결과 표시
```

#### 학습 포인트
- **Firebase Firestore 쿼리**: 복합 조건으로 데이터 조회
- **데이터 매핑**: Firestore 문서 → Java 객체 변환
- **비즈니스 로직**: 학점 계산 알고리즘
- **UI 업데이트**: 비동기 데이터 로딩 후 UI 반영

#### 코드 예시
```java
// FirebaseDataManager에서 졸업 요건 조회
firebaseDataManager.getGraduationRequirements(
    studentYear,
    department,
    track,
    new FirebaseDataManager.GraduationRequirementsCallback() {
        @Override
        public void onSuccess(GraduationRules rules) {
            // 졸업 요건 데이터 처리
            analyzeGraduation(rules);
        }

        @Override
        public void onFailure(String error) {
            // 에러 처리
        }
    }
);
```

#### 실습 과제
1. 졸업 요건 데이터 구조 분석하기
2. 학점 계산 알고리즘 이해하기
3. 새로운 학부/트랙 추가하기

---

### 2.2 시각적 진도 관리

#### 개념
도넛 차트를 사용하여 졸업 진행률을 직관적으로 표시

#### 주요 컴포넌트
```java
DonutChartView.java
├── Custom View 상속
├── Canvas API로 도넛 차트 그리기
├── 애니메이션 효과
└── 터치 이벤트 처리
```

#### 학습 포인트
- **Custom View 생성**: View 클래스 상속 및 onDraw 구현
- **Canvas API**: 원, 호, 텍스트 그리기
- **Paint 객체**: 색상, 스타일, 두께 설정
- **ValueAnimator**: 부드러운 애니메이션 구현

#### 코드 분석
```java
public class DonutChartView extends View {
    private Paint paint;
    private RectF oval;
    private float progress; // 0.0 ~ 1.0

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 배경 원 그리기
        paint.setColor(backgroundColor);
        canvas.drawArc(oval, 0, 360, false, paint);

        // 진행률 호 그리기
        paint.setColor(progressColor);
        float sweepAngle = 360 * progress;
        canvas.drawArc(oval, -90, sweepAngle, false, paint);

        // 중앙 텍스트
        String percentage = (int)(progress * 100) + "%";
        canvas.drawText(percentage, centerX, centerY, textPaint);
    }
}
```

#### 실습 과제
1. 도넛 차트 색상 변경하기
2. 여러 세그먼트 표시하기 (전공/교양 구분)
3. 터치 시 상세 정보 표시하기

---

### 2.3 수강 강의 관리

#### 개념
학생이 수강한 과목을 입력하고 카테고리별로 분류하는 시스템

#### 주요 컴포넌트
```java
CourseInputActivity.java
├── 과목 검색 (자동완성)
├── 카테고리별 입력 (전공필수, 전공선택 등)
├── 학점 오버플로우 처리
└── Firestore 저장
```

#### 학습 포인트
- **AutoCompleteTextView**: 자동완성 기능 구현
- **RecyclerView**: 동적 리스트 표시
- **데이터 검증**: 중복 체크, 학점 범위 검증
- **오버플로우 로직**: 초과 학점 재분배 알고리즘

#### 학점 오버플로우 시스템
```java
// 20-22학번: 모든 초과 → 일반선택
if (cohort >= 2020 && cohort <= 2022) {
    if (전공필수 > 요구학점) {
        초과학점 = 전공필수 - 요구학점;
        일반선택 += 초과학점;
    }
}

// 23-25학번: 모든 초과 → 잔여학점
if (cohort >= 2023 && cohort <= 2025) {
    if (전공필수 > 요구학점) {
        초과학점 = 전공필수 - 요구학점;
        잔여학점 += 초과학점;
    }
}
```

#### 실습 과제
1. 새로운 과목 카테고리 추가하기
2. 학점 계산 로직 디버깅하기
3. 과목 삭제 기능 구현하기

---

### 2.4 Firebase 인증 시스템

#### 개념
Firebase Authentication을 활용한 사용자 로그인/회원가입

#### 주요 컴포넌트
```java
LoginActivity.java
├── 이메일/비밀번호 로그인
├── 자동 로그인 (SharedPreferences)
└── 에러 처리

SignUpActivity.java
├── 회원가입 폼 검증
├── Firebase 사용자 생성
└── Firestore 사용자 문서 생성
```

#### 학습 포인트
- **Firebase Authentication API**
- **비동기 콜백 처리**
- **입력 검증 (이메일 형식, 비밀번호 강도)**
- **SharedPreferences로 로그인 상태 유지**

#### 코드 예시
```java
// 로그인
FirebaseAuth.getInstance()
    .signInWithEmailAndPassword(email, password)
    .addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
            FirebaseUser user = task.getResult().getUser();
            // 로그인 성공 처리
        } else {
            // 에러 처리
        }
    });

// 회원가입
FirebaseAuth.getInstance()
    .createUserWithEmailAndPassword(email, password)
    .addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
            String userId = task.getResult().getUser().getUid();
            // Firestore에 사용자 정보 저장
            saveUserToFirestore(userId, userData);
        }
    });
```

#### 실습 과제
1. 비밀번호 재설정 기능 추가하기
2. Google 소셜 로그인 구현하기
3. 이메일 인증 기능 추가하기

---

### 2.5 시간표 관리 시스템

#### 개념
주간 시간표를 시각적으로 표시하고 수업을 관리하는 기능

#### 주요 컴포넌트
```java
TimeTableFragment.java
├── 격자형 시간표 뷰
├── 수업 블록 표시
└── 터치 이벤트 처리

AddScheduleActivity.java
├── 수업 정보 입력
├── 시간 충돌 검사
└── Firestore 저장

SavedTimetablesActivity.java
├── 여러 시간표 관리
├── 시간표 전환
└── 시간표 공유
```

#### 학습 포인트
- **GridLayout 또는 Custom View로 시간표 구현**
- **시간 충돌 알고리즘**
- **다중 시간표 관리**
- **로컬 저장소 (SharedPreferences/Room)**

#### 시간 충돌 검사 알고리즘
```java
public boolean isTimeConflict(Schedule newSchedule, List<Schedule> existingSchedules) {
    for (Schedule existing : existingSchedules) {
        // 같은 요일인지 확인
        if (newSchedule.dayOfWeek == existing.dayOfWeek) {
            // 시간 겹침 확인
            if (newSchedule.startTime < existing.endTime &&
                newSchedule.endTime > existing.startTime) {
                return true; // 충돌
            }
        }
    }
    return false; // 충돌 없음
}
```

#### 실습 과제
1. 시간표 색상 커스터마이징 기능 추가
2. 시간표 이미지로 저장하기
3. 친구 시간표와 비교 기능 구현

---

## 3. 아키텍처 패턴

### 3.1 MVC (Model-View-Controller) 패턴

#### 구조
```
Model (데이터)
├── GraduationRules.java
├── Student.java
├── CourseInfo.java
└── FirebaseDataManager.java

View (UI)
├── activity_*.xml (레이아웃)
├── DonutChartView.java (커스텀 뷰)
└── Adapter 클래스들

Controller (로직)
├── *Activity.java
├── *Fragment.java
└── Manager 클래스들
```

#### 장점
- 역할 분리로 코드 가독성 향상
- 유지보수 용이
- 테스트 가능성 증가

#### 한계
- 대규모 프로젝트에서 Controller 비대화
- View-Model 간 강한 결합

---

### 3.2 싱글톤 패턴

#### FirebaseDataManager 분석
```java
public class FirebaseDataManager {
    private static FirebaseDataManager instance;
    private FirebaseFirestore db;

    private FirebaseDataManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static FirebaseDataManager getInstance() {
        if (instance == null) {
            instance = new FirebaseDataManager();
        }
        return instance;
    }

    // 데이터 조회/저장 메서드들
}
```

#### 사용 이유
- 전역 접근 가능
- 메모리 절약 (인스턴스 1개)
- 상태 공유 용이

#### 주의사항
- 멀티스레드 안전성 (동기화 필요)
- 테스트 어려움
- 의존성 주입 고려

---

## 4. 주요 기술 스택

### 4.1 Firebase Firestore

#### 개념
NoSQL 클라우드 데이터베이스, 실시간 동기화 지원

#### 주요 기능
```java
// 문서 읽기
db.collection("users").document(userId)
    .get()
    .addOnSuccessListener(documentSnapshot -> {
        String name = documentSnapshot.getString("name");
    });

// 문서 쓰기
Map<String, Object> userData = new HashMap<>();
userData.put("name", "홍길동");
userData.put("studentYear", 2023);

db.collection("users").document(userId)
    .set(userData)
    .addOnSuccessListener(aVoid -> {
        // 저장 성공
    });

// 쿼리
db.collection("graduation_requirements")
    .whereEqualTo("department", "IT학부")
    .whereEqualTo("track", "멀티미디어")
    .get()
    .addOnSuccessListener(querySnapshot -> {
        for (DocumentSnapshot doc : querySnapshot) {
            // 문서 처리
        }
    });
```

#### 학습 자료
- [Firestore 공식 문서](https://firebase.google.com/docs/firestore)
- 컬렉션 vs 문서 개념
- 쿼리 최적화
- 보안 규칙 설정

---

### 4.2 Material Design 3

#### 주요 컴포넌트
```xml
<!-- MaterialButton -->
<com.google.android.material.button.MaterialButton
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="졸업 분석 시작"
    app:cornerRadius="8dp"
    app:elevation="4dp" />

<!-- TextInputLayout -->
<com.google.android.material.textfield.TextInputLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="이메일">

    <com.google.android.material.textfield.TextInputEditText
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
</com.google.android.material.textfield.TextInputLayout>

<!-- BottomNavigationView -->
<com.google.android.material.bottomnavigation.BottomNavigationView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:menu="@menu/bottom_navigation" />
```

#### 테마 시스템
```xml
<!-- themes.xml -->
<style name="Theme.SakMvp1" parent="Theme.Material3.DayNight">
    <item name="colorPrimary">@color/md_theme_primary</item>
    <item name="colorSecondary">@color/md_theme_secondary</item>
    <item name="colorTertiary">@color/md_theme_tertiary</item>
</style>
```

---

### 4.3 RecyclerView

#### 개념
효율적인 리스트 표시를 위한 뷰

#### 구현 단계
```java
// 1. Adapter 생성
public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.ViewHolder> {

    private List<Course> courses;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView courseName;
        TextView credits;

        public ViewHolder(View itemView) {
            super(itemView);
            courseName = itemView.findViewById(R.id.courseName);
            credits = itemView.findViewById(R.id.credits);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_course, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Course course = courses.get(position);
        holder.courseName.setText(course.getName());
        holder.credits.setText(course.getCredits() + "학점");
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }
}

// 2. Activity에서 사용
RecyclerView recyclerView = findViewById(R.id.recyclerView);
recyclerView.setLayoutManager(new LinearLayoutManager(this));
CourseAdapter adapter = new CourseAdapter(courseList);
recyclerView.setAdapter(adapter);
```

---

## 5. 데이터베이스 설계

### 5.1 Firestore 컬렉션 구조

#### graduation_requirements 컬렉션
```javascript
{
  // 문서 ID: "IT학부_멀티미디어_2023"
  "department": "IT학부",
  "track": "멀티미디어",
  "cohort": 2023,

  // 학점 요건
  "전공필수": 42,
  "전공선택": 18,
  "교양필수": 27,
  "교양선택": 6,
  "학부공통": 12,
  "소양": 6,
  "잔여학점": 19,
  "총졸업학점": 130,

  // 졸업 규칙
  "rules": {
    "전공필수": [
      "프로그래밍기초",
      "자료구조",
      "알고리즘",
      // ...
    ],
    "전공선택": [
      "웹프로그래밍",
      "모바일프로그래밍",
      // ...
    ],
    "교양필수": {
      "oneOf": [
        ["채플1", "채플2", "채플3", "채플4"],
        ["기독교의이해"]
      ]
    }
  },

  // 대체 과목 규칙
  "replacementCourses": {
    "자료구조": ["자료구조및실습", "데이터구조"],
    "알고리즘": ["알고리즘분석", "알고리즘및응용"]
  }
}
```

#### users 컬렉션
```javascript
{
  // 문서 ID: Firebase UID
  "name": "홍길동",
  "email": "hong@example.com",
  "studentYear": 2023,
  "department": "IT학부",
  "track": "멀티미디어",
  "signUpDate": Timestamp,
  "lastGraduationCheckDate": Timestamp,

  // 서브컬렉션: graduation_check_history
  "graduation_check_history": {
    // 문서 ID: 자동 생성
    "abc123": {
      "checkedAt": Timestamp,
      "year": 2023,
      "department": "IT학부",
      "track": "멀티미디어",
      "courses": [
        {
          "name": "프로그래밍기초",
          "credits": 3,
          "category": "전공필수",
          "semester": "2023-1"
        },
        // ...
      ],
      "additionalRequirements": {
        "채플": 4,
        "TLC": 2,
        "사회봉사": true
      },
      "analysisResult": {
        "전공필수": { "required": 42, "completed": 36 },
        "전공선택": { "required": 18, "completed": 12 },
        // ...
      }
    }
  }
}
```

### 5.2 데이터 모델링 원칙

#### 정규화 vs 비정규화
```
정규화 (관계형 DB 스타일)
- 장점: 데이터 중복 최소화, 일관성 유지
- 단점: 여러 번의 쿼리 필요, 복잡한 조인

비정규화 (NoSQL 스타일) ← Firestore 권장
- 장점: 빠른 읽기, 단일 쿼리로 데이터 조회
- 단점: 데이터 중복, 업데이트 복잡
```

#### 이 프로젝트의 선택
- 졸업 요건: 비정규화 (모든 정보를 하나의 문서에)
- 사용자 이력: 서브컬렉션 사용 (시간순 데이터)

---

## 6. 핵심 컴포넌트 분석

### 6.1 FirebaseDataManager

#### 역할
- Firebase Firestore와의 모든 통신 담당
- 데이터 캐싱
- 콜백 인터페이스 제공

#### 주요 메서드
```java
public class FirebaseDataManager {

    // 졸업 요건 조회
    public void getGraduationRequirements(
        int studentYear,
        String department,
        String track,
        GraduationRequirementsCallback callback
    ) {
        String docId = department + "_" + track + "_" + studentYear;

        db.collection("graduation_requirements")
            .document(docId)
            .get()
            .addOnSuccessListener(doc -> {
                GraduationRules rules = doc.toObject(GraduationRules.class);
                callback.onSuccess(rules);
            })
            .addOnFailureListener(e -> {
                callback.onFailure(e.getMessage());
            });
    }

    // 사용자 데이터 저장
    public void saveUserData(String userId, Map<String, Object> data) {
        db.collection("users").document(userId)
            .set(data, SetOptions.merge());
    }

    // 졸업 검사 이력 저장
    public void saveGraduationCheckHistory(
        String userId,
        GraduationAnalysisResult result
    ) {
        db.collection("users").document(userId)
            .collection("graduation_check_history")
            .add(result.toMap());
    }
}
```

#### 콜백 인터페이스
```java
public interface GraduationRequirementsCallback {
    void onSuccess(GraduationRules rules);
    void onFailure(String error);
}
```

---

### 6.2 GraduationRules 모델

#### 구조
```java
public class GraduationRules {
    private String department;
    private String track;
    private int cohort;

    // 학점 요건
    private int 전공필수;
    private int 전공선택;
    private int 교양필수;
    private int 교양선택;
    private int 학부공통;
    private int 소양;
    private int 잔여학점;
    private int 총졸업학점;

    // 과목 리스트
    private List<String> 전공필수과목;
    private List<String> 전공선택과목;
    private Map<String, Object> 교양필수그룹;

    // 대체 과목 규칙
    private Map<String, List<String>> replacementCourses;

    // Getter/Setter 생략
}
```

---

### 6.3 도넛 차트 상세 분석

#### DonutChartView.java
```java
public class DonutChartView extends View {

    // 페인트 객체
    private Paint arcPaint;
    private Paint textPaint;
    private Paint backgroundPaint;

    // 데이터
    private float progress; // 0.0 ~ 1.0
    private int currentValue;
    private int maxValue;

    // 애니메이션
    private ValueAnimator animator;

    public DonutChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Paint 초기화
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(40f);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(48f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int radius = Math.min(centerX, centerY) - 60;

        RectF oval = new RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        );

        // 배경 원
        backgroundPaint.setColor(Color.LTGRAY);
        canvas.drawArc(oval, 0, 360, false, backgroundPaint);

        // 진행률 호
        arcPaint.setColor(getProgressColor());
        float sweepAngle = 360 * progress;
        canvas.drawArc(oval, -90, sweepAngle, false, arcPaint);

        // 중앙 텍스트
        String text = currentValue + "/" + maxValue;
        canvas.drawText(text, centerX, centerY, textPaint);

        String percentage = (int)(progress * 100) + "%";
        canvas.drawText(percentage, centerX, centerY + 60, textPaint);
    }

    public void setProgress(int current, int max) {
        this.currentValue = current;
        this.maxValue = max;

        // 애니메이션
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0f, (float)current / max);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate(); // 다시 그리기
        });
        animator.start();
    }

    private int getProgressColor() {
        if (progress < 0.5f) return Color.RED;
        if (progress < 0.8f) return Color.YELLOW;
        return Color.GREEN;
    }
}
```

#### 학습 포인트
1. **Canvas API**: 2D 그래픽 그리기
2. **Paint 설정**: 색상, 두께, 스타일
3. **ValueAnimator**: 부드러운 애니메이션
4. **invalidate()**: View 다시 그리기 요청

---

## 7. 성능 최적화 기법

### 7.1 Single-flight 패턴

#### 문제점
```java
// 같은 데이터를 여러 번 요청
getGraduationRequirements(...); // 요청 1
getGraduationRequirements(...); // 요청 2 (중복!)
getGraduationRequirements(...); // 요청 3 (중복!)
```

#### 해결책
```java
private Map<String, Task<DocumentSnapshot>> ongoingRequests = new HashMap<>();

public Task<DocumentSnapshot> getDocument(String docId) {
    // 이미 진행 중인 요청이 있으면 재사용
    if (ongoingRequests.containsKey(docId)) {
        return ongoingRequests.get(docId);
    }

    // 새 요청 시작
    Task<DocumentSnapshot> task = db.collection("data").document(docId).get();
    ongoingRequests.put(docId, task);

    // 완료 후 제거
    task.addOnCompleteListener(t -> {
        ongoingRequests.remove(docId);
    });

    return task;
}
```

---

### 7.2 캐싱 시스템

#### 5분 메모리 캐시
```java
private Map<String, CachedDocument> cache = new HashMap<>();

private static class CachedDocument {
    DocumentSnapshot snapshot;
    long timestamp;

    boolean isValid() {
        long age = System.currentTimeMillis() - timestamp;
        return age < 5 * 60 * 1000; // 5분
    }
}

public void getDocumentWithCache(String docId, Callback callback) {
    // 캐시 확인
    if (cache.containsKey(docId) && cache.get(docId).isValid()) {
        callback.onSuccess(cache.get(docId).snapshot);
        return;
    }

    // 네트워크 요청
    db.collection("data").document(docId)
        .get()
        .addOnSuccessListener(doc -> {
            // 캐시 저장
            cache.put(docId, new CachedDocument(doc, System.currentTimeMillis()));
            callback.onSuccess(doc);
        });
}
```

---

### 7.3 배치 로딩

#### Before (순차 로딩)
```java
// 느림: 3개의 순차적 네트워크 요청
loadUserData(() -> {
    loadGraduationRules(() -> {
        loadCourseHistory(() -> {
            // 모두 완료
        });
    });
});
```

#### After (병렬 로딩)
```java
// 빠름: 3개의 동시 요청
Task<DocumentSnapshot> userTask = loadUserData();
Task<DocumentSnapshot> rulesTask = loadGraduationRules();
Task<QuerySnapshot> historyTask = loadCourseHistory();

Tasks.whenAllSuccess(userTask, rulesTask, historyTask)
    .addOnSuccessListener(results -> {
        // 모두 완료
    });
```

---

### 7.4 디바운스 (Debounce)

#### 개념
연속된 이벤트 중 마지막 이벤트만 처리

#### 구현
```java
private Handler debounceHandler = new Handler();
private Runnable debounceRunnable;

public void onSearchTextChanged(String query) {
    // 이전 예약 취소
    if (debounceRunnable != null) {
        debounceHandler.removeCallbacks(debounceRunnable);
    }

    // 새로운 검색 예약 (100ms 후)
    debounceRunnable = () -> {
        performSearch(query);
    };
    debounceHandler.postDelayed(debounceRunnable, 100);
}
```

---

## 8. 학습 로드맵

### 8.1 초급 (1-2주)

#### Week 1: 기초 이해
- [ ] Android 프로젝트 구조 파악
- [ ] Activity 생명주기 이해
- [ ] XML 레이아웃 작성
- [ ] Intent로 화면 전환

#### Week 2: UI 구성
- [ ] RecyclerView 사용법
- [ ] Material Design 컴포넌트
- [ ] 커스텀 뷰 기초 (DonutChartView 분석)

### 8.2 중급 (3-4주)

#### Week 3: Firebase 기초
- [ ] Firestore 데이터 읽기/쓰기
- [ ] Firebase Authentication
- [ ] 비동기 처리 (콜백, Task)

#### Week 4: 비즈니스 로직
- [ ] 학점 계산 알고리즘
- [ ] 데이터 검증
- [ ] 오버플로우 처리 로직

### 8.3 고급 (5-6주)

#### Week 5: 성능 최적화
- [ ] 캐싱 구현
- [ ] Single-flight 패턴
- [ ] 배치 로딩

#### Week 6: 고급 기능
- [ ] 커스텀 뷰 애니메이션
- [ ] 복잡한 데이터 구조 설계
- [ ] 에러 처리 및 로깅

---

## 9. 실습 과제

### 9.1 기초 과제

#### 과제 1: 새로운 Activity 추가
- 목표: 학점 계산기 Activity 생성
- 요구사항:
  - 과목명, 학점, 성적 입력
  - 평균 학점(GPA) 계산
  - 결과를 텍스트로 표시

#### 과제 2: RecyclerView 구현
- 목표: 수강 과목 목록 표시
- 요구사항:
  - 과목명, 학점, 카테고리 표시
  - 클릭 시 상세 정보 표시
  - 삭제 버튼 추가

### 9.2 중급 과제

#### 과제 3: Firebase 연동
- 목표: Firestore에서 데이터 읽어오기
- 요구사항:
  - graduation_requirements 컬렉션 조회
  - 데이터를 RecyclerView에 표시
  - 로딩 상태 표시

#### 과제 4: 커스텀 뷰 개선
- 목표: DonutChartView에 기능 추가
- 요구사항:
  - 여러 세그먼트 표시 (전공/교양 구분)
  - 각 세그먼트 다른 색상
  - 터치 시 해당 카테고리 정보 표시

### 9.3 고급 과제

#### 과제 5: 추천 알고리즘 개선
- 목표: 더 스마트한 과목 추천
- 요구사항:
  - 현재 학기, 선수과목 고려
  - 졸업까지 남은 학기 계산
  - 학기별 수강 계획 제안

#### 과제 6: 성능 최적화
- 목표: 앱 성능 측정 및 개선
- 요구사항:
  - 앱 시작 시간 측정
  - 메모리 사용량 모니터링
  - 불필요한 네트워크 요청 제거

---

## 10. 디버깅 및 테스트

### 10.1 로그 사용법

```java
// 로그 레벨
Log.v(TAG, "Verbose"); // 상세 정보
Log.d(TAG, "Debug");   // 디버그 정보
Log.i(TAG, "Info");    // 일반 정보
Log.w(TAG, "Warning"); // 경고
Log.e(TAG, "Error");   // 에러

// 실전 예시
private static final String TAG = "GraduationAnalysis";

public void analyzeGraduation() {
    Log.d(TAG, "졸업 분석 시작");

    try {
        // 분석 로직
        Log.i(TAG, "분석 완료: " + result);
    } catch (Exception e) {
        Log.e(TAG, "분석 실패", e);
    }
}
```

### 10.2 Firestore 디버깅

```java
// DebugFirestoreActivity 활용
public class DebugFirestoreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 모든 컬렉션 조회
        FirebaseFirestore.getInstance()
            .collection("graduation_requirements")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                for (DocumentSnapshot doc : querySnapshot) {
                    Log.d("Firestore", doc.getId() + " => " + doc.getData());
                }
            });
    }
}
```

---

## 11. 참고 자료

### 11.1 공식 문서
- [Android Developers](https://developer.android.com/)
- [Firebase Documentation](https://firebase.google.com/docs)
- [Material Design](https://m3.material.io/)

### 11.2 추천 학습 자료
- Android 공식 Codelabs
- Udacity Android 강좌
- Firebase YouTube 채널

### 11.3 유용한 도구
- Android Studio Profiler (성능 분석)
- Layout Inspector (UI 분석)
- Firebase Console (데이터 관리)
- Logcat (로그 확인)

---

## 12. FAQ

### Q1: Firestore와 Realtime Database의 차이는?
**A:** Firestore는 더 강력한 쿼리, 자동 스케일링, 오프라인 지원이 우수합니다. 이 프로젝트는 복잡한 쿼리가 필요하여 Firestore를 선택했습니다.

### Q2: 왜 MVVM이 아닌 MVC를 사용했나요?
**A:** 프로젝트 규모가 중소형이고, 팀의 Java 기반 개발 경험을 활용하기 위해 MVC를 선택했습니다.

### Q3: Room 대신 Firestore만 사용하는 이유는?
**A:** 클라우드 동기화가 핵심 요구사항이며, 오프라인 지원은 Firestore의 내장 캐시로 충분합니다.

### Q4: 커스텀 뷰를 사용하는 이유는?
**A:** 도넛 차트 같은 특수한 UI는 표준 뷰로 구현이 어렵고, 커스텀 뷰로 완전한 제어와 성능 최적화가 가능합니다.

---

## 13. 프로젝트 확장 아이디어

### 13.1 단기 개선 (1-2주)
- [ ] 다크 모드 지원
- [ ] 알림 기능 (수강신청 기간 알림)
- [ ] 통계 대시보드 (학년별 진행률 비교)

### 13.2 중기 개선 (1-2개월)
- [ ] 친구 기능 (시간표 공유)
- [ ] 학점 시뮬레이터 (예상 성적으로 졸업 가능성 예측)
- [ ] AI 기반 과목 추천

### 13.3 장기 개선 (3-6개월)
- [ ] 다른 대학 지원
- [ ] 웹 버전 개발
- [ ] 교수님용 관리 도구

---

**작성일**: 2025년 12월 2일
**버전**: 1.0
**문의**: 프로젝트 GitHub Issues

> 🎓 이 가이드로 Android 개발의 전반적인 흐름을 익히고, 실무에 바로 적용할 수 있는 기술을 습득하세요!
