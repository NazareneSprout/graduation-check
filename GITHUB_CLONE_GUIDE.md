# GitHub 프로젝트 클론(Clone) 가이드

## 📋 목차
1. [기본 클론 방법](#1-기본-클론-방법)
2. [Android Studio에서 클론하기](#2-android-studio에서-클론하기)
3. [VS Code에서 클론하기](#3-vs-code에서-클론하기)
4. [클론 후 설정](#4-클론-후-설정)
5. [문제 해결](#5-문제-해결)

---

## 1. 기본 클론 방법

### 1-1. GitHub에서 Repository URL 복사

1. **GitHub 웹사이트 접속**
   - https://github.com 로그인
   - 초대받은 프로젝트(Repository) 페이지로 이동

2. **Clone URL 복사**
   - 프로젝트 페이지 상단의 **초록색 `Code` 버튼** 클릭
   - 세 가지 옵션이 표시됨:
     - **HTTPS** (권장 - 초보자용)
     - **SSH** (권장 - SSH 키 설정 완료한 경우)
     - **GitHub CLI**

3. **URL 복사**
   ```
   HTTPS 예시: https://github.com/username/project-name.git
   SSH 예시: git@github.com:username/project-name.git
   ```

---

### 1-2. Git Bash 또는 터미널에서 클론

#### 방법 A: HTTPS로 클론 (권장 - 간단함)

```bash
# 1. 원하는 폴더로 이동
cd C:/Users/jsk00/AndroidStudioProjects

# 2. Git 클론 실행
git clone https://github.com/username/project-name.git

# 3. 클론한 폴더로 이동
cd project-name

# 4. 현재 상태 확인
git status
```

**비밀번호 입력:**
- GitHub 아이디와 비밀번호 입력 요청됨
- **주의:** 비밀번호 대신 **Personal Access Token** 사용 필요 (2021년 8월부터)

---

#### 방법 B: SSH로 클론 (빠르고 안전 - SSH 키 필요)

```bash
# 1. 원하는 폴더로 이동
cd C:/Users/jsk00/AndroidStudioProjects

# 2. SSH로 클론
git clone git@github.com:username/project-name.git

# 3. 클론한 폴더로 이동
cd project-name
```

**SSH 키 설정이 안 되어 있다면:**
- [SSH 키 생성 방법](#ssh-키-생성-방법) 참고

---

## 2. Android Studio에서 클론하기

### 2-1. Android Studio에서 직접 클론 (가장 쉬움)

1. **Android Studio 실행**

2. **Welcome 화면에서:**
   - `Get from VCS` (Version Control System) 클릭

   **또는 프로젝트 열려있는 경우:**
   - 상단 메뉴: `File` → `New` → `Project from Version Control...`

3. **Repository URL 입력**
   ```
   URL: https://github.com/username/project-name.git
   Directory: C:\Users\jsk00\AndroidStudioProjects\project-name
   ```

4. **Clone 버튼 클릭**
   - GitHub 로그인 요청 시 로그인
   - Personal Access Token 입력 (비밀번호 대신)

5. **프로젝트 자동 열림**
   - Gradle sync 자동 실행
   - 의존성 다운로드 대기

---

### 2-2. 클론 후 확인 사항

**프로젝트가 정상적으로 열렸는지 확인:**

```bash
# Android Studio Terminal에서 실행
git status
git branch
git remote -v
```

**예상 출력:**
```
origin  https://github.com/username/project-name.git (fetch)
origin  https://github.com/username/project-name.git (push)
```

---

## 3. VS Code에서 클론하기

### 3-1. VS Code에서 클론

1. **VS Code 실행**

2. **Source Control 열기**
   - 왼쪽 사이드바의 Source Control 아이콘 (또는 `Ctrl+Shift+G`)

3. **Clone Repository 클릭**
   - 또는 `Ctrl+Shift+P` → `Git: Clone` 입력

4. **Repository URL 입력**
   ```
   https://github.com/username/project-name.git
   ```

5. **저장 위치 선택**
   ```
   C:\Users\jsk00\AndroidStudioProjects
   ```

6. **Open 클릭**
   - 클론 완료 후 프로젝트 열기

---

## 4. 클론 후 설정

### 4-1. Git 사용자 정보 설정 (처음 사용 시)

```bash
# 프로젝트 폴더로 이동
cd C:/Users/jsk00/AndroidStudioProjects/project-name

# Git 사용자 이름 설정
git config user.name "Your Name"

# Git 이메일 설정 (GitHub 이메일과 동일하게)
git config user.email "your.email@example.com"

# 설정 확인
git config --list
```

---

### 4-2. 브랜치 확인 및 전환

```bash
# 현재 브랜치 확인
git branch

# 모든 브랜치 확인 (원격 포함)
git branch -a

# 특정 브랜치로 전환
git checkout branch-name

# 새 브랜치 생성 및 전환
git checkout -b new-branch-name
```

---

### 4-3. Android 프로젝트 특정 설정

#### google-services.json 파일 확인

```bash
# Firebase 프로젝트인 경우 필요
ls app/google-services.json
```

**파일이 없다면:**
- 프로젝트 관리자에게 요청
- Firebase Console에서 다운로드
- `.gitignore`에 포함되어 있을 수 있음

#### local.properties 파일 생성

```bash
# Android SDK 경로 설정
echo "sdk.dir=C:\\Users\\jsk00\\AppData\\Local\\Android\\Sdk" > local.properties
```

#### Gradle Sync

```bash
# Android Studio Terminal에서
./gradlew build

# 또는 Android Studio에서
# File → Sync Project with Gradle Files
```

---

## 5. 문제 해결

### 5-1. Personal Access Token 생성 (비밀번호 대신 사용)

**GitHub에서 Token 생성:**

1. GitHub 로그인 → 우측 상단 프로필 → `Settings`
2. 왼쪽 메뉴 맨 아래 → `Developer settings`
3. `Personal access tokens` → `Tokens (classic)` → `Generate new token`
4. **권한 선택:**
   - `repo` (전체 체크)
   - `workflow` (선택 사항)
   - `admin:org` (조직 프로젝트인 경우)
5. **Generate token** 클릭
6. **생성된 토큰 복사** (다시 볼 수 없으니 안전한 곳에 저장)

**Token 사용:**
```bash
# 클론 시 비밀번호 대신 Token 입력
git clone https://github.com/username/project-name.git
Username: your-github-username
Password: ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxx (토큰 붙여넣기)
```

---

### 5-2. SSH 키 생성 방법

**SSH 키가 없는 경우:**

```bash
# 1. SSH 키 생성
ssh-keygen -t ed25519 -C "your.email@example.com"

# 엔터 3번 (기본 위치, 비밀번호 없음)
# 생성 위치: C:/Users/jsk00/.ssh/id_ed25519

# 2. SSH 키 복사
cat ~/.ssh/id_ed25519.pub
```

**GitHub에 SSH 키 등록:**

1. GitHub → `Settings` → `SSH and GPG keys`
2. `New SSH key` 클릭
3. Title: "My Windows PC"
4. Key: 위에서 복사한 공개키 붙여넣기
5. `Add SSH key` 클릭

**SSH 연결 테스트:**
```bash
ssh -T git@github.com
# 출력: Hi username! You've successfully authenticated...
```

---

### 5-3. 권한 오류 해결

**"Permission denied" 오류:**

```bash
# 1. 원격 저장소 확인
git remote -v

# 2. HTTPS로 변경
git remote set-url origin https://github.com/username/project-name.git

# 3. 또는 SSH로 변경
git remote set-url origin git@github.com:username/project-name.git
```

---

### 5-4. "fatal: not a git repository" 오류

```bash
# 현재 위치 확인
pwd

# 프로젝트 폴더로 이동
cd C:/Users/jsk00/AndroidStudioProjects/project-name

# .git 폴더 확인
ls -la .git
```

---

### 5-5. Gradle 오류 해결

**"Could not resolve dependencies" 오류:**

```bash
# 1. Gradle 캐시 삭제
./gradlew clean

# 2. Gradle Wrapper 재생성
./gradlew wrapper --gradle-version=8.9

# 3. 의존성 다운로드
./gradlew build --refresh-dependencies
```

**"SDK location not found" 오류:**

```bash
# local.properties 파일 생성
echo "sdk.dir=C:\\Users\\jsk00\\AppData\\Local\\Android\\Sdk" > local.properties
```

---

## 6. 단계별 체크리스트

### 6-1. 클론 전 준비

- [ ] Git 설치 확인: `git --version`
- [ ] GitHub 계정 로그인
- [ ] 프로젝트 초대 수락 확인
- [ ] 저장할 폴더 경로 확인

### 6-2. 클론 실행

- [ ] Repository URL 복사
- [ ] 터미널/Git Bash에서 클론 또는 Android Studio에서 클론
- [ ] 로그인 인증 (Token 또는 SSH)
- [ ] 클론 완료 확인

### 6-3. 클론 후 설정

- [ ] Git 사용자 정보 설정
- [ ] `local.properties` 파일 생성 (Android 프로젝트)
- [ ] `google-services.json` 확인 (Firebase 프로젝트)
- [ ] Gradle Sync 실행
- [ ] 빌드 테스트: `./gradlew build`

### 6-4. Git 작업 준비

- [ ] 브랜치 확인: `git branch -a`
- [ ] 작업 브랜치 생성: `git checkout -b feature/my-work`
- [ ] `.gitignore` 확인
- [ ] 원격 저장소 확인: `git remote -v`

---

## 7. 자주 사용하는 Git 명령어

### 7-1. 기본 작업 흐름

```bash
# 1. 최신 변경사항 가져오기
git pull origin main

# 2. 새 브랜치 생성
git checkout -b feature/new-feature

# 3. 파일 수정 후 상태 확인
git status

# 4. 변경사항 스테이징
git add .

# 5. 커밋
git commit -m "feat: Add new feature"

# 6. 원격에 푸시
git push origin feature/new-feature
```

---

### 7-2. 브랜치 관리

```bash
# 모든 브랜치 보기
git branch -a

# 브랜치 전환
git checkout main

# 원격 브랜치 추적
git checkout -b local-branch origin/remote-branch

# 브랜치 삭제
git branch -d branch-name
```

---

### 7-3. 변경사항 확인

```bash
# 변경된 파일 목록
git status

# 변경 내용 상세 확인
git diff

# 커밋 히스토리
git log --oneline --graph

# 특정 파일 변경 이력
git log --follow -- filename
```

---

## 8. 빠른 참조 (Quick Reference)

### 8-1. 가장 간단한 방법 (Android Studio)

```
1. Android Studio 실행
2. Get from VCS 클릭
3. URL 입력: https://github.com/username/project-name.git
4. Directory: C:\Users\jsk00\AndroidStudioProjects\project-name
5. Clone 클릭
6. GitHub 로그인 (Token 입력)
7. 완료!
```

---

### 8-2. 가장 간단한 방법 (터미널)

```bash
cd C:/Users/jsk00/AndroidStudioProjects
git clone https://github.com/username/project-name.git
cd project-name
code .  # VS Code로 열기
```

---

### 8-3. 첫 커밋 예시

```bash
# 1. 파일 수정
# 2. 변경사항 확인
git status

# 3. 모든 변경사항 추가
git add .

# 4. 커밋
git commit -m "chore: Initial setup after cloning"

# 5. 푸시 (선택 사항)
git push origin main
```

---

## 9. 추가 리소스

### 9-1. 공식 문서
- Git 공식 문서: https://git-scm.com/doc
- GitHub 가이드: https://docs.github.com
- Android Studio VCS: https://developer.android.com/studio/intro/version-control

### 9-2. Git 설치
- Git for Windows: https://git-scm.com/download/win
- GitHub Desktop: https://desktop.github.com (GUI 도구)

### 9-3. 유용한 도구
- GitKraken: 시각적 Git 클라이언트
- SourceTree: Atlassian의 무료 Git GUI
- GitHub Desktop: GitHub 공식 데스크톱 앱

---

## 💡 팁

1. **HTTPS vs SSH:**
   - HTTPS: 간단하지만 매번 인증 필요 (Token 사용)
   - SSH: 초기 설정 복잡하지만 이후 편리함

2. **Token 저장:**
   - Windows Credential Manager에 자동 저장됨
   - 한 번 입력하면 다시 물어보지 않음

3. **Android 프로젝트:**
   - `local.properties`와 `google-services.json`은 `.gitignore`에 포함됨
   - 프로젝트 관리자에게 별도로 받아야 할 수 있음

4. **Branch 전략:**
   - `main` 브랜치는 직접 수정하지 말 것
   - 항상 새 브랜치를 만들어서 작업
   - Pull Request로 병합

---

**작성일:** 2025-11-27
**대상 OS:** Windows 11
**대상 IDE:** Android Studio, VS Code
