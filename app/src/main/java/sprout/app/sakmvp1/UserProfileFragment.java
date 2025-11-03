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
     * 다이얼로그에서 색약 모드 등의 접근성 옵션을 선택할 수 있습니다.
     */
    private void showAccessibilityOptionsDialog() {
        // 1단계: 다이얼로그에 표시할 옵션 목록 정의
        // final: 이 배열은 나중에 변경되지 않습니다 (상수)
        // String[]: 문자열 배열 (옵션 이름들)
        final String[] options = {
                "색약 전용 모드",  // 인덱스 0: 흑백 화면으로 전환
                "선택2",          // 인덱스 1: 나중에 추가할 기능
                "선택3",          // 인덱스 2: 나중에 추가할 기능
                "선택4",          // 인덱스 3: 나중에 추가할 기능
                "선택5"           // 인덱스 4: 나중에 추가할 기능
        };

        // 2단계: 현재 저장된 설정 불러오기
        // requireContext(): 현재 Fragment가 속한 Activity의 Context를 가져옵니다
        // getSharedPreferences(): "accessibility_prefs"라는 이름의 설정 저장소를 엽니다
        // MODE_PRIVATE: 이 앱만 접근 가능하도록 설정
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("accessibility_prefs", android.content.Context.MODE_PRIVATE);

        // 3단계: 각 옵션의 선택 상태를 저장할 배열 생성
        // boolean[5]: true/false를 저장하는 배열 (5개 옵션)
        // true = 선택됨, false = 선택 안됨
        final boolean[] selectedOptions = new boolean[5];

        // 4단계: 저장소에서 현재 설정값 읽어오기
        // getBoolean(): 저장된 true/false 값을 읽어옵니다
        // 첫 번째 파라미터: 설정 이름 (키)
        // 두 번째 파라미터: 기본값 (설정이 없을 때 사용할 값)
        selectedOptions[0] = prefs.getBoolean("color_blind_mode", false);  // 색약 모드
        selectedOptions[1] = prefs.getBoolean("option_2", false);          // 옵션 2
        selectedOptions[2] = prefs.getBoolean("option_3", false);          // 옵션 3
        selectedOptions[3] = prefs.getBoolean("option_4", false);          // 옵션 4
        selectedOptions[4] = prefs.getBoolean("option_5", false);          // 옵션 5

        // 5단계: AlertDialog(팝업 창) 생성 및 표시
        // AlertDialog.Builder: 다이얼로그를 만드는 도구
        new AlertDialog.Builder(requireContext())
                // 다이얼로그의 제목 설정
                .setTitle("장애학생 배려 모드")

                // 다중 선택 가능한 체크박스 목록 설정
                // setMultiChoiceItems: 여러 개를 동시에 선택할 수 있는 체크박스 목록
                // options: 표시할 옵션 이름들 (위에서 정의한 배열)
                // selectedOptions: 각 옵션의 현재 선택 상태 (true/false)
                // 람다식: 사용자가 체크박스를 클릭할 때 실행되는 코드
                .setMultiChoiceItems(options, selectedOptions, (dialog, which, isChecked) -> {
                    // which: 몇 번째 옵션이 클릭되었는지 (0~4)
                    // isChecked: 체크되었는지(true) 해제되었는지(false)
                    // 배열에 새로운 선택 상태를 저장합니다
                    selectedOptions[which] = isChecked;
                })

                // "적용" 버튼 설정
                // setPositiveButton: 긍정적인 행동을 하는 버튼 (확인, 적용, 예 등)
                .setPositiveButton("적용", (dialog, which) -> {
                    // === 6단계: 선택한 설정을 저장소에 저장 ===

                    // Editor: SharedPreferences에 값을 쓰기 위한 도구
                    // edit(): 설정을 수정할 준비를 합니다
                    android.content.SharedPreferences.Editor editor = prefs.edit();

                    // 각 옵션의 선택 상태를 저장소에 기록합니다
                    // putBoolean(키, 값): true/false 값을 저장합니다
                    editor.putBoolean("color_blind_mode", selectedOptions[0]);  // 색약 모드 상태 저장
                    editor.putBoolean("option_2", selectedOptions[1]);          // 옵션 2 상태 저장
                    editor.putBoolean("option_3", selectedOptions[2]);          // 옵션 3 상태 저장
                    editor.putBoolean("option_4", selectedOptions[3]);          // 옵션 4 상태 저장
                    editor.putBoolean("option_5", selectedOptions[4]);          // 옵션 5 상태 저장

                    // apply(): 변경사항을 저장소에 실제로 저장합니다
                    // (비동기: 백그라운드에서 저장되어 UI가 멈추지 않습니다)
                    editor.apply();

                    // === 7단계: 사용자에게 메시지 표시 및 앱 재시작 ===

                    // StringBuilder: 여러 문자열을 효율적으로 연결하는 도구
                    // 선택된 옵션들의 이름을 모아서 하나의 문장으로 만듭니다
                    StringBuilder selected = new StringBuilder();

                    // for문: 모든 옵션을 하나씩 확인합니다
                    for (int i = 0; i < options.length; i++) {
                        // 이 옵션이 선택되었는지 확인
                        if (selectedOptions[i]) {
                            // 이미 다른 옵션이 추가되었으면 쉼표로 구분
                            if (selected.length() > 0) selected.append(", ");
                            // 옵션 이름을 추가
                            selected.append(options[i]);
                        }
                    }

                    // 선택된 옵션이 하나라도 있는지 확인
                    if (selected.length() > 0) {
                        // 있으면: "적용되었습니다: 색약 전용 모드" 형태의 메시지 표시
                        // Toast: 화면 하단에 잠깐 나타나는 알림 메시지
                        // LENGTH_LONG: 긴 시간(약 3.5초) 동안 표시
                        Toast.makeText(requireContext(),
                                "적용되었습니다: " + selected.toString() + "\n앱을 재시작합니다.",
                                Toast.LENGTH_LONG).show();

                        // 설정이 변경되었으므로 앱을 재시작합니다
                        restartApp();
                    } else {
                        // 없으면: "모든 옵션이 해제되었습니다" 메시지 표시
                        Toast.makeText(requireContext(),
                                "모든 옵션이 해제되었습니다\n앱을 재시작합니다.",
                                Toast.LENGTH_LONG).show();
                        // 마찬가지로 앱을 재시작합니다
                        restartApp();
                    }
                })

                // "취소" 버튼 설정
                // setNegativeButton: 부정적인 행동을 하는 버튼 (취소, 아니오 등)
                // null: 버튼을 눌러도 아무것도 하지 않습니다 (그냥 다이얼로그만 닫힘)
                .setNegativeButton("취소", null)

                // 다이얼로그를 화면에 표시합니다
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

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            loadAcademicInfo(user.getUid());
        }
    }
}
