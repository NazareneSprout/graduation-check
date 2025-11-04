package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * 사용자 프로필 Fragment
 */
public class UserProfileFragment extends Fragment {

    private static final String TAG = "UserProfileFragment";

    private MaterialToolbar toolbar;
    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvStudentYear;
    private TextView tvDepartment;
    private TextView tvTrack;
    private TextView tvNoUserInfo;
    private LinearLayout btnEditUserInfo;
    private LinearLayout btnAccessibilityMode;
    private LinearLayout btnLogout;

    // 바로가기 버튼들
    private LinearLayout btnShortcutGraduationResult;
    private LinearLayout btnShortcutHistoryTimetable;
    private LinearLayout btnShortcutLinksHeader;
    private LinearLayout layoutLinksContent;
    private ImageView ivExpandIcon;
    private LinearLayout btnShortcutCalendar;
    private LinearLayout btnShortcutNotice;

    private boolean isLinksExpanded = false;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initFirebase();
        loadUserData();
        setupListeners();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if (((AppCompatActivity) requireActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvStudentYear = view.findViewById(R.id.tvStudentYear);
        tvDepartment = view.findViewById(R.id.tvDepartment);
        tvTrack = view.findViewById(R.id.tvTrack);
        tvNoUserInfo = view.findViewById(R.id.tvNoUserInfo);

        btnEditUserInfo = view.findViewById(R.id.btnEditUserInfo);
        btnAccessibilityMode = view.findViewById(R.id.btnAccessibilityMode);
        btnLogout = view.findViewById(R.id.btnLogout);

        // 바로가기 버튼들
        btnShortcutGraduationResult = view.findViewById(R.id.btnShortcutGraduationResult);
        btnShortcutHistoryTimetable = view.findViewById(R.id.btnShortcutHistoryTimetable);
        btnShortcutLinksHeader = view.findViewById(R.id.btnShortcutLinksHeader);
        layoutLinksContent = view.findViewById(R.id.layoutLinksContent);
        ivExpandIcon = view.findViewById(R.id.ivExpandIcon);
        btnShortcutCalendar = view.findViewById(R.id.btnShortcutCalendar);
        btnShortcutNotice = view.findViewById(R.id.btnShortcutNotice);
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void loadUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String displayName = user.getDisplayName();
        String email = user.getEmail();

        tvUserName.setText(displayName != null ? displayName : "사용자");
        tvUserEmail.setText(email != null ? email : "");

        loadAcademicInfo(user.getUid());
    }

    private void loadAcademicInfo(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() &&
                        documentSnapshot.contains("studentYear") &&
                        documentSnapshot.contains("department") &&
                        documentSnapshot.contains("track")) {

                        String year = documentSnapshot.getString("studentYear");
                        String department = documentSnapshot.getString("department");
                        String track = documentSnapshot.getString("track");

                        showAcademicInfo(year, department, track);
                    } else {
                        showNoAcademicInfo();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "학적 정보 로딩 실패", e);
                    showNoAcademicInfo();
                });
    }

    private void showAcademicInfo(String year, String department, String track) {
        tvNoUserInfo.setVisibility(View.GONE);
        tvStudentYear.setVisibility(View.VISIBLE);
        tvDepartment.setVisibility(View.VISIBLE);
        tvTrack.setVisibility(View.VISIBLE);

        String displayYear = year.length() >= 4 ? year.substring(2) + "학번" : year + "학번";
        tvStudentYear.setText(displayYear);
        tvDepartment.setText(department);
        tvTrack.setText(track);
    }

    private void showNoAcademicInfo() {
        tvNoUserInfo.setVisibility(View.VISIBLE);
        tvStudentYear.setVisibility(View.GONE);
        tvDepartment.setVisibility(View.GONE);
        tvTrack.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnEditUserInfo.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), UserInfoActivity.class);
            startActivity(intent);
        });

        btnAccessibilityMode.setOnClickListener(v -> {
            showAccessibilityOptionsDialog();
        });

        btnLogout.setOnClickListener(v -> {
            showLogoutDialog();
        });

        // 바로가기 버튼 리스너들

        // 졸업분석 결과 바로가기
        btnShortcutGraduationResult.setOnClickListener(v -> {
            loadAndShowGraduationResult();
        });

        // 역대 시간표 확인하기
        btnShortcutHistoryTimetable.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "역대 시간표 기능은 준비 중입니다", Toast.LENGTH_SHORT).show();
            // TODO: 역대 시간표 Activity 구현 필요
        });

        // 링크 바로가기 아코디언 토글
        btnShortcutLinksHeader.setOnClickListener(v -> {
            toggleLinksAccordion();
        });

        // 학사일정
        btnShortcutCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), WebViewActivity.class);
            intent.putExtra("url", "https://www.kornu.ac.kr/user/academicCalenderList.mbs?academicIdx=3839052&id=kornukr_050100000000");
            startActivity(intent);
        });

        // 공지사항
        btnShortcutNotice.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), WebViewActivity.class);
            intent.putExtra("url", "https://www.kornu.ac.kr/mbs/kornukr/jsp/board/list.jsp?boardId=21&id=kornukr_080103000000");
            startActivity(intent);
        });
    }

    private void toggleLinksAccordion() {
        isLinksExpanded = !isLinksExpanded;

        if (isLinksExpanded) {
            // 펼치기 (아래 화살표)
            layoutLinksContent.setVisibility(View.VISIBLE);
            ivExpandIcon.setRotation(90f);
        } else {
            // 접기 (오른쪽 화살표)
            layoutLinksContent.setVisibility(View.GONE);
            ivExpandIcon.setRotation(-90f);
        }
    }

    /**
     * 저장된 졸업분석 결과 불러와서 표시
     */
    private void loadAndShowGraduationResult() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();

        // Firestore에서 저장된 졸업요건 검사 결과 조회
        db.collection("users").document(userId)
                .collection("current_graduation_analysis")
                .document("latest")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // 저장된 결과가 없으면 과목 입력 화면으로 이동
                        Toast.makeText(requireContext(), "저장된 졸업분석 결과가 없습니다.\n새로 분석을 시작합니다.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(requireContext(), LoadingUserInfoActivity.class);
                        startActivity(intent);
                        return;
                    }

                    // 결과 화면으로 이동
                    Intent intent = new Intent(requireContext(), GraduationAnalysisResultActivity.class);
                    intent.putExtra("fromSaved", true);
                    intent.putExtra("savedDocId", "latest");
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "졸업분석 결과 조회 실패", e);
                    Toast.makeText(requireContext(), "졸업분석 결과 조회에 실패했습니다", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 접근성 옵션 선택 다이얼로그 표시
     *
     * 사용자가 "장애학생 배려 모드" 버튼을 클릭하면 이 메서드가 호출됩니다.
     * 라디오 버튼 형식으로 한 가지 모드만 선택할 수 있습니다.
     */
    private void showAccessibilityOptionsDialog() {
        Log.d(TAG, "showAccessibilityOptionsDialog() 호출됨");
        // 1단계: 다이얼로그에 표시할 옵션 목록 정의 (라디오 버튼 방식 - 하나만 선택 가능)
        final String[] options = {
                "없음 (기본 모드)",      // 인덱스 0: 모든 배려 모드 비활성화
                "색약 전용 모드 (흑백)"  // 인덱스 1: 색약을 위한 흑백 화면
        };

        // 2단계: 현재 저장된 설정 불러오기
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("accessibility_prefs", android.content.Context.MODE_PRIVATE);

        // 3단계: 현재 선택된 모드 확인
        // 색약 모드가 켜져있으면 인덱스 1, 아니면 인덱스 0
        boolean colorBlindMode = prefs.getBoolean("color_blind_mode", false);
        final int currentSelection = colorBlindMode ? 1 : 0;

        // 4단계: AlertDialog 생성 (라디오 버튼 방식)
        new AlertDialog.Builder(requireContext())
                .setTitle("장애학생 배려 모드")

                // 단일 선택 라디오 버튼 목록
                // setSingleChoiceItems: 하나만 선택 가능한 라디오 버튼 목록
                // options: 표시할 옵션 이름들
                // currentSelection: 현재 선택된 항목의 인덱스
                // null: 선택 변경 시 특별히 할 작업 없음 (적용 버튼을 누를 때 처리)
                .setSingleChoiceItems(options, currentSelection, null)

                // "적용" 버튼 설정
                .setPositiveButton("적용", (dialog, which) -> {
                    Log.d(TAG, "적용 버튼 클릭됨");

                    // 5단계: 사용자가 선택한 항목 확인
                    AlertDialog alertDialog = (AlertDialog) dialog;
                    int selectedPosition = alertDialog.getListView().getCheckedItemPosition();
                    Log.d(TAG, "선택된 위치: " + selectedPosition);

                    // 6단계: 선택한 설정을 저장소에 저장
                    android.content.SharedPreferences.Editor editor = prefs.edit();

                    // 선택된 인덱스에 따라 색약 모드 활성화/비활성화
                    boolean enableColorBlindMode = (selectedPosition == 1);
                    Log.d(TAG, "색약 모드 활성화: " + enableColorBlindMode);

                    editor.putBoolean("color_blind_mode", enableColorBlindMode);

                    // 기존의 사용하지 않는 옵션들은 모두 비활성화
                    editor.putBoolean("option_2", false);
                    editor.putBoolean("option_3", false);
                    editor.putBoolean("option_4", false);
                    editor.putBoolean("option_5", false);

                    editor.apply();
                    Log.d(TAG, "SharedPreferences 저장 완료");

                    // 7단계: Firestore에도 설정 저장 (완료 후 앱 재시작)
                    Log.d(TAG, "Firestore 저장 시작");

                    // 사용자에게 메시지 표시
                    String selectedMode = options[selectedPosition];
                    Toast.makeText(requireContext(),
                            "선택한 모드: " + selectedMode + "\n설정을 저장하고 앱을 재시작합니다.",
                            Toast.LENGTH_LONG).show();

                    // Firestore 저장 완료를 기다린 후 앱 재시작
                    saveAccessibilitySettingsToFirestoreAndRestart(enableColorBlindMode);
                })

                // "취소" 버튼 설정
                .setNegativeButton("취소", null)

                // 다이얼로그 표시
                .show();
    }

    /**
     * 앱 재시작
     *
     * 색약 모드 설정이 변경되면 화면을 새로 그려야 하므로 앱을 재시작합니다.
     * 설정 변경 → 저장 → 재시작 → 새 설정으로 화면 표시
     */
    private void restartApp() {
        // 1단계: 새로운 Intent(의도) 생성
        // Intent: 안드로이드에서 화면 전환이나 작업을 요청할 때 사용하는 객체
        // MainActivityNew.class: 앱의 메인 화면으로 이동하겠다는 의미
        Intent intent = new Intent(requireContext(), MainActivityNew.class);

        // 2단계: Intent에 플래그(옵션) 추가
        // addFlags: 이 Intent가 어떻게 동작할지 옵션을 설정합니다
        //
        // FLAG_ACTIVITY_NEW_TASK:
        // - 새로운 작업(Task)으로 Activity를 시작합니다
        // - 완전히 새로 시작한다는 의미입니다
        //
        // FLAG_ACTIVITY_CLEAR_TASK:
        // - 기존의 모든 Activity를 제거합니다
        // - 이전 화면 스택을 모두 지웁니다
        //
        // 두 플래그를 함께 사용하면:
        // → 앱을 완전히 종료하고 새로 시작하는 것과 같은 효과
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // 3단계: Intent 실행 (화면 전환)
        // startActivity: Intent에 지정된 화면으로 이동합니다
        startActivity(intent);

        // 4단계: 현재 Activity 종료
        // requireActivity(): 현재 Fragment가 속한 Activity를 가져옵니다
        // finish(): Activity를 종료합니다 (화면을 닫습니다)
        requireActivity().finish();

        // 5단계: 앱 프로세스 완전 종료
        // Runtime.getRuntime(): 현재 실행 중인 Java 런타임 환경
        // exit(0): 프로세스를 완전히 종료합니다
        // 0: 정상 종료를 의미하는 코드
        //
        // 왜 필요한가요?
        // - finish()만 하면 Activity만 닫히고 프로세스는 남아있을 수 있습니다
        // - exit(0)을 호출해야 앱이 완전히 종료되고 다시 시작됩니다
        // - 이렇게 해야 새로운 설정이 확실히 적용됩니다
        Runtime.getRuntime().exit(0);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("로그아웃")
                .setMessage("로그아웃 하시겠습니까?")
                .setPositiveButton("로그아웃", (dialog, which) -> {
                    auth.signOut();
                    Toast.makeText(requireContext(), "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(requireContext(), sprout.app.sakmvp1.Login.LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * Firestore에 접근성 설정 저장 후 앱 재시작
     *
     * 사용자가 색약 모드를 켜거나 끌 때 Firestore에 저장합니다.
     * Firestore 저장이 완료된 후에만 앱을 재시작합니다.
     * 이렇게 해야 설정이 제대로 저장되고 다시 로드될 수 있습니다.
     *
     * @param enableColorBlindMode true: 색약 모드 켜기, false: 색약 모드 끄기
     */
    private void saveAccessibilitySettingsToFirestoreAndRestart(boolean enableColorBlindMode) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "접근성 설정 저장 실패: 로그인된 사용자가 없습니다 - 로컬 설정만 사용하고 재시작");
            // 로그인하지 않은 경우 로컬 설정만 사용하고 재시작
            restartApp();
            return;
        }

        String userId = user.getUid();

        // Firestore의 users 컬렉션에 color_blind_mode 필드 저장
        db.collection("users")
                .document(userId)
                .update("color_blind_mode", enableColorBlindMode)
                .addOnSuccessListener(aVoid -> {
                    // 저장 성공: 로그 출력 후 앱 재시작
                    Log.d(TAG, "접근성 설정 Firestore 저장 성공: color_blind_mode = " + enableColorBlindMode);
                    Log.d(TAG, "Firestore 저장 완료 - 앱 재시작 시작");
                    restartApp();
                })
                .addOnFailureListener(e -> {
                    // 저장 실패: 경고 로그 출력 후에도 앱 재시작
                    // (이미 SharedPreferences에 저장되었으므로 로컬에서는 동작함)
                    Log.e(TAG, "접근성 설정 Firestore 저장 실패 - 로컬 설정만 사용하고 재시작", e);
                    restartApp();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            loadAcademicInfo(user.getUid());
        }
    }
}
