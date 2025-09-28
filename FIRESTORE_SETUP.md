# Firestore 인덱스 설정 가이드

## 복합 인덱스 배포

프로젝트 루트에 `firestore.indexes.json` 파일을 생성했습니다.

### 1. Firebase CLI로 인덱스 배포

```bash
# Firebase 프로젝트 디렉토리에서 실행
firebase deploy --only firestore:indexes
```

### 2. Firebase 콘솔에서 수동 생성

Firebase Console → Firestore Database → 색인 탭에서:

**컬렉션**: `student_progress`
**필드**:
- `userId` (오름차순)
- `lastUpdated` (내림차순)

### 3. 인덱스 생성 이유

`FirebaseDataManager.java:1613`에서 사용하는 쿼리:
```java
.orderBy("lastUpdated", Query.Direction.DESCENDING)
```

사용자별 최신 진행상황 조회 시 성능 향상을 위해 복합 인덱스가 필요합니다.

### 4. 배포 후 확인

- Firebase Console에서 인덱스 상태가 "단일 필드" → "빌드 중" → "사용 설정됨"으로 변경되는지 확인
- 일반적으로 5-10분 소요됩니다.