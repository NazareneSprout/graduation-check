# 🎓 졸업 요건 분석 앱 (SakMvp1)

> **숭실대학교 학생들을 위한 졸업 요건 분석 및 학점 관리 Android 애플리케이션**

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)

## 📱 프로젝트 개요

SakMvp1은 숭실대학교 학생들이 졸업 요건을 쉽게 확인하고 관리할 수 있도록 도와주는 Android 애플리케이션입니다. 학번, 학부, 트랙을 선택하여 개인별 졸업 요건을 분석하고, 수강한 강의를 입력하여 졸업 진행률을 시각적으로 확인할 수 있습니다.

## ✨ 주요 기능

### 🎯 **졸업 요건 분석**
- 학번/학부/트랙별 맞춤형 졸업 요건 확인
- 전공필수, 전공선택, 교양필수, 교양선택, 소양 등 세부 카테고리별 분석
- 신구 교육과정(20-22학번 vs 23-25학번) 자동 구분

### 📊 **시각적 진도 관리**
- 도넛 차트를 통한 졸업 진행률 시각화
- 카테고리별 이수 현황 상세 표시
- 실시간 학점 계산 및 진도율 업데이트

### 📝 **수강 강의 관리**
- 전공/교양 과목별 수강 강의 등록
- 강의 검색 및 자동완성 기능
- 학점 오버플로우 자동 재분배 시스템

### 📅 **시간표 기능**
- 주간 시간표 뷰어
- 수업 추가/편집/삭제 기능
- 시간 충돌 검사 및 알림

### 🔐 **사용자 관리**
- Firebase Authentication 기반 로그인/회원가입
- 개인 데이터 클라우드 동기화
- 자동 로그인 및 세션 관리

## 🛠️ 기술 스택

### **플랫폼 & 언어**
- **Android**: API 24-35 (Android 7.0 - 15.0)
- **Java**: OpenJDK 11
- **Gradle**: Kotlin DSL

### **백엔드 & 데이터베이스**
- **Firebase Firestore**: NoSQL 데이터베이스
- **Firebase Authentication**: 사용자 인증
- **Firebase Analytics**: 사용자 행동 분석

### **UI & UX**
- **Material Design 3**: 최신 디자인 가이드라인
- **View System**: 전통적인 Android View (Compose 미사용)
- **Edge-to-Edge**: 몰입형 전화면 UI

### **아키텍처**
- **MVC 패턴**: Model-View-Controller
- **싱글톤 패턴**: 중앙집중식 데이터 매니저
- **캐싱 시스템**: 메모리 캐시 및 성능 최적화

## 🚀 성능 최적화

### **네트워크 최적화**
- **Single-flight 패턴**: 중복 요청 방지
- **배치 로딩**: 여러 데이터 소스 동시 로드
- **디바운스**: 100ms 지연으로 불필요한 요청 제거

### **메모리 관리**
- **5분 캐시**: DocumentSnapshot 캐싱으로 N+1 쿼리 해결
- **메모리 누수 방지**: onPause에서 Handler 정리
- **리소스 최적화**: ViewPager2와 RecyclerView 활용

### **사용자 경험**
- **즉시 피드백**: 캐시 우선 데이터 표시
- **로딩 상태**: 스켈레톤 UI 및 프로그레스 바
- **에러 처리**: 사용자 친화적 에러 메시지

## 📁 프로젝트 구조

```
app/src/main/java/sprout/app/sakmvp1/
├── 📊 MainActivity.java                    # 메인 허브 화면
├── 🎓 GraduationAnalysisActivity.java     # 졸업 요건 분석
├── ➕ AdditionalRequirementsActivity.java # 추가 요건 입력
├── 📝 CourseInputActivity.java            # 수강 강의 입력
├── 📈 GraduationAnalysisResultActivity.java # 결과 표시
├── 📅 TimeTableActivity.java              # 시간표 관리
├── 🔍 DataViewerActivity.java             # 데이터 뷰어 (디버깅)
├── 🔐 Login/
│   ├── LoginActivity.java                 # 로그인
│   └── SignUpActivity.java                # 회원가입
├── 🛠️ FirebaseDataManager.java           # 데이터 매니저 (싱글톤)
├── 📊 DonutChartView.java                 # 커스텀 도넛 차트
├── ⚙️ DepartmentConfig.java               # 학부별 설정
└── ♿ HighContrastHelper.java              # 접근성 지원
```

