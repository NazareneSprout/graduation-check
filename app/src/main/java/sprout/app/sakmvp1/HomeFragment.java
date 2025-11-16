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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class HomeFragment extends Fragment {

    private static final boolean USE_CLEAN_HOME_LAYOUT = true;

    private static final String TAG = "HomeFragment";

    // UI 컴포넌트
    private MaterialToolbar toolbar;
    private MaterialButton btnAccessibilityMode;

    // 기능 카드 캐러셀
    private ViewPager2 featureCardViewPager;
    private FeatureCardAdapter featureCardAdapter;
    private LinearLayout featureIndicatorLayout;
    private List<ImageView> featureIndicators = new ArrayList<>();

    // 배너 캐러셀
    private ViewPager2 bannerViewPager;
    private BannerAdapter bannerAdapter;
    private LinearLayout bannerIndicatorLayout;
    private List<ImageView> bannerIndicators = new ArrayList<>();

    private LinearLayout btnFunction1;
    private LinearLayout btnFunction2;
    private LinearLayout btnFunction3;
    private LinearLayout btnFunction4;
    private LinearLayout btnFunction5;
    private LinearLayout btnFunction6;
    private LinearLayout btnFunction7;
    private LinearLayout btnFunction8;
    private LinearLayout btnAssistProgram;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int layoutId = USE_CLEAN_HOME_LAYOUT
                ? R.layout.fragment_home_clean
                : R.layout.fragment_home;
        return inflater.inflate(layoutId, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initFirebase();
        initViews(view);
        setupListeners();
        loadBanners();
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void initViews(View view) {
        // AppBar
        toolbar = view.findViewById(R.id.toolbar);
        btnAccessibilityMode = view.findViewById(R.id.btn_accessibility_mode);

        // 기능 카드 캐러셀
        featureCardViewPager = view.findViewById(R.id.feature_card_viewpager);
        featureIndicatorLayout = view.findViewById(R.id.feature_indicator_layout);
        setupFeatureCardViewPager();

        // 배너 캐러셀
        bannerViewPager = view.findViewById(R.id.banner_viewpager);
        bannerIndicatorLayout = view.findViewById(R.id.banner_indicator_layout);
        setupBannerViewPager();

        // 기능 버튼들
        btnFunction1 = view.findViewById(R.id.btnFunction1);
        btnFunction2 = view.findViewById(R.id.btnFunction2);
        btnFunction3 = view.findViewById(R.id.btnFunction3);
        btnFunction4 = view.findViewById(R.id.btnFunction4);
        btnFunction5 = view.findViewById(R.id.btnFunction5);
        btnFunction6 = view.findViewById(R.id.btnFunction6);
        btnFunction7 = view.findViewById(R.id.btnFunction7);
        btnFunction8 = view.findViewById(R.id.btnFunction8);
        btnAssistProgram = view.findViewById(R.id.btnAssistProgram);
    }

    private void setupListeners() {
        // AppBar 버튼 리스너
        if (btnAccessibilityMode != null) {
            btnAccessibilityMode.setOnClickListener(v -> showAccessibilityOptionsDialog());
        }

        // 기능 버튼 리스너
        if (btnFunction1 != null) {
            btnFunction1.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), LoadingUserInfoActivity.class);
                startActivity(intent);
            });
        }

        if (btnFunction2 != null) {
            btnFunction2.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), CertificateBoardActivity.class);
                startActivity(intent);
            });
        }

        if (btnFunction3 != null) {
            btnFunction3.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), CourseRecommendationActivity.class);
                startActivity(intent);
            });
        }

        if (btnFunction4 != null) {
            btnFunction4.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), RequiredDocumentsActivity.class);
                startActivity(intent);
            });
        }

        if (btnFunction5 != null) {
            btnFunction5.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), WebViewActivity.class);
                intent.putExtra("url", "https://www.kornu.ac.kr/user/academicCalenderList.mbs?academicIdx=3839052&id=kornukr_050100000000");
                startActivity(intent);
            });
        }

        if (btnFunction6 != null) {
            btnFunction6.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), WebViewActivity.class);
                intent.putExtra("url", "https://www.kornu.ac.kr/mbs/kornukr/jsp/board/list.jsp?boardId=21&id=kornukr_080103000000");
                startActivity(intent);
            });
        }

        if (btnFunction7 != null) {
            btnFunction7.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), WebViewActivity.class);
                intent.putExtra("url", "https://nabest.kornu.ac.kr/Career/");
                startActivity(intent);
            });
        }

        if (btnFunction8 != null) {
            btnFunction8.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MealMenuActivity.class);
                startActivity(intent);
            });
        }

        if (btnAssistProgram != null) {
            btnAssistProgram.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AssistProgramActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupFeatureCardViewPager() {
        if (featureCardViewPager == null) {
            Log.e("HomeFragment", "featureCardViewPager is null");
            return;
        }

        try {
            // 기능 카드 데이터 생성
            List<FeatureCard> featureCards = createFeatureCards();

            // 어댑터 설정
            featureCardAdapter = new FeatureCardAdapter(featureCards, this);
            featureCardViewPager.setAdapter(featureCardAdapter);
            featureCardViewPager.setOffscreenPageLimit(1);

            // 카드 간격 설정
            featureCardViewPager.setPageTransformer(new ViewPager2.PageTransformer() {
                @Override
                public void transformPage(@NonNull View page, float position) {
                    float absPos = Math.abs(position);
                    page.setScaleY(1.0f - (absPos * 0.1f));
                    page.setAlpha(1.0f - (absPos * 0.3f));
                }
            });

            // 인디케이터 설정
            setupFeatureIndicators(featureCards.size());

            // 페이지 변경 콜백
            featureCardViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    updateFeatureIndicators(position);
                }
            });
        } catch (Exception e) {
            Log.e("HomeFragment", "기능 카드 ViewPager 설정 실패: " + e.getMessage());
        }
    }

    private List<FeatureCard> createFeatureCards() {
        List<FeatureCard> cards = new ArrayList<>();

        cards.add(new FeatureCard(
                "학식메뉴",
                "오늘의 학식 메뉴를\n미리 확인하세요",
                R.drawable.image_8,
                "#FFEBEE",
                "#EF5350"
        ));

        cards.add(new FeatureCard(
                "졸업요건 분석",
                "나의 졸업 요건을 한눈에\n확인하고 관리하세요",
                R.drawable.image_1,
                "#E3F2FD",
                "#1976D2"
        ));

        cards.add(new FeatureCard(
                "학사일정",
                "중요한 학사 일정을\n놓치지 마세요",
                R.drawable.image_5,
                "#FCE4EC",
                "#E91E63"
        ));

        cards.add(new FeatureCard(
                "수강과목 추천",
                "맞춤형 수강과목을\n추천받아보세요",
                R.drawable.image_3,
                "#F3E5F5",
                "#8E24AA"
        ));

        cards.add(new FeatureCard(
                "자격증 모음",
                "취득 가능한 자격증을\n확인하고 준비하세요",
                R.drawable.image_2,
                "#FFF3E0",
                "#F57C00"
        ));

        return cards;
    }

    private void setupFeatureIndicators(int count) {
        if (featureIndicatorLayout == null) return;

        featureIndicatorLayout.removeAllViews();
        featureIndicators.clear();

        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (8 * getResources().getDisplayMetrics().density),
                    (int) (8 * getResources().getDisplayMetrics().density)
            );
            params.setMargins(
                    (int) (4 * getResources().getDisplayMetrics().density),
                    0,
                    (int) (4 * getResources().getDisplayMetrics().density),
                    0
            );
            dot.setLayoutParams(params);
            dot.setImageResource(R.drawable.indicator_dot_inactive);
            featureIndicatorLayout.addView(dot);
            featureIndicators.add(dot);
        }

        if (count > 0) {
            featureIndicators.get(0).setImageResource(R.drawable.indicator_dot_active);
        }
    }

    private void updateFeatureIndicators(int position) {
        for (int i = 0; i < featureIndicators.size(); i++) {
            if (i == position) {
                featureIndicators.get(i).setImageResource(R.drawable.indicator_dot_active);
            } else {
                featureIndicators.get(i).setImageResource(R.drawable.indicator_dot_inactive);
            }
        }
    }

    void onFeatureCardClick(int position) {
        Intent intent;
        switch (position) {
            case 0: // 학식메뉴
                intent = new Intent(requireContext(), MealMenuActivity.class);
                startActivity(intent);
                break;
            case 1: // 졸업요건 분석
                intent = new Intent(requireContext(), LoadingUserInfoActivity.class);
                startActivity(intent);
                break;
            case 2: // 학사일정
                intent = new Intent(requireContext(), WebViewActivity.class);
                intent.putExtra("url", "https://www.kornu.ac.kr/user/academicCalenderList.mbs?academicIdx=3839052&id=kornukr_050100000000");
                startActivity(intent);
                break;
            case 3: // 수강과목 추천
                intent = new Intent(requireContext(), CourseRecommendationActivity.class);
                startActivity(intent);
                break;
            case 4: // 자격증 모음
                intent = new Intent(requireContext(), CertificateBoardActivity.class);
                startActivity(intent);
                break;
        }
    }

    // =================================================================================
    // << FeatureCard 모델 >>
    // =================================================================================
    private static class FeatureCard {
        String title;
        String description;
        int iconRes;
        String backgroundColor;
        String iconColor;

        FeatureCard(String title, String description, int iconRes, String backgroundColor, String iconColor) {
            this.title = title;
            this.description = description;
            this.iconRes = iconRes;
            this.backgroundColor = backgroundColor;
            this.iconColor = iconColor;
        }
    }

    // =================================================================================
    // << FeatureCardAdapter >>
    // =================================================================================
    private static class FeatureCardAdapter extends RecyclerView.Adapter<FeatureCardAdapter.FeatureCardViewHolder> {
        private final List<FeatureCard> cards;
        private final WeakReference<HomeFragment> fragmentRef;

        FeatureCardAdapter(List<FeatureCard> cards, HomeFragment fragment) {
            this.cards = cards;
            this.fragmentRef = new WeakReference<>(fragment);
        }

        @NonNull
        @Override
        public FeatureCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.feature_card_item, parent, false);
            return new FeatureCardViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FeatureCardViewHolder holder, int position) {
            FeatureCard card = cards.get(position);
            HomeFragment fragment = fragmentRef.get();

            holder.title.setText(card.title);
            holder.description.setText(card.description);
            holder.icon.setImageResource(card.iconRes);

            // 배경색 설정
            try {
                holder.cardContainer.setBackgroundColor(android.graphics.Color.parseColor(card.backgroundColor));
            } catch (Exception e) {
                Log.e("FeatureCardAdapter", "배경색 설정 실패: " + e.getMessage());
            }

            // 아이콘 색상 설정 제거 (3D 이미지 원본 색상 유지)
            holder.icon.clearColorFilter();

            // 자세히 보기 버튼 클릭 리스너
            if (holder.btnViewDetails != null && fragment != null) {
                holder.btnViewDetails.setOnClickListener(v -> {
                    fragment.onFeatureCardClick(position);
                });
            }
        }

        @Override
        public int getItemCount() {
            return cards.size();
        }

        static class FeatureCardViewHolder extends RecyclerView.ViewHolder {
            LinearLayout cardContainer;
            ImageView icon;
            TextView title;
            TextView description;
            MaterialButton btnViewDetails;

            FeatureCardViewHolder(@NonNull View itemView) {
                super(itemView);
                cardContainer = itemView.findViewById(R.id.card_container);
                icon = itemView.findViewById(R.id.feature_icon);
                title = itemView.findViewById(R.id.feature_title);
                description = itemView.findViewById(R.id.feature_description);
                btnViewDetails = itemView.findViewById(R.id.btn_card_view_details);
            }
        }
    }

    // =================================================================================
    // << 장애학생 배려 모드 >>
    // =================================================================================

    /**
     * 접근성 옵션 선택 다이얼로그 표시
     */
    private void showAccessibilityOptionsDialog() {
        Log.d(TAG, "showAccessibilityOptionsDialog() 호출됨");

        // 다이얼로그 옵션 목록
        final String[] options = {
                "없음 (기본 모드)",
                "색약 전용 모드 (흑백)"
        };

        // 현재 저장된 설정 불러오기
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("accessibility_prefs", android.content.Context.MODE_PRIVATE);
        boolean colorBlindMode = prefs.getBoolean("color_blind_mode", false);
        final int currentSelection = colorBlindMode ? 1 : 0;

        // AlertDialog 생성
        new AlertDialog.Builder(requireContext())
                .setTitle("장애학생 배려 모드")
                .setSingleChoiceItems(options, currentSelection, null)
                .setPositiveButton("적용", (dialog, which) -> {
                    Log.d(TAG, "적용 버튼 클릭됨");

                    AlertDialog alertDialog = (AlertDialog) dialog;
                    int selectedPosition = alertDialog.getListView().getCheckedItemPosition();
                    Log.d(TAG, "선택된 위치: " + selectedPosition);

                    // 선택한 설정을 저장소에 저장
                    android.content.SharedPreferences.Editor editor = prefs.edit();
                    boolean enableColorBlindMode = (selectedPosition == 1);
                    Log.d(TAG, "색약 모드 활성화: " + enableColorBlindMode);

                    editor.putBoolean("color_blind_mode", enableColorBlindMode);
                    editor.putBoolean("option_2", false);
                    editor.putBoolean("option_3", false);
                    editor.putBoolean("option_4", false);
                    editor.putBoolean("option_5", false);
                    editor.apply();
                    Log.d(TAG, "SharedPreferences 저장 완료");

                    // 사용자에게 메시지 표시
                    String selectedMode = options[selectedPosition];
                    Toast.makeText(requireContext(),
                            "선택한 모드: " + selectedMode + "\n설정을 저장하고 앱을 재시작합니다.",
                            Toast.LENGTH_LONG).show();

                    // Firestore 저장 후 앱 재시작
                    saveAccessibilitySettingsToFirestoreAndRestart(enableColorBlindMode);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * Firestore에 접근성 설정 저장 후 앱 재시작
     */
    private void saveAccessibilitySettingsToFirestoreAndRestart(boolean enableColorBlindMode) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "접근성 설정 저장 실패: 로그인된 사용자가 없습니다 - 로컬 설정만 사용하고 재시작");
            restartApp();
            return;
        }

        String userId = user.getUid();

        db.collection("users")
                .document(userId)
                .update("color_blind_mode", enableColorBlindMode)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "접근성 설정 Firestore 저장 성공: color_blind_mode = " + enableColorBlindMode);
                    Log.d(TAG, "Firestore 저장 완료 - 앱 재시작 시작");
                    restartApp();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "접근성 설정 Firestore 저장 실패 - 로컬 설정만 사용하고 재시작", e);
                    restartApp();
                });
    }

    /**
     * 앱 재시작
     */
    private void restartApp() {
        Intent intent = new Intent(requireContext(), MainActivityNew.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
        Runtime.getRuntime().exit(0);
    }

    // =================================================================================
    // << 배너 관련 >>
    // =================================================================================

    /**
     * 배너 ViewPager 초기 설정
     */
    private void setupBannerViewPager() {
        if (bannerViewPager == null) {
            Log.e(TAG, "bannerViewPager is null");
            return;
        }

        // 어댑터 설정
        bannerAdapter = new BannerAdapter(banner -> {
            // 배너 클릭 시 BannerRouter로 라우팅
            BannerRouter.navigate(requireContext(), banner);
        });
        bannerViewPager.setAdapter(bannerAdapter);
        bannerViewPager.setOffscreenPageLimit(1);

        // 페이지 변경 콜백
        bannerViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateBannerIndicators(position);
            }
        });
    }

    /**
     * Firestore에서 배너 로드
     */
    private void loadBanners() {
        if (db == null) {
            Log.e(TAG, "Firestore instance is null");
            return;
        }

        // 현재 사용자 학부 정보 가져오기
        String userDepartment = "ALL"; // 기본값
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String dept = documentSnapshot.getString("department");
                            loadBannersForDepartment(dept != null ? dept : "ALL");
                        } else {
                            loadBannersForDepartment("ALL");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "사용자 정보 로드 실패", e);
                        loadBannersForDepartment("ALL");
                    });
        } else {
            loadBannersForDepartment("ALL");
        }
    }

    /**
     * 특정 학부에 맞는 배너 로드
     */
    private void loadBannersForDepartment(String userDepartment) {
        db.collection("banners")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Fragment가 여전히 attached 상태인지 확인
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "Fragment not attached, skipping banner setup");
                        return;
                    }

                    List<Banner> banners = new ArrayList<>();

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Banner banner = doc.toObject(Banner.class);
                        banner.setId(doc.getId());

                        // 활성화 기간 체크
                        if (!banner.isInActivePeriod()) {
                            continue;
                        }

                        // 학부별 필터링
                        if (!banner.isVisibleForDepartment(userDepartment)) {
                            continue;
                        }

                        banners.add(banner);
                    }

                    // 우선순위 순으로 정렬
                    banners.sort((b1, b2) -> Integer.compare(b1.getPriority(), b2.getPriority()));

                    // 어댑터에 설정
                    if (bannerAdapter != null) {
                        bannerAdapter.setBanners(banners);
                    }

                    // 인디케이터 설정
                    setupBannerIndicators(banners.size());

                    Log.d(TAG, "배너 로드 성공: " + banners.size() + "개");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "배너 로드 실패", e);
                });
    }

    /**
     * 배너 인디케이터 설정
     */
    private void setupBannerIndicators(int count) {
        if (bannerIndicatorLayout == null) return;

        // Fragment가 여전히 attached 상태인지 확인
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not attached, skipping indicator setup");
            return;
        }

        bannerIndicatorLayout.removeAllViews();
        bannerIndicators.clear();

        if (count == 0) {
            bannerIndicatorLayout.setVisibility(View.GONE);
            return;
        }

        bannerIndicatorLayout.setVisibility(View.VISIBLE);

        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (8 * getResources().getDisplayMetrics().density),
                    (int) (8 * getResources().getDisplayMetrics().density)
            );
            params.setMargins(
                    (int) (4 * getResources().getDisplayMetrics().density),
                    0,
                    (int) (4 * getResources().getDisplayMetrics().density),
                    0
            );
            dot.setLayoutParams(params);
            dot.setImageResource(R.drawable.indicator_dot_inactive);
            bannerIndicatorLayout.addView(dot);
            bannerIndicators.add(dot);
        }

        if (count > 0) {
            bannerIndicators.get(0).setImageResource(R.drawable.indicator_dot_active);
        }
    }

    /**
     * 배너 인디케이터 업데이트
     */
    private void updateBannerIndicators(int position) {
        for (int i = 0; i < bannerIndicators.size(); i++) {
            if (i == position) {
                bannerIndicators.get(i).setImageResource(R.drawable.indicator_dot_active);
            } else {
                bannerIndicators.get(i).setImageResource(R.drawable.indicator_dot_inactive);
            }
        }
    }

}
