package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

/**
 * 학적 정보 로딩 전용 화면
 *
 * 저장된 학적 정보를 확인하고 졸업 요건을 검증한 후
 * 적절한 화면으로 이동합니다.
 */
public class LoadingUserInfoActivity extends AppCompatActivity {

    private static final String TAG = "LoadingUserInfo";

    private TextView tvLoadingTitle;
    private TextView tvLoadingDescription;
    private ProgressBar progressBar;
    private LinearLayout layoutUserInfo;
    private TextView tvUserInfoDetails;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseDataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_user_info);

        initViews();
        initFirebase();

        // 약간의 지연 후 정보 로딩 시작 (UI 표시를 위해)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkAndLoadUserInfo();
        }, 500);
    }

    private void initViews() {
        tvLoadingTitle = findViewById(R.id.tvLoadingTitle);
        tvLoadingDescription = findViewById(R.id.tvLoadingDescription);
        progressBar = findViewById(R.id.progressBar);
        layoutUserInfo = findViewById(R.id.layoutUserInfo);
        tvUserInfoDetails = findViewById(R.id.tvUserInfoDetails);
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        dataManager = FirebaseDataManager.getInstance();
    }

    private void checkAndLoadUserInfo() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Log.w(TAG, "사용자가 로그인하지 않음");
            showUserInfoRequiredDialog();
            return;
        }

        tvLoadingTitle.setText("학적 정보 확인 중");
        tvLoadingDescription.setText("저장된 학적 정보를 불러오고 있습니다...");

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() &&
                        documentSnapshot.contains("studentYear") &&
                        documentSnapshot.contains("department") &&
                        documentSnapshot.contains("track")) {

                        String year = documentSnapshot.getString("studentYear");
                        String department = documentSnapshot.getString("department");
                        String track = documentSnapshot.getString("track");

                        Log.d(TAG, "저장된 정보 발견: " + year + "/" + department + "/" + track);

                        // 학적 정보 표시
                        showUserInfo(year, department, track);

                        // 졸업 요건 확인
                        checkGraduationRequirements(year, department, track);
                    } else {
                        // 저장된 정보가 없음
                        Log.d(TAG, "저장된 사용자 정보 없음");
                        showUserInfoRequiredDialog();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "사용자 정보 확인 실패", e);
                    Toast.makeText(this, "정보 확인 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    navigateToManualSelection();
                });
    }

    private void showUserInfo(String year, String department, String track) {
        // 학적 정보 표시
        String shortYear = year.length() >= 4 ? year.substring(2) : year;
        String infoText = shortYear + "학번 · " + department + " · " + track;

        tvUserInfoDetails.setText(infoText);
        layoutUserInfo.setVisibility(View.VISIBLE);
    }

    private void checkGraduationRequirements(String year, String department, String track) {
        tvLoadingTitle.setText("졸업 요건 확인 중");
        tvLoadingDescription.setText("해당 학적 정보의 졸업 요건을 확인하고 있습니다...");

        dataManager.loadGraduationRequirements(department, track, year,
            new FirebaseDataManager.OnGraduationRequirementsLoadedListener() {
                @Override
                public void onSuccess(Map<String, Object> requirements) {
                    Log.d(TAG, "졸업 요건 확인 완료 - 추가 요건 화면으로 이동");

                    // 성공 메시지 잠깐 보여주고 이동
                    tvLoadingTitle.setText("확인 완료!");
                    tvLoadingDescription.setText("졸업 요건 분석을 시작합니다");

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        navigateToAdditionalRequirements(year, department, track);
                    }, 800);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.w(TAG, "졸업 요건 확인 실패", e);

                    // 졸업 요건이 없는 경우
                    new Handler(Looper.getMainLooper()).post(() -> {
                        new AlertDialog.Builder(LoadingUserInfoActivity.this)
                                .setTitle("졸업 요건 없음")
                                .setMessage("저장된 학적 정보(" + tvUserInfoDetails.getText() + ")의 졸업 요건을 찾을 수 없습니다.\n\n" +
                                        "학번/학부/트랙을 직접 선택하시겠습니까?")
                                .setPositiveButton("직접 선택", (dialog, which) -> {
                                    navigateToManualSelection();
                                })
                                .setNegativeButton("취소", (dialog, which) -> {
                                    finish();
                                })
                                .setCancelable(false)
                                .show();
                    });
                }
            }
        );
    }

    private void navigateToAdditionalRequirements(String year, String department, String track) {
        Intent intent = new Intent(this, AdditionalRequirementsActivity.class);
        intent.putExtra("year", year);
        intent.putExtra("department", department);
        intent.putExtra("track", track);
        startActivity(intent);
        finish();
    }

    private void navigateToManualSelection() {
        Intent intent = new Intent(this, GraduationAnalysisActivity.class);
        intent.putExtra("skipAutoLoad", true); // 자동 로딩 건너뛰기 플래그
        startActivity(intent);
        finish();
    }

    private void showUserInfoRequiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle("학적 정보 입력 필요")
                .setMessage("졸업 요건 분석을 위해서는 학적 정보(학번, 학부, 트랙)가 필요합니다.\n\n" +
                        "'내 학적정보' 메뉴에서 정보를 입력해주세요.")
                .setPositiveButton("정보 입력하기", (dialog, which) -> {
                    Intent intent = new Intent(LoadingUserInfoActivity.this, UserInfoActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("직접 입력", (dialog, which) -> {
                    navigateToManualSelection();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onBackPressed() {
        // 뒤로가기 버튼 비활성화 (로딩 중에는 뒤로 가기 방지)
        super.onBackPressed();
        finish();
    }
}