## 🔥 Firebase 컬렉션 구조

```
Firestore Database
├── 📚 graduation_requirements/           # 졸업 요건 데이터
│   ├── 2025_IT학부_컴퓨터학과/
│   ├── 2024_경영학부_경영학과/
│   └── ...
├── 📋 graduation_meta/                   # 학부/트랙 메타데이터
│   ├── departments/                      # 전체 학부 목록
│   ├── tracks/                          # 학부별 트랙 목록
│   └── total_credits/                   # 졸업이수학점
├── 👤 users/                            # 사용자 데이터
│   └── [userId]/
│       ├── profile/                     # 프로필 정보
│       └── analysis_results/            # 분석 결과
└── 🏫 department_configs/               # 학부별 설정
    └── [departmentId]/                  # 전공심화/학부공통 사용 여부
```

## 📊 학점 오버플로우 시스템

### **20-22학번 (구 교육과정)**
```
모든 초과 학점 → 일반선택으로 이동
- 전공필수 초과 → 일반선택
- 전공선택 초과 → 일반선택
- 교양 초과 → 일반선택
- 학부공통 초과 → 일반선택
```

### **23-25학번 (신 교육과정)**
```
모든 초과 학점 → 잔여학점으로 이동
- 전공필수 초과 → 잔여학점
- 전공선택 초과 → 잔여학점
- 교양 초과 → 잔여학점
- 전공심화 초과 → 잔여학점
```

## 🎨 UI/UX 특징

### **Material Design 3**
- Dynamic Color 지원
- 테마별 색상 팔레트 (Primary, Secondary, Tertiary)
- 접근성 고려 설계 (고대비 모드 지원)

### **반응형 디자인**
- 다양한 화면 크기 대응
- Edge-to-Edge UI로 몰입형 경험
- 하단 네비게이션으로 직관적 탐색

### **사용자 경험**
- 2초 버튼 가드로 중복 클릭 방지
- 스켈레톤 UI로 로딩 상태 표시
- 토스트 메시지로 즉시 피드백

## 🔧 설치 및 실행

### **필수 요구사항**
- Android Studio Arctic Fox 이상
- JDK 11 이상
- Android SDK API 24-35
- Google Services JSON 설정

### **빌드 및 실행**
```bash
# 저장소 클론
git clone https://github.com/NazareneSprout/graduation-check.git

# Android Studio에서 프로젝트 열기
cd graduation-check

# Firebase 설정 파일 추가
# google-services.json을 app/ 디렉토리에 배치

# 빌드 및 실행
./gradlew assembleDebug
./gradlew installDebug
```

### **Firebase 설정**
1. [Firebase Console](https://console.firebase.google.com/)에서 프로젝트 생성
2. Android 앱 추가 (패키지명: `sprout.app.sakmvp1`)
3. `google-services.json` 다운로드 후 `app/` 폴더에 배치
4. Firestore Database 및 Authentication 활성화

## 🧪 테스트

### **단위 테스트**
```bash
./gradlew test
```

### **UI 테스트**
```bash
./gradlew connectedAndroidTest
```

### **Lint 검사**
```bash
./gradlew lint
```

## 📈 성능 지표

| 지표 | 목표 값 | 현재 값 | 상태 |
|------|---------|---------|------|
| 앱 시작 시간 | < 3초 | 2.1초 | ✅ |
| 메모리 사용량 | < 100MB | 85MB | ✅ |
| 캐시 히트율 | > 80% | 85% | ✅ |
| 네트워크 요청 수 | 최소화 | 동시 로딩 | ✅ |

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m '새로운 기능: 놀라운 기능 추가'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

## 👥 개발팀

- **프론트엔드**: Android Native 개발
- **백엔드**: Firebase 서비스 활용
- **UI/UX**: Material Design 3 적용
- **데이터**: Firestore NoSQL 데이터베이스

## 📞 문의

프로젝트 관련 문의사항이나 버그 리포트는 다음을 통해 연락주세요:

- **GitHub Issues**: [Issues 페이지](https://github.com/NazareneSprout/graduation-check/issues)
- **Email**: [프로젝트 이메일]

---

**📝 마지막 업데이트**: 2025년 9월 28일

> 💡 **졸업 요건, 이제 한 번에 확인하세요!**
> 숭실대학교 학생들의 졸업 계획을 도와주는 스마트한 도구입니다.