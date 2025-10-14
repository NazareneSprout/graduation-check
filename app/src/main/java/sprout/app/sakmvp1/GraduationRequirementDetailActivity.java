package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import sprout.app.sakmvp1.utils.GraduationRequirementUtils;

/**
 * 졸업요건 상세보기 Activity
 * Firestore에서 졸업요건 문서를 조회하여 상세 정보 및 과목 목록 표시
 */
public class GraduationRequirementDetailActivity extends AppCompatActivity {

    private static final String TAG = "GradReqDetail";
    public static final String EXTRA_DOCUMENT_ID = "document_id";
    public static final String EXTRA_YEAR = "year";
    public static final String EXTRA_DEPARTMENT = "department";
    public static final String EXTRA_TRACK = "track";

    private MaterialToolbar toolbar;
    private ProgressBar progressBar;
    private NestedScrollView contentLayout;

    // 전공 문서 정보 카드
    private MaterialCardView cardMajorDocInfo;
    private TextView tvMajorDocName, tvMajorDocUpdated;

    // 교양 문서 정보 카드
    private MaterialCardView cardGeneralDocInfo;
    private TextView tvGeneralDocName, tvGeneralDocUpdated;

    // 전공 학점
    private TextView tvMajorRequired, tvMajorElective, tvMajorTotal;
    private LinearLayout layoutMajorDeptOrAdvanced;
    private TextView tvMajorDeptOrAdvancedLabel, tvMajorDeptOrAdvanced;

    // 교양 학점
    private TextView tvGeneralRequired, tvGeneralElective, tvGeneralTotal, tvLiberalArts;

    // 기타 학점 카드 (자율선택/잔여학점만)
    private com.google.android.material.card.MaterialCardView cardOtherCredits;
    private TextView tvFreeElectiveLabel, tvFreeElective;

