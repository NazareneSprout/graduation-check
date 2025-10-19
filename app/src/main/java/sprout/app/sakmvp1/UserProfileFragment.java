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

        // Firestore에서 최근 졸업요건 검사 결과 조회
        db.collection("users").document(userId)
                .collection("graduation_check_history")
                .orderBy("checkedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // 저장된 결과가 없으면 과목 입력 화면으로 이동
                        Toast.makeText(requireContext(), "저장된 졸업분석 결과가 없습니다.\n새로 분석을 시작합니다.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(requireContext(), LoadingUserInfoActivity.class);
                        startActivity(intent);
                        return;
                    }

                    // 가장 최근 검사 결과 가져오기
                    com.google.firebase.firestore.DocumentSnapshot doc = querySnapshot.getDocuments().get(0);

                    // 결과 화면으로 이동
                    Intent intent = new Intent(requireContext(), GraduationAnalysisResultActivity.class);
                    intent.putExtra("fromSaved", true);
                    intent.putExtra("savedDocId", doc.getId());
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "졸업분석 결과 조회 실패", e);
                    Toast.makeText(requireContext(), "졸업분석 결과 조회에 실패했습니다", Toast.LENGTH_SHORT).show();
                });
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
