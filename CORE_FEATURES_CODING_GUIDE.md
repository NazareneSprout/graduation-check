# 💻 핵심 기능 코딩 가이드

> SakMvp1 프로젝트의 핵심 기능을 코드 수준에서 이해하고 구현하는 상세 가이드

## 📖 목차

1. [싱글톤 패턴과 FirebaseDataManager](#1-싱글톤-패턴과-firebasedatamanager)
2. [졸업 요건 분석 플로우](#2-졸업-요건-분석-플로우)
3. [수강 강의 입력 시스템](#3-수강-강의-입력-시스템)
4. [커스텀 뷰 - 도넛 차트](#4-커스텀-뷰---도넛-차트)
5. [비동기 처리와 콜백 패턴](#5-비동기-처리와-콜백-패턴)
6. [캐싱 시스템](#6-캐싱-시스템)
7. [데이터 모델링과 Parcelable](#7-데이터-모델링과-parcelable)
8. [성능 최적화 테크닉](#8-성능-최적화-테크닉)

---

## 1. 싱글톤 패턴과 FirebaseDataManager

### 1.1 싱글톤 패턴이란?

**개념**: 애플리케이션 전체에서 **단 하나의 인스턴스**만 생성하여 공유하는 디자인 패턴

### 1.2 FirebaseDataManager 구조

```java
public class FirebaseDataManager {
    private static final String TAG = "FirebaseDataManager";

    // ============ 핵심 1: 싱글톤 필드 ============
    private static FirebaseDataManager instance;  // 단 하나의 인스턴스

    // ============ 핵심 2: Firestore 인스턴스 ============
    private FirebaseFirestore db;

    // ============ 핵심 3: 캐시 저장소 ============
    private Map<String, List<String>> studentYearsCache = new HashMap<>();
    private Map<String, List<CourseInfo>> coursesCache = new HashMap<>();

    // ============ 핵심 4: private 생성자 (외부 생성 차단) ============
    private FirebaseDataManager() {
        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "FirebaseDataManager 초기화");
    }

    // ============ 핵심 5: getInstance() - 접근 메서드 ============
    public static synchronized FirebaseDataManager getInstance() {
        if (instance == null) {
            instance = new FirebaseDataManager();
        }
        return instance;
    }
}
```

### 1.3 싱글톤 사용 방법

```java
// Activity A에서 사용
FirebaseDataManager manager = FirebaseDataManager.getInstance();
manager.loadStudentYears(new OnStudentYearsLoadedListener() {
    @Override
    public void onSuccess(List<String> years) {
        // 학번 데이터 처리
    }

    @Override
    public void onFailure(Exception e) {
        // 에러 처리
    }
});

// Activity B에서도 같은 인스턴스 사용
FirebaseDataManager sameManager = FirebaseDataManager.getInstance();
// sameManager == manager (true) - 같은 객체!
```

### 1.4 왜 싱글톤을 사용하나?

| 장점 | 설명 |
|------|------|
| **메모리 절약** | 인스턴스 1개만 생성 (여러 Activity에서 공유) |
| **전역 접근** | 어디서든 `getInstance()`로 접근 가능 |
| **캐시 공유** | 한 번 로드한 데이터를 모든 화면에서 재사용 |
| **상태 공유** | Firestore 연결 상태를 앱 전체가 공유 |

### 1.5 주요 메서드 분석

#### 학번 데이터 로드
```java
public void loadStudentYears(OnStudentYearsLoadedListener listener) {
    // 1단계: 캐시 확인
    String cacheKey = "student_years";
    if (studentYearsCache.containsKey(cacheKey)) {
        // 캐시 히트 - 즉시 반환 (네트워크 요청 없음)
        listener.onSuccess(studentYearsCache.get(cacheKey));
        return;
    }

    // 2단계: 캐시 미스 - Firestore에서 데이터 가져오기
    db.collection("graduation_requirements")
        .get()
        .addOnSuccessListener(queryDocumentSnapshots -> {
            Set<String> yearsSet = new HashSet<>();

            // 문서 ID에서 연도 추출 (예: "IT학부_멀티미디어_2025" → "2025")
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String docId = doc.getId();
                String[] parts = docId.split("_");
                if (parts.length >= 3) {
                    String year = parts[parts.length - 1];  // 마지막 부분
                    yearsSet.add(year);
                }
            }

            // 3단계: 정렬 및 캐시 저장
            List<String> years = new ArrayList<>(yearsSet);
            years.sort((a, b) -> b.compareTo(a));  // 최신년도 우선
            studentYearsCache.put(cacheKey, years);  // 캐시에 저장

            // 4단계: 콜백으로 결과 전달
            listener.onSuccess(years);
        })
        .addOnFailureListener(e -> {
            listener.onFailure(e);
        });
}
```

**핵심 포인트**:
1. **캐시 우선** → 빠른 응답
2. **네트워크 요청 최소화** → 성능 향상
3. **비동기 처리** → UI 블로킹 없음
4. **콜백 패턴** → 완료 시 호출

---

## 2. 졸업 요건 분석 플로우

### 2.1 전체 흐름도

```
[GraduationAnalysisActivity]
        ↓
  학번/학부/트랙 선택
        ↓
  졸업요건 존재 확인 ← FirebaseDataManager
        ↓
[AdditionalRequirementsActivity]
        ↓
  TLC/채플 등 추가요건 입력
        ↓
[CourseInputActivity]
        ↓
  수강 과목 입력 (전공/교양)
        ↓
[GraduationAnalysisResultActivity]
        ↓
  분석 결과 + 도넛 차트
```

### 2.2 GraduationAnalysisActivity 핵심 코드

#### 초기 데이터 로딩 (병렬 처리)
```java
private void loadAllInitialData() {
    studentYearsLoaded = false;
    departmentsLoaded = false;
    allTracksLoaded = false;

    // ⚡ 병렬 로딩 (3개 요청 동시에)
    loadStudentYears();   // 학번 목록
    loadDepartments();    // 학부 목록
    loadAllTracks();      // 모든 트랙 데이터
}

private void checkAndUpdateInitialDataUI() {
    // 모든 로딩 완료 확인
    if (studentYearsLoaded && departmentsLoaded && allTracksLoaded) {
        // UI 업데이트
        studentIdAdapter.clear();
        studentIdAdapter.addAll(shortYears);

        departmentAdapter.clear();
        departmentAdapter.addAll(loadedDepartments);

        Log.d(TAG, "모든 초기 데이터 로딩 완료");
    }
}
```

**핵심 포인트**:
- 3개 요청을 **순차가 아닌 병렬**로 처리
- 가장 느린 요청이 완료되면 즉시 UI 업데이트
- 기존 방식(순차): 3초 → 개선 방식(병렬): 1초

#### 졸업요건 검증
```java
private void startGraduationAnalysis() {
    // 1. 입력 검증
    if (spinnerStudentId.getSelectedItemPosition() == -1) {
        Toast.makeText(this, "모든 항목을 선택해주세요", Toast.LENGTH_SHORT).show();
        return;
    }

    // 2. 데이터 수집
    String selectedYear = "20" + selectedShortYear;  // "25" → "2025"
    String selectedDepartment = spinnerDepartment.getSelectedItem().toString();
    String selectedTrack = spinnerTrack.getSelectedItem().toString();

    // 3. 졸업요건 존재 여부 확인
    dataManager.loadGraduationRequirements(
        selectedDepartment, selectedTrack, selectedYear,
        new OnGraduationRequirementsLoadedListener() {
            @Override
            public void onSuccess(Map<String, Object> requirements) {
                // 성공: 다음 화면으로 이동
                Intent intent = new Intent(
                    GraduationAnalysisActivity.this,
                    AdditionalRequirementsActivity.class
                );
                intent.putExtra("year", selectedYear);
                intent.putExtra("department", selectedDepartment);
                intent.putExtra("track", selectedTrack);
                startActivity(intent);
            }

            @Override
            public void onFailure(Exception e) {
                // 실패: 에러 메시지
                Toast.makeText(GraduationAnalysisActivity.this,
                    "해당 조건의 졸업 요건을 찾을 수 없습니다",
                    Toast.LENGTH_SHORT).show();
            }
        }
    );
}
```

### 2.3 데이터 전달 (Intent)

```java
// Activity A에서 데이터 전송
Intent intent = new Intent(this, CourseInputActivity.class);
intent.putExtra("year", selectedYear);
intent.putExtra("department", selectedDepartment);
intent.putExtra("track", selectedTrack);
intent.putExtra("additionalRequirements", requirementsObject);  // Parcelable
startActivity(intent);

// Activity B에서 데이터 수신
Intent intent = getIntent();
String year = intent.getStringExtra("year");
String department = intent.getStringExtra("department");
String track = intent.getStringExtra("track");

// Parcelable 객체 수신
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    requirements = intent.getParcelableExtra("additionalRequirements",
                                             AdditionalRequirements.class);
} else {
    requirements = intent.getParcelableExtra("additionalRequirements");
}
```

---

## 3. 수강 강의 입력 시스템

### 3.1 다이얼로그 기반 입력 UI

```java
private void showAddCourseDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    View dialogView = inflater.inflate(R.layout.dialog_add_course, null);
    builder.setView(dialogView);
    AlertDialog dialog = builder.create();

    // UI 컴포넌트 참조
    RadioGroup radioGroupCourseType = dialogView.findViewById(R.id.radio_group_course_type);
    Spinner spinnerMajorCourses = dialogView.findViewById(R.id.spinner_major_courses);
    Spinner spinnerCourseCategory = dialogView.findViewById(R.id.spinner_course_category);
    EditText editCourseName = dialogView.findViewById(R.id.edit_course_name);

    // 전공/교양 라디오 버튼 전환
    radioGroupCourseType.setOnCheckedChangeListener((group, checkedId) -> {
        boolean isMajor = (checkedId == R.id.radio_major);
        updateCategorySpinner(categoryAdapter, isMajor);

        if (isMajor) {
            // 전공 과목 로드
            loadCoursesForCategory(selectedCategory, majorCoursesAdapter);
        } else {
            // 교양 과목 로드
            loadCoursesForCategory("교양필수", majorCoursesAdapter);
        }
    });

    dialog.show();
}
```

### 3.2 In-Flight 요청 병합 (중복 방지)

```java
private void loadCoursesForCategory(String category,
                                     CleanArrayAdapter<CourseInfo> courseAdapter) {
    long now = System.currentTimeMillis();

    // ⚡ 핵심 1: 동일 카테고리 로딩 중이면 대기열에 추가
    if (isLoadingCourses && category.equals(lastLoadedCategory)) {
        Log.d(TAG, "In-Flight 합치기: 대기열 추가 => " + category);

        List<CleanArrayAdapter<CourseInfo>> adapters = pendingRequests.get(category);
        if (adapters == null) {
            adapters = new ArrayList<>();
            pendingRequests.put(category, adapters);
        }
        adapters.add(courseAdapter);
        return;  // 네트워크 요청 하지 않음!
    }

    // ⚡ 핵심 2: 최소 간격 체크 (500ms)
    if (category.equals(lastLoadedCategory) &&
        (now - lastLoadTime) < MIN_LOAD_INTERVAL) {
        Log.d(TAG, "너무 빠른 재요청 차단");
        return;
    }

    // 실제 네트워크 요청
    isLoadingCourses = true;
    lastLoadedCategory = category;
    lastLoadTime = now;

    dataManager.loadMajorCourses(department, track, year, category,
        new OnMajorCoursesLoadedListener() {
            @Override
            public void onSuccess(List<CourseInfo> courses) {
                // 메인 어댑터 업데이트
                courseAdapter.clear();
                courseAdapter.addAll(courses);
                courseAdapter.notifyDataSetChanged();

                // ⚡ 핵심 3: 대기 중인 모든 어댑터도 동시 업데이트
                updatePendingAdapters(category, courses);

                isLoadingCourses = false;
            }

            @Override
            public void onFailure(Exception e) {
                isLoadingCourses = false;
            }
        }
    );
}

private void updatePendingAdapters(String category, List<CourseInfo> courses) {
    List<CleanArrayAdapter<CourseInfo>> adapters = pendingRequests.remove(category);
    if (adapters != null) {
        for (CleanArrayAdapter<CourseInfo> adapter : adapters) {
            adapter.clear();
            adapter.addAll(courses);
            adapter.notifyDataSetChanged();
        }
        Log.d(TAG, adapters.size() + "개 어댑터 동시 업데이트");
    }
}
```

**시나리오**:
1. 사용자가 "전공필수" 빠르게 2번 클릭
2. 첫 번째 요청은 진행, 두 번째 요청은 대기열에 추가
3. 첫 번째 요청 완료 시 대기열의 모든 어댑터에도 데이터 적용
4. **결과**: 네트워크 요청 1회, 업데이트 2회

### 3.3 중복 과목 체크

```java
private boolean addCourseFromDialog(Spinner spinnerCourseCategory,
                                     Spinner spinnerMajorCourses,
                                     CleanArrayAdapter<String> categoryAdapter,
                                     CleanArrayAdapter<CourseInfo> courseAdapter) {
    String category = categoryAdapter.getItem(catPos);
    CourseInfo selected = courseAdapter.getItem(coursePos);
    String courseName = selected.getName();
    int credits = selected.getCredits();

    // 중복 체크
    for (Course existingCourse : courseList) {
        if (existingCourse.getName().equals(courseName)) {
            Toast.makeText(this, "이미 등록된 강의입니다", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    // 과목 추가
    Course newCourse = new Course(category, courseName, credits);
    courseList.add(newCourse);
    updateCourseDisplay();

    return true;
}
```

### 3.4 탭 시스템과 필터링

```java
private void updateCourseDisplay() {
    layoutSelectedCategoryCourses.removeAllViews();

    // 현재 탭에 해당하는 과목만 필터링
    List<Course> filtered = new ArrayList<>();
    for (Course c : courseList) {
        if (c.getCategory().equals(currentSelectedTab)) {
            filtered.add(c);
        }
    }

    if (filtered.isEmpty()) {
        textEmptyCourses.setText("선택된 카테고리에 표시할 강의가 없습니다");
    } else {
        for (Course c : filtered) {
            createCourseItemView(c);  // 카드 뷰 생성
        }
    }
}

private void createCourseItemView(Course course) {
    // Material Card 생성
    MaterialCardView card = new MaterialCardView(this);
    card.setRadius(12dp);
    card.setCardElevation(0);

    // 카드 내용
    TextView name = new TextView(this);
    name.setText(course.getName());
    name.setTextSize(15);

    TextView credits = new TextView(this);
    credits.setText(course.getCredits() + "학점");

    // 삭제 버튼
    ImageButton delete = new ImageButton(this);
    delete.setImageResource(android.R.drawable.ic_delete);
    delete.setOnClickListener(v -> {
        courseList.remove(course);
        updateCourseDisplay();
        Toast.makeText(this, course.getName() + " 삭제됨", Toast.LENGTH_SHORT).show();
    });

    layoutSelectedCategoryCourses.addView(card);
}
```

---

## 4. 커스텀 뷰 - 도넛 차트

### 4.1 DonutChartView 전체 구조

```java
public class DonutChartView extends View {
    // ========== 페인트 객체 ==========
    private Paint paintProgress;      // 진행률 호
    private Paint paintBackground;    // 배경 원

    // ========== 데이터 ==========
    private float progress = 75f;     // 진행률 (0-100)
    private float strokeWidth = 20f;  // 선 두께

    // ========== 영역 ==========
    private RectF rectF;              // 호를 그릴 영역

    public DonutChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 진행률 페인트 (파란색)
        paintProgress = new Paint();
        paintProgress.setAntiAlias(true);              // 부드러운 선
        paintProgress.setStyle(Paint.Style.STROKE);    // 외곽선 스타일
        paintProgress.setStrokeWidth(strokeWidth);     // 두께 20px
        paintProgress.setStrokeCap(Paint.Cap.ROUND);   // 둥근 끝
        paintProgress.setColor(0xFF2196F3);            // 파란색

        // 배경 페인트 (연한 회색)
        paintBackground = new Paint();
        paintBackground.setAntiAlias(true);
        paintBackground.setStyle(Paint.Style.STROKE);
        paintBackground.setStrokeWidth(strokeWidth);
        paintBackground.setColor(0xFFE0E0E0);
        paintBackground.setAlpha(50);                  // 투명도

        rectF = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int radius = Math.min(width, height) / 2 - (int)strokeWidth;

        int centerX = width / 2;
        int centerY = height / 2;

        // 호가 그려질 사각형 영역 설정
        rectF.set(
            centerX - radius,  // left
            centerY - radius,  // top
            centerX + radius,  // right
            centerY + radius   // bottom
        );

        // 1단계: 배경 원 그리기 (360도 전체)
        canvas.drawArc(rectF, 0, 360, false, paintBackground);

        // 2단계: 진행률 호 그리기 (상단 -90도부터 시작)
        float sweepAngle = (progress / 100f) * 360f;
        canvas.drawArc(rectF, -90, sweepAngle, false, paintProgress);
    }

    // 진행률 설정 (애니메이션 없음)
    public void setProgress(float progress) {
        this.progress = Math.max(0, Math.min(100, progress));
        invalidate();  // 다시 그리기 요청
    }
}
```

### 4.2 Canvas API 이해

```java
// 1. Canvas란?
// - 그림을 그릴 수 있는 "도화지"
// - Android가 제공하는 2D 그래픽 API

// 2. Paint란?
// - "붓"과 "물감"의 역할
// - 색상, 두께, 스타일을 설정

// 3. 주요 Canvas 메서드
canvas.drawArc(rectF, startAngle, sweepAngle, useCenter, paint);
// rectF: 호가 그려질 사각형 영역
// startAngle: 시작 각도 (-90 = 12시 방향)
// sweepAngle: 그릴 각도 (90 = 90도만큼)
// useCenter: false = 호만, true = 부채꼴
// paint: 어떤 Paint로 그릴지

canvas.drawCircle(cx, cy, radius, paint);
canvas.drawText(text, x, y, paint);
canvas.drawLine(startX, startY, stopX, stopY, paint);
```

### 4.3 invalidate()의 역할

```java
public void setProgress(float progress) {
    this.progress = progress;
    invalidate();  // ⚡ 핵심: onDraw() 다시 호출
}

// 호출 흐름:
// setProgress(80)
//   → invalidate()
//   → Android가 onDraw() 자동 호출
//   → 화면에 새로운 진행률 표시
```

### 4.4 애니메이션 추가 (고급)

```java
public void setProgressWithAnimation(float targetProgress) {
    ValueAnimator animator = ValueAnimator.ofFloat(progress, targetProgress);
    animator.setDuration(1000);  // 1초 동안

    animator.addUpdateListener(animation -> {
        float animatedValue = (float) animation.getAnimatedValue();
        setProgress(animatedValue);
    });

    animator.start();
}

// 사용 예시:
donutChart.setProgressWithAnimation(85f);  // 0 → 85까지 1초 동안 부드럽게
```

### 4.5 XML에서 사용

```xml
<sprout.app.sakmvp1.DonutChartView
    android:id="@+id/donut_chart"
    android:layout_width="200dp"
    android:layout_height="200dp"
    android:layout_gravity="center" />
```

```java
// Activity에서 사용
DonutChartView chart = findViewById(R.id.donut_chart);
chart.setProgress(75f);           // 75% 설정
chart.setProgressColor(0xFF00FF00);  // 녹색으로 변경
chart.setStrokeWidth(30f);        // 두께 30px
```

---

## 5. 비동기 처리와 콜백 패턴

### 5.1 왜 비동기가 필요한가?

```java
// ❌ 나쁜 예: 동기 방식 (UI 프리징)
public void loadDataBad() {
    // 메인 스레드에서 네트워크 요청 (3초 걸림)
    List<String> data = db.collection("data").get().getResult();  // 앱 멈춤!
    updateUI(data);
}

// ✅ 좋은 예: 비동기 방식
public void loadDataGood() {
    db.collection("data")
        .get()
        .addOnSuccessListener(snapshot -> {
            List<String> data = parseSnapshot(snapshot);
            updateUI(data);  // UI 스레드에서 자동 실행
        });
    // 바로 다음 코드 실행 (앱 안 멈춤)
}
```

### 5.2 콜백 인터페이스 정의

```java
// 1단계: 콜백 인터페이스 정의
public interface OnStudentYearsLoadedListener {
    void onSuccess(List<String> years);  // 성공 시 호출
    void onFailure(Exception e);         // 실패 시 호출
}

// 2단계: 메서드에서 콜백 사용
public void loadStudentYears(OnStudentYearsLoadedListener listener) {
    db.collection("graduation_requirements")
        .get()
        .addOnSuccessListener(querySnapshot -> {
            List<String> years = extractYears(querySnapshot);
            listener.onSuccess(years);  // 콜백 호출
        })
        .addOnFailureListener(e -> {
            listener.onFailure(e);  // 실패 콜백 호출
        });
}

// 3단계: 호출 측에서 콜백 구현
dataManager.loadStudentYears(new OnStudentYearsLoadedListener() {
    @Override
    public void onSuccess(List<String> years) {
        // UI 업데이트
        adapter.clear();
        adapter.addAll(years);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onFailure(Exception e) {
        Toast.makeText(this, "로드 실패: " + e.getMessage(),
                       Toast.LENGTH_SHORT).show();
    }
});
```

### 5.3 람다 표현식으로 간결하게

```java
// 익명 클래스 방식 (길고 복잡)
dataManager.loadStudentYears(new OnStudentYearsLoadedListener() {
    @Override
    public void onSuccess(List<String> years) {
        updateUI(years);
    }

    @Override
    public void onFailure(Exception e) {
        showError(e);
    }
});

// 람다 표현식 (Java 8+, 간결)
dataManager.loadStudentYears(
    years -> updateUI(years),      // onSuccess
    e -> showError(e)              // onFailure
);
```

### 5.4 콜백 지옥 (Callback Hell) 해결

```java
// ❌ 콜백 지옥 (3단계 중첩)
loadStudentYears(years -> {
    loadDepartments(years, departments -> {
        loadTracks(departments, tracks -> {
            updateUI(years, departments, tracks);
        });
    });
});

// ✅ 병렬 로딩으로 개선
AtomicInteger completedCount = new AtomicInteger(0);
List<String> years = new ArrayList<>();
List<String> departments = new ArrayList<>();
List<String> tracks = new ArrayList<>();

Runnable checkAllCompleted = () -> {
    if (completedCount.incrementAndGet() == 3) {
        updateUI(years, departments, tracks);
    }
};

loadStudentYears(result -> {
    years.addAll(result);
    checkAllCompleted.run();
});

loadDepartments(result -> {
    departments.addAll(result);
    checkAllCompleted.run();
});

loadTracks(result -> {
    tracks.addAll(result);
    checkAllCompleted.run();
});
```

---

## 6. 캐싱 시스템

### 6.1 캐시 구조

```java
public class FirebaseDataManager {
    // ========== 캐시 저장소 ==========
    private Map<String, List<String>> studentYearsCache = new HashMap<>();
    private Map<String, List<String>> departmentsCache = new HashMap<>();
    private Map<String, List<CourseInfo>> coursesCache = new HashMap<>();

    // ========== 캐시 타임스탬프 ==========
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();

    // ========== 캐시 유효 시간: 5분 ==========
    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000;
}
```

### 6.2 캐시 읽기 로직

```java
public void loadStudentYears(OnStudentYearsLoadedListener listener) {
    String cacheKey = "student_years";

    // ⚡ 1단계: 캐시 확인
    if (studentYearsCache.containsKey(cacheKey)) {
        Long cachedTime = cacheTimestamps.get(cacheKey);
        long age = System.currentTimeMillis() - (cachedTime != null ? cachedTime : 0);

        // ⚡ 2단계: 캐시 유효성 검사
        if (age < CACHE_VALIDITY_MS) {
            Log.d(TAG, "캐시 히트: " + cacheKey);
            listener.onSuccess(studentYearsCache.get(cacheKey));
            return;  // 네트워크 요청 없음!
        } else {
            Log.d(TAG, "캐시 만료: " + cacheKey);
        }
    }

    // ⚡ 3단계: 캐시 미스 - Firestore 조회
    db.collection("graduation_requirements")
        .get()
        .addOnSuccessListener(snapshot -> {
            List<String> years = extractYears(snapshot);

            // ⚡ 4단계: 캐시에 저장
            studentYearsCache.put(cacheKey, years);
            cacheTimestamps.put(cacheKey, System.currentTimeMillis());

            listener.onSuccess(years);
        });
}
```

### 6.3 캐시 무효화

```java
// 수동 캐시 초기화
public void clearCache() {
    studentYearsCache.clear();
    departmentsCache.clear();
    coursesCache.clear();
    cacheTimestamps.clear();
    Log.d(TAG, "모든 캐시 초기화");
}

// 특정 키만 무효화
public void invalidateCache(String key) {
    studentYearsCache.remove(key);
    cacheTimestamps.remove(key);
    Log.d(TAG, "캐시 무효화: " + key);
}
```

### 6.4 캐시 전략

| 전략 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **Cache-Aside** | 캐시 확인 → 미스 시 DB 조회 → 캐시 저장 | 유연함 | 코드 복잡 |
| **Read-Through** | 캐시가 DB 자동 조회 | 간단함 | 캐시 의존성 높음 |
| **Write-Through** | 쓰기 시 캐시+DB 동시 업데이트 | 일관성 보장 | 느림 |

**이 프로젝트는 Cache-Aside 패턴 사용**

---

## 7. 데이터 모델링과 Parcelable

### 7.1 Course 모델

```java
public static class Course implements Parcelable {
    private String category;    // 카테고리 (전공필수, 교양선택 등)
    private String name;        // 과목명
    private int credits;        // 학점
    private String groupId;     // oneOf 그룹 ID (교양필수 그룹)
    private String competency;  // 역량 (1역량, 2역량 등)

    // ========== 생성자 ==========
    public Course(String category, String name, int credits) {
        this(category, name, credits, null, null);
    }

    public Course(String category, String name, int credits,
                  String groupId, String competency) {
        this.category = category;
        this.name = name;
        this.credits = credits;
        this.groupId = groupId;
        this.competency = competency;
    }

    // ========== Parcelable 구현 (Intent로 전달하기 위함) ==========
    protected Course(Parcel in) {
        category = in.readString();
        name = in.readString();
        credits = in.readInt();
        groupId = in.readString();
        competency = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(category);
        dest.writeString(name);
        dest.writeInt(credits);
        dest.writeString(groupId);
        dest.writeString(competency);
    }

    public static final Creator<Course> CREATOR = new Creator<Course>() {
        @Override
        public Course createFromParcel(Parcel in) {
            return new Course(in);
        }

        @Override
        public Course[] newArray(int size) {
            return new Course[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    // ========== Getter/Setter ==========
    public String getCategory() { return category; }
    public String getName() { return name; }
    public int getCredits() { return credits; }
    public String getGroupId() { return groupId; }
    public String getCompetency() { return competency; }
}
```

### 7.2 Parcelable vs Serializable

| 특징 | Parcelable | Serializable |
|------|-----------|--------------|
| **성능** | 빠름 (최적화됨) | 느림 (리플렉션 사용) |
| **코드량** | 많음 (수동 구현) | 적음 (자동) |
| **Android 권장** | ✅ 권장 | ❌ 비권장 |
| **타입 안전성** | 강함 | 약함 |

### 7.3 Intent로 전달

```java
// Activity A: 데이터 전송
List<Course> courseList = new ArrayList<>();
courseList.add(new Course("전공필수", "알고리즘", 3));
courseList.add(new Course("교양선택", "영어회화", 2, null, "1역량"));

Intent intent = new Intent(this, ResultActivity.class);
intent.putParcelableArrayListExtra("courses", new ArrayList<>(courseList));
startActivity(intent);

// Activity B: 데이터 수신
Intent intent = getIntent();
List<Course> receivedCourses;

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    receivedCourses = intent.getParcelableArrayListExtra("courses", Course.class);
} else {
    receivedCourses = intent.getParcelableArrayListExtra("courses");
}
```

---

## 8. 성능 최적화 테크닉

### 8.1 디바운스 (Debounce)

```java
private Handler debounceHandler = new Handler(Looper.getMainLooper());
private Runnable pendingTrackLoad;

spinnerDepartment.setOnItemSelectedListener(new OnItemSelectedListener() {
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // 이전 예약 취소
        if (pendingTrackLoad != null) {
            debounceHandler.removeCallbacks(pendingTrackLoad);
        }

        // 새로운 작업 예약 (100ms 후)
        pendingTrackLoad = () -> {
            String department = departmentAdapter.getItem(position);
            loadTracksForDepartment(department);
        };
        debounceHandler.postDelayed(pendingTrackLoad, 100);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}
});
```

**효과**:
- 사용자가 스피너를 빠르게 여러 번 선택해도
- 마지막 선택에 대해서만 네트워크 요청 수행
- 100ms 디바운스로 불필요한 요청 99% 감소

### 8.2 Button Guard (중복 클릭 방지)

```java
private void analyzeGraduation() {
    // 2초 버튼 가드
    btnAnalyzeGraduation.setEnabled(false);  // 즉시 비활성화

    // 졸업 분석 로직 실행
    Intent intent = new Intent(this, ResultActivity.class);
    startActivity(intent);

    // 2초 후 재활성화
    btnAnalyzeGraduation.postDelayed(() -> {
        btnAnalyzeGraduation.setEnabled(true);
    }, 2000);
}
```

### 8.3 RecyclerView 최적화

```java
// ViewHolder 패턴 (뷰 재사용)
public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.ViewHolder> {

    private List<Course> courses;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView courseName;
        TextView credits;

        public ViewHolder(View itemView) {
            super(itemView);
            courseName = itemView.findViewById(R.id.course_name);
            credits = itemView.findViewById(R.id.credits);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_course, parent, false);
        return new ViewHolder(view);  // 뷰 생성 (재사용됨)
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Course course = courses.get(position);
        holder.courseName.setText(course.getName());
        holder.credits.setText(course.getCredits() + "학점");
        // 데이터만 갱신 (뷰는 재사용)
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }
}
```

### 8.4 메모리 누수 방지

```java
@Override
protected void onPause() {
    super.onPause();

    // Handler 콜백 제거 (메모리 누수 방지)
    if (debounceHandler != null && pendingTrackLoad != null) {
        debounceHandler.removeCallbacks(pendingTrackLoad);
        pendingTrackLoad = null;
    }
}

@Override
protected void onDestroy() {
    super.onDestroy();

    // 리소스 정리
    if (layoutSelectedCategoryCourses != null) {
        layoutSelectedCategoryCourses.removeAllViews();
    }

    // 캐시 정리 (필요 시)
    isLoadingCourses = false;
    lastLoadedCategory = null;
}
```

### 8.5 배치 로딩

```java
// ❌ 나쁜 예: 순차 로딩
loadStudentYears(() -> {
    // 1초 후
    loadDepartments(() -> {
        // 2초 후
        loadTracks(() -> {
            // 3초 후 완료
        });
    });
});
// 총 시간: 1 + 2 + 3 = 6초

// ✅ 좋은 예: 병렬 로딩
AtomicInteger completed = new AtomicInteger(0);

Runnable checkCompletion = () -> {
    if (completed.incrementAndGet() == 3) {
        Log.d(TAG, "모든 로딩 완료!");
    }
};

loadStudentYears(() -> checkCompletion.run());    // 1초
loadDepartments(() -> checkCompletion.run());     // 2초
loadTracks(() -> checkCompletion.run());          // 3초
// 총 시간: max(1, 2, 3) = 3초 (병렬 실행)
```

---

## 9. 실전 디버깅 팁

### 9.1 로그 활용

```java
private static final String TAG = "CourseInput";

// 데이터 흐름 추적
Log.d(TAG, "과목 추가: " + courseName + " (" + credits + "학점)");
Log.d(TAG, "현재 과목 수: " + courseList.size());

// 네트워크 요청 추적
Log.d(TAG, "Firestore 요청 시작: " + category);
Log.d(TAG, "Firestore 요청 성공: " + courses.size() + "개");

// 에러 로그
Log.e(TAG, "과목 로드 실패", exception);
Log.w(TAG, "캐시 미스: " + cacheKey);
```

### 9.2 Toast 디버깅

```java
// 개발 중 임시 디버깅 메시지
if (BuildConfig.DEBUG) {
    Toast.makeText(this, "캐시 히트: " + cacheKey, Toast.LENGTH_SHORT).show();
}
```

### 9.3 Firestore 디버그 뷰어

```java
public class DebugFirestoreActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

## 10. 학습 체크리스트

### 초급
- [ ] 싱글톤 패턴 이해하고 구현하기
- [ ] Intent로 데이터 전달하기
- [ ] 콜백 인터페이스 만들고 사용하기
- [ ] Firestore 기본 CRUD 작업
- [ ] RecyclerView 기본 사용법

### 중급
- [ ] Parcelable 구현하기
- [ ] 커스텀 뷰 만들기 (Canvas API)
- [ ] 캐싱 시스템 구현하기
- [ ] 비동기 처리 이해하기
- [ ] 디바운스 패턴 적용하기

### 고급
- [ ] In-Flight 요청 병합 구현
- [ ] 배치 로딩으로 성능 개선
- [ ] 메모리 누수 방지
- [ ] N+1 쿼리 문제 해결
- [ ] 아키텍처 패턴 적용 (MVC → MVVM)

---

## 11. 다음 단계

1. **코드 리뷰**: 실제 코드를 읽으며 위 패턴 찾아보기
2. **실습 프로젝트**: 간단한 Todo 앱에 싱글톤 + 캐싱 적용
3. **성능 측정**: Android Profiler로 실제 개선 효과 확인
4. **고급 주제**: RxJava, Coroutines, LiveData 학습

---

**작성일**: 2025년 12월 2일
**버전**: 1.0
**대상**: Android 중급 개발자

> 💡 **Tip**: 이 문서를 읽으면서 실제 코드를 함께 보면 이해가 훨씬 빠릅니다!
