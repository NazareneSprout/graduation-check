// Firestore 데이터 확인 스크립트
const admin = require('firebase-admin');

// Firebase Admin 초기화
const serviceAccount = require('./functions/serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkEducationDocuments() {
  console.log('=== 교양 문서 확인 ===\n');

  try {
    const snapshot = await db.collection('graduation_requirements').get();

    const educationDocs = [];
    snapshot.forEach(doc => {
      if (doc.id.startsWith('교양_')) {
        educationDocs.push({
          id: doc.id,
          data: doc.data()
        });
      }
    });

    console.log(`총 ${educationDocs.length}개의 교양 문서 발견\n`);

    educationDocs.forEach((doc, index) => {
      console.log(`[${index + 1}] 문서 ID: ${doc.id}`);
      console.log('학점 정보:');
      console.log(`  - 교양필수: ${doc.data.교양필수 || 0}학점`);
      console.log(`  - 교양선택: ${doc.data.교양선택 || 0}학점`);
      console.log(`  - 소양: ${doc.data.소양 || 0}학점`);

      if (doc.data.rules && doc.data.rules.requirements) {
        console.log(`  - 과목 요구사항: ${doc.data.rules.requirements.length}개`);
        doc.data.rules.requirements.slice(0, 3).forEach((req, i) => {
          if (req.type === 'single') {
            console.log(`    ${i + 1}. [단일] ${req.name} (${req.credit}학점)`);
          } else if (req.type === 'oneOf') {
            console.log(`    ${i + 1}. [선택] ${req.min}개 이상 (${req.credit}학점) - ${req.options.length}개 옵션`);
          }
        });
        if (doc.data.rules.requirements.length > 3) {
          console.log(`    ... 외 ${doc.data.rules.requirements.length - 3}개`);
        }
      }
      console.log('');
    });

    // 첫 번째 교양 문서의 전체 데이터 출력 (JSON)
    if (educationDocs.length > 0) {
      console.log('=== 첫 번째 문서 전체 데이터 ===');
      console.log(JSON.stringify(educationDocs[0], null, 2));
    }

  } catch (error) {
    console.error('데이터 조회 실패:', error);
  }

  process.exit(0);
}

checkEducationDocuments();