    private FirebaseFirestore db;
    private String documentId;
    private String year, department, track;
    private DocumentSnapshot currentDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graduation_requirement_detail);

        db = FirebaseFirestore.getInstance();

        // Intent에서 데이터 가져오기
        documentId = getIntent().getStringExtra(EXTRA_DOCUMENT_ID);
        year = getIntent().getStringExtra(EXTRA_YEAR);
        department = getIntent().getStringExtra(EXTRA_DEPARTMENT);
        track = getIntent().getStringExtra(EXTRA_TRACK);

        if (documentId == null) {
            Toast.makeText(this, "문서 ID가 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면으로 돌아올 때마다 데이터 새로고침
        loadGraduationRequirement();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progress_bar);
        contentLayout = findViewById(R.id.content_layout);

        // 전공 문서 정보 카드
        cardMajorDocInfo = findViewById(R.id.card_major_doc_info);
        tvMajorDocName = findViewById(R.id.tv_major_doc_name);
        tvMajorDocUpdated = findViewById(R.id.tv_major_doc_updated);

        // 교양 문서 정보 카드
        cardGeneralDocInfo = findViewById(R.id.card_general_doc_info);
        tvGeneralDocName = findViewById(R.id.tv_general_doc_name);
        tvGeneralDocUpdated = findViewById(R.id.tv_general_doc_updated);

        // 전공
        tvMajorRequired = findViewById(R.id.tv_major_required);
        tvMajorElective = findViewById(R.id.tv_major_elective);
        tvMajorTotal = findViewById(R.id.tv_major_total);
        layoutMajorDeptOrAdvanced = findViewById(R.id.layout_major_dept_or_advanced);
        tvMajorDeptOrAdvancedLabel = findViewById(R.id.tv_major_dept_or_advanced_label);
        tvMajorDeptOrAdvanced = findViewById(R.id.tv_major_dept_or_advanced);

        // 교양
        tvGeneralRequired = findViewById(R.id.tv_general_required);
        tvGeneralElective = findViewById(R.id.tv_general_elective);
        tvGeneralTotal = findViewById(R.id.tv_general_total);
        tvLiberalArts = findViewById(R.id.tv_liberal_arts);

        // 기타 학점 카드 (자율선택/잔여학점만)
        cardOtherCredits = findViewById(R.id.card_other_credits);
        tvFreeElectiveLabel = findViewById(R.id.tv_free_elective_label);
        tvFreeElective = findViewById(R.id.tv_free_elective);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_graduation_requirement_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_edit) {
            Intent intent = new Intent(this, GraduationRequirementEditActivity.class);
            intent.putExtra(GraduationRequirementEditActivity.EXTRA_DOCUMENT_ID, documentId);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Firestore에서 졸업요건 문서 로드
     */
    private void loadGraduationRequirement() {
        showLoading(true);

        db.collection("graduation_requirements")
                .document(documentId)
                .get()
                .addOnSuccessListener(this::displayGraduationRequirement)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "졸업요건 로드 실패", e);
                    showLoading(false);
                    Toast.makeText(this, "데이터 로드 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * 졸업요건 데이터 표시
     */
    private void displayGraduationRequirement(DocumentSnapshot document) {
        if (!document.exists()) {
            Toast.makeText(this, "문서가 존재하지 않습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentDocument = document;

        // 자율선택과 잔여학점은 현재 문서에서 가져옴
        int freeElective = GraduationRequirementUtils.getLongAsInt(document.getLong("자율선택"), 0);
        int remainingCredits = GraduationRequirementUtils.getLongAsInt(document.getLong("잔여학점"), 0);

        // 전공 학점과 교양 학점은 참조된 문서에서 로드
        loadAndDisplayCredits(freeElective, remainingCredits);

        // 전공 문서 정보 로드 (항상 표시)
        loadMajorDocumentInfo(true);

        // 교양 문서 정보 로드 (항상 표시)
        loadGeneralDocumentInfo(true);

        showLoading(false);
    }

    /**
     * 참조된 전공/교양 문서에서 학점 정보 로드하여 표시
     */
    private void loadAndDisplayCredits(int freeElective, int remainingCredits) {
        // majorDocId 확인
        String majorDocId = currentDocument.getString("majorDocId");
        String effectiveMajorDocId = (majorDocId != null && !majorDocId.trim().isEmpty())
            ? majorDocId
            : documentId; // 기본값: 현재 문서

        // generalEducationDocId 확인
        String generalDocId = currentDocument.getString("generalEducationDocId");
        String effectiveGeneralDocId;
        if (generalDocId != null && !generalDocId.trim().isEmpty()) {
            effectiveGeneralDocId = generalDocId;
        } else {
            // 기본값: "교양_공통_{year}"
            String year = GraduationRequirementUtils.getYearFromDocId(documentId);
            effectiveGeneralDocId = (year != null) ? "교양_공통_" + year : "교양_공통";
        }

        Log.d(TAG, "전공 문서에서 학점 로드: " + effectiveMajorDocId);
        Log.d(TAG, "교양 문서에서 학점 로드: " + effectiveGeneralDocId);

        // 전공 문서에서 학점 로드
        db.collection("graduation_requirements").document(effectiveMajorDocId).get()
                .addOnSuccessListener(majorDoc -> {
                    int majorRequired = 0;
                    int majorElective = 0;
                    int majorAdvanced = 0;
                    int departmentCommon = 0;

                    if (majorDoc.exists()) {
                        // 디버깅: 전공 문서의 모든 필드 출력
                        Log.d(TAG, "전공 문서 필드 전체: " + majorDoc.getData());

                        // 전공 문서에서는 "학점" 접미사가 붙은 필드명 사용 시도
                        // (예: 전공필수학점, 전공선택학점, 전공심화학점, 학부공통학점)
                        majorRequired = GraduationRequirementUtils.getLongAsInt(majorDoc.getLong("전공필수학점"), 0);
                        if (majorRequired == 0) {
                            // 백업: "학점" 접미사 없이 시도
                            majorRequired = GraduationRequirementUtils.getLongAsInt(majorDoc.getLong("전공필수"), 0);
                        }

                        majorElective = GraduationRequirementUtils.getLongAsInt(majorDoc.getLong("전공선택학점"), 0);
                        if (majorElective == 0) {
                            majorElective = GraduationRequirementUtils.getLongAsInt(majorDoc.getLong("전공선택"), 0);
                        }

                        majorAdvanced = GraduationRequirementUtils.getLongAsInt(majorDoc.getLong("전공심화학점"), 0);
                        if (majorAdvanced == 0) {
                            majorAdvanced = GraduationRequirementUtils.getLongAsInt(majorDoc.getLong("전공심화"), 0);
                        }

                        departmentCommon = GraduationRequirementUtils.getLongAsInt(majorDoc.getLong("학부공통학점"), 0);
                        if (departmentCommon == 0) {
                            departmentCommon = GraduationRequirementUtils.getLongAsInt(majorDoc.getLong("학부공통"), 0);
                        }

                        Log.d(TAG, "전공 문서 로드 성공 - 전공필수: " + majorRequired
                            + ", 전공선택: " + majorElective
                            + ", 전공심화: " + majorAdvanced
                            + ", 학부공통: " + departmentCommon);
                    } else {
                        Log.w(TAG, "전공 문서가 존재하지 않음: " + effectiveMajorDocId);
                    }

                    final int finalMajorRequired = majorRequired;
                    final int finalMajorElective = majorElective;
                    final int finalMajorAdvanced = majorAdvanced;
                    final int finalDepartmentCommon = departmentCommon;

                    // 교양 문서에서 학점 로드
                    db.collection("graduation_requirements").document(effectiveGeneralDocId).get()
                            .addOnSuccessListener(generalDoc -> {
                                int generalRequired = 0;
                                int generalElective = 0;
                                int liberalArts = 0;

                                if (generalDoc.exists()) {
                                    // 디버깅: 교양 문서의 모든 필드 출력
                                    Log.d(TAG, "교양 문서 필드 전체: " + generalDoc.getData());

                                    // 교양 문서에서는 "학점" 접미사가 붙은 필드명 사용 시도
                                    // (예: 교양필수학점, 교양선택학점, 소양학점)
                                    Long genReqWithSuffix = generalDoc.getLong("교양필수학점");
                                    Long genReqWithoutSuffix = generalDoc.getLong("교양필수");

                                    if (genReqWithSuffix != null) {
                                        generalRequired = GraduationRequirementUtils.getLongAsInt(genReqWithSuffix, 0);
                                        Log.d(TAG, "교양필수학점 필드 사용: " + generalRequired);
                                    } else if (genReqWithoutSuffix != null) {
                                        generalRequired = GraduationRequirementUtils.getLongAsInt(genReqWithoutSuffix, 0);
                                        Log.d(TAG, "교양필수 필드 사용: " + generalRequired);
                                    } else {
                                        Log.w(TAG, "교양필수 필드를 찾을 수 없음");
                                    }

                                    Long genElecWithSuffix = generalDoc.getLong("교양선택학점");
                                    Long genElecWithoutSuffix = generalDoc.getLong("교양선택");

                                    if (genElecWithSuffix != null) {
                                        generalElective = GraduationRequirementUtils.getLongAsInt(genElecWithSuffix, 0);
                                        Log.d(TAG, "교양선택학점 필드 사용: " + generalElective);
                                    } else if (genElecWithoutSuffix != null) {
                                        generalElective = GraduationRequirementUtils.getLongAsInt(genElecWithoutSuffix, 0);
                                        Log.d(TAG, "교양선택 필드 사용: " + generalElective);
                                    } else {
                                        Log.w(TAG, "교양선택 필드를 찾을 수 없음");
                                    }

                                    Long libArtsWithSuffix = generalDoc.getLong("소양학점");
                                    Long libArtsWithoutSuffix = generalDoc.getLong("소양");

                                    if (libArtsWithSuffix != null) {
                                        liberalArts = GraduationRequirementUtils.getLongAsInt(libArtsWithSuffix, 0);
                                        Log.d(TAG, "소양학점 필드 사용: " + liberalArts);
                                    } else if (libArtsWithoutSuffix != null) {
                                        liberalArts = GraduationRequirementUtils.getLongAsInt(libArtsWithoutSuffix, 0);
                                        Log.d(TAG, "소양 필드 사용: " + liberalArts);
                                    } else {
                                        Log.w(TAG, "소양 필드를 찾을 수 없음");
                                    }

                                    Log.d(TAG, "교양 문서 로드 성공 - 교양필수: " + generalRequired
                                        + ", 교양선택: " + generalElective
                                        + ", 소양: " + liberalArts);

                                    // 교양 문서가 존재하지만 필드가 모두 0인 경우 fallback 시도
                                    if (generalRequired == 0 && generalElective == 0 && liberalArts == 0) {
                                        Log.d(TAG, "교양 문서에 학점 필드가 없음. 현재 문서에서 fallback 시도");
                                        generalRequired = GraduationRequirementUtils.getLongAsInt(currentDocument.getLong("교양필수"), 0);
                                        generalElective = GraduationRequirementUtils.getLongAsInt(currentDocument.getLong("교양선택"), 0);
                                        liberalArts = GraduationRequirementUtils.getLongAsInt(currentDocument.getLong("소양"), 0);
                                        Log.d(TAG, "Fallback 결과 - 교양필수: " + generalRequired
                                            + ", 교양선택: " + generalElective
                                            + ", 소양: " + liberalArts);
                                    }
                                } else {
                                    Log.w(TAG, "교양 문서가 존재하지 않음: " + effectiveGeneralDocId);
                                    // 교양 문서가 없으면 현재 문서에서 fallback 시도
                                    Log.d(TAG, "현재 문서에서 교양 데이터 fallback 시도");
                                    generalRequired = GraduationRequirementUtils.getLongAsInt(currentDocument.getLong("교양필수"), 0);
                                    generalElective = GraduationRequirementUtils.getLongAsInt(currentDocument.getLong("교양선택"), 0);
                                    liberalArts = GraduationRequirementUtils.getLongAsInt(currentDocument.getLong("소양"), 0);
                                    Log.d(TAG, "Fallback 결과 - 교양필수: " + generalRequired
                                        + ", 교양선택: " + generalElective
                                        + ", 소양: " + liberalArts);
                                }

                                // UI 업데이트
                                displayCreditsInUI(
                                    finalMajorRequired, finalMajorElective, finalMajorAdvanced, finalDepartmentCommon,
                                    generalRequired, generalElective, liberalArts,
                                    freeElective, remainingCredits
                                );
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "교양 문서 로드 실패", e);
                                // 실패해도 전공 학점만이라도 표시
                                displayCreditsInUI(
                                    finalMajorRequired, finalMajorElective, finalMajorAdvanced, finalDepartmentCommon,
                                    0, 0, 0,
                                    freeElective, remainingCredits
                                );
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "전공 문서 로드 실패", e);
                    // 실패 시 현재 문서의 값 사용 (폴백)
                    int majorRequired = GraduationRequirementUtils.getLongAsInt(currentDocument.getLong("전공필수"), 0);
                    int majorElective = GraduationRequirementUtils.getLongAsInt(currentDocument.getLong("전공선택"), 0);
                    int majorAdvanced = GraduationRequirementUtils.getLongAsInt(currentDocument.getLong("전공심화"), 0);
                    int generalRequired = GraduationRequirementUtils.getLongAsInt(currentDocument.getLong("교양필수"), 0);
                    int generalElective = GraduationRequirementUtils.getLongAsInt(currentDocument.getLong("교양선택"), 0);
                    int liberalArts = GraduationRequirementUtils.getLongAsInt(currentDocument.getLong("소양"), 0);
                    int departmentCommon = GraduationRequirementUtils.getLongAsInt(currentDocument.getLong("학부공통"), 0);

                    displayCreditsInUI(
                        majorRequired, majorElective, majorAdvanced, departmentCommon,
                        generalRequired, generalElective, liberalArts,
                        freeElective, remainingCredits
                    );
                });
    }

    /**
     * UI에 학점 정보 표시
     */
    private void displayCreditsInUI(
            int majorRequired, int majorElective, int majorAdvanced, int departmentCommon,
            int generalRequired, int generalElective, int liberalArts,
            int freeElective, int remainingCredits) {

        // 학점 표시
        tvMajorRequired.setText(majorRequired + "학점");
        tvMajorElective.setText(majorElective + "학점");

        // 학부공통 또는 전공심화 표시 (전공 학점 카드 내)
        int deptOrAdvancedValue = 0;
        if (departmentCommon > 0) {
            layoutMajorDeptOrAdvanced.setVisibility(View.VISIBLE);
            tvMajorDeptOrAdvancedLabel.setText("학부공통");
            tvMajorDeptOrAdvanced.setText(departmentCommon + "학점");
            deptOrAdvancedValue = departmentCommon;
        } else if (majorAdvanced > 0) {
            layoutMajorDeptOrAdvanced.setVisibility(View.VISIBLE);
            tvMajorDeptOrAdvancedLabel.setText("전공심화");
            tvMajorDeptOrAdvanced.setText(majorAdvanced + "학점");
            deptOrAdvancedValue = majorAdvanced;
        } else {
            layoutMajorDeptOrAdvanced.setVisibility(View.GONE);
        }

        int majorTotal = majorRequired + majorElective + deptOrAdvancedValue;
        tvMajorTotal.setText(majorTotal + "학점");

        tvGeneralRequired.setText(generalRequired + "학점");
        tvGeneralElective.setText(generalElective + "학점");
        tvLiberalArts.setText(liberalArts + "학점");

        int generalTotal = generalRequired + generalElective + liberalArts;
        tvGeneralTotal.setText(generalTotal + "학점");

        // 기타 학점 카드 조건부 표시 (자율선택/잔여학점만)
        setupOtherCreditsCard(
            majorRequired, majorElective,
            generalRequired, generalElective, liberalArts,
            freeElective, remainingCredits,
            deptOrAdvancedValue
        );
    }

    /**
     * 기타 학점 카드 조건부 표시 설정 (자율선택/잔여학점만)
     * - 자율선택 필드가 있으면 표시
     * - 잔여학점 필드가 있으면 표시
     * - 둘 다 없으면 127 - (모든 학점 합계) 계산해서 잔여학점으로 표시
     */
    private void setupOtherCreditsCard(
            int majorRequired, int majorElective,
            int generalRequired, int generalElective, int liberalArts,
            int freeElective, int remainingCredits,
            int deptOrAdvancedValue) {

        // 자율선택 또는 잔여학점 표시
        if (freeElective > 0) {
            tvFreeElectiveLabel.setText("자율선택");
            tvFreeElective.setText(freeElective + "학점");
            cardOtherCredits.setVisibility(View.VISIBLE);
        } else if (remainingCredits > 0) {
            tvFreeElectiveLabel.setText("잔여학점");
            tvFreeElective.setText(remainingCredits + "학점");
            cardOtherCredits.setVisibility(View.VISIBLE);
        } else {
            // 둘 다 없으면 127에서 다른 학점들을 뺀 나머지 계산
            int sum = majorRequired + majorElective + generalRequired + generalElective + liberalArts + deptOrAdvancedValue;

            int calculated = 127 - sum;
            if (calculated > 0) {
                tvFreeElectiveLabel.setText("잔여학점");
                tvFreeElective.setText(calculated + "학점");
                cardOtherCredits.setVisibility(View.VISIBLE);
            } else {
                cardOtherCredits.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 과목 목록 표시
     */
    // 과목 목록 표시 기능은 제거됨 (상세 페이지에서는 학점 요약만 표시)
    /*
    private void displayCourses(DocumentSnapshot document) {
        // coursesContainer 제거로 인해 주석 처리
    }

    private void addCourseCategory(DocumentSnapshot document, String categoryName, String colorHex) {
        // coursesContainer 제거로 인해 주석 처리
    }
    */

    // 과목 아이템 관련 메서드들도 제거됨 (과목 목록 기능 제거)
    /*
    private void addCourseItem(LinearLayout parent, Map<String, Object> course) {
        // coursesContainer 제거로 인해 주석 처리
    }

    private void addCourseItemString(LinearLayout parent, String courseName) {
        // coursesContainer 제거로 인해 주석 처리
    }

    private int dpToPx(int dp) {
        // coursesContainer 제거로 인해 주석 처리
    }
    */

    /**
     * 전공 문서 정보 로드 및 표시
     * @param alwaysShow true면 문서 ID가 없어도 현재 문서 ID를 표시
     */
    private void loadMajorDocumentInfo(boolean alwaysShow) {
        if (currentDocument == null) {
            return;
        }

        // majorDocId 확인
        String customMajorDocId = currentDocument.getString("majorDocId");
        String displayDocId = (customMajorDocId != null && !customMajorDocId.trim().isEmpty())
            ? customMajorDocId
            : documentId; // 기본값으로 현재 문서 ID 사용

        Log.d(TAG, "전공 문서 정보 표시: " + displayDocId);

        db.collection("graduation_requirements").document(displayDocId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvMajorDocName.setText(displayDocId);

                        // updatedAt 타임스탬프 가져오기
                        com.google.firebase.Timestamp timestamp = doc.getTimestamp("updatedAt");
                        if (timestamp != null) {
                            // 날짜 포맷 변환 (YYYY년 MM월 DD일)
                            java.util.Date date = timestamp.toDate();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN);
                            tvMajorDocUpdated.setText("최종 수정: " + sdf.format(date));
                        } else {
                            // updatedAt이 없으면 version 필드 확인
                            String version = doc.getString("version");
                            if (version != null) {
                                tvMajorDocUpdated.setText("버전: " + version);
                            } else {
                                tvMajorDocUpdated.setText("최종 수정: 정보 없음");
                            }
                        }

                        cardMajorDocInfo.setVisibility(View.VISIBLE);
                        Log.d(TAG, "전공 문서 정보 표시 완료");
                    } else {
                        Log.w(TAG, "전공 문서를 찾을 수 없음: " + displayDocId);
                        if (alwaysShow) {
                            // 문서가 없어도 ID는 표시
                            tvMajorDocName.setText(displayDocId);
                            tvMajorDocUpdated.setText("문서 정보 없음");
                            cardMajorDocInfo.setVisibility(View.VISIBLE);
                        } else {
                            cardMajorDocInfo.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "전공 문서 정보 로드 실패", e);
                    if (alwaysShow) {
                        // 에러가 발생해도 ID는 표시
                        tvMajorDocName.setText(displayDocId);
                        tvMajorDocUpdated.setText("로드 실패");
                        cardMajorDocInfo.setVisibility(View.VISIBLE);
                    } else {
                        cardMajorDocInfo.setVisibility(View.GONE);
                    }
                });
    }

    /**
     * 교양 문서 정보 로드 및 표시
     * @param alwaysShow true면 문서 ID가 없어도 기본 교양 문서 ID를 표시
     */
    private void loadGeneralDocumentInfo(boolean alwaysShow) {
        if (currentDocument == null) {
            return;
        }

        // generalEducationDocId 확인
        String customDocId = currentDocument.getString("generalEducationDocId");
        String displayDocId;

        if (customDocId != null && !customDocId.trim().isEmpty()) {
            // 관리자가 설정한 교양 문서 사용
            displayDocId = customDocId;
        } else {
            // 기본값 생성: 문서 ID에서 연도 추출하여 "교양_공통_{year}" 생성
            String year = GraduationRequirementUtils.getYearFromDocId(documentId);
            if (year != null) {
                displayDocId = "교양_공통_" + year;
            } else {
                displayDocId = "교양_공통";
            }
        }

        Log.d(TAG, "교양 문서 정보 표시: " + displayDocId);

        db.collection("graduation_requirements").document(displayDocId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvGeneralDocName.setText(displayDocId);

                        // updatedAt 타임스탬프 가져오기
                        com.google.firebase.Timestamp timestamp = doc.getTimestamp("updatedAt");
                        if (timestamp != null) {
                            // 날짜 포맷 변환 (YYYY년 MM월 DD일)
                            java.util.Date date = timestamp.toDate();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN);
                            tvGeneralDocUpdated.setText("최종 수정: " + sdf.format(date));
                        } else {
                            // updatedAt이 없으면 version 필드 확인
                            String version = doc.getString("version");
                            if (version != null) {
                                tvGeneralDocUpdated.setText("버전: " + version);
                            } else {
                                tvGeneralDocUpdated.setText("최종 수정: 정보 없음");
                            }
                        }

                        cardGeneralDocInfo.setVisibility(View.VISIBLE);
                        Log.d(TAG, "교양 문서 정보 표시 완료");
                    } else {
                        Log.w(TAG, "교양 문서를 찾을 수 없음: " + displayDocId);
                        if (alwaysShow) {
                            // 문서가 없어도 ID는 표시
                            tvGeneralDocName.setText(displayDocId);
                            tvGeneralDocUpdated.setText("문서 정보 없음");
                            cardGeneralDocInfo.setVisibility(View.VISIBLE);
                        } else {
                            cardGeneralDocInfo.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "교양 문서 정보 로드 실패", e);
                    if (alwaysShow) {
                        // 에러가 발생해도 ID는 표시
                        tvGeneralDocName.setText(displayDocId);
                        tvGeneralDocUpdated.setText("로드 실패");
                        cardGeneralDocInfo.setVisibility(View.VISIBLE);
                    } else {
                        cardGeneralDocInfo.setVisibility(View.GONE);
                    }
                });
    }

    /**
     * 로딩 표시 제어
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        contentLayout.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
