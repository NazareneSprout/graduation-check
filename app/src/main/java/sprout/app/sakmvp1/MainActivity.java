package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ImageButton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.lang.ref.WeakReference;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * 애플리케이션의 메인 허브 화면
 *
 * <p>이 Activity는 앱의 진입점이자 중앙 허브 역할을 담당하는 메인 화면입니다.
 * 사용자가 앱을 실행했을 때 가장 먼저 보게 되는 화면으로, 다양한 기능들로의
 * 접근점을 제공하고 시각적으로 매력적인 배너 슬라이드를 통해 사용자 경험을 향상시킵니다.</p>
 *
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li>🎠 <strong>자동 배너 슬라이드</strong>: 8초 간격으로 3개 배너 이미지 자동 전환</li>
 *   <li>🧭 <strong>기능 네비게이션</strong>: 졸업 분석, 데이터 뷰어 등 주요 기능 접근</li>
 *   <li>♿ <strong>접근성 지원</strong>: 고대비 모드 토글 기능</li>
 *   <li>📊 <strong>Analytics 연동</strong>: Firebase Analytics를 통한 사용자 행동 추적</li>
 *   <li>🎨 <strong>Edge-to-Edge UI</strong>: 현대적인 전면 화면 레이아웃</li>
 * </ul>
 *
 * <h3>UI 구성:</h3>
 * <ul>
 *   <li>📱 <strong>상단 배너</strong>: ViewPager2 기반 이미지 슬라이드</li>
 *   <li>🔘 <strong>메인 기능 버튼</strong>: 4개의 주요 기능 접근 버튼</li>
 *   <li>🔹 <strong>서브 기능 버튼</strong>: 2개의 추가 기능 버튼</li>
 *   <li>🧭 <strong>하단 네비게이션</strong>: BottomNavigationView를 통한 섹션 이동</li>
 * </ul>
 *
 * <h3>성능 최적화:</h3>
 * <ul>
 *   <li>⚡ <strong>백그라운드 초기화</strong>: Firebase 초기화를 별도 스레드에서 처리</li>
 *   <li>💾 <strong>메모리 관리</strong>: WeakReference를 통한 메모리 누수 방지</li>
 *   <li>🔄 <strong>라이프사이클 관리</strong>: onPause/onResume에서 자동 슬라이드 제어</li>
 *   <li>🧹 <strong>리소스 정리</strong>: ExecutorService 및 Handler 적절한 해제</li>
 * </ul>
 *
 * <h3>접근성 기능:</h3>
 * <ul>
 *   <li>🎨 <strong>고대비 테마</strong>: HighContrastHelper를 통한 접근성 향상</li>
 *   <li>📱 <strong>시스템 UI 적응</strong>: WindowInsets를 통한 적절한 패딩 처리</li>
 * </ul>
 *
 * @see GraduationAnalysisActivity 졸업 분석 기능
 * @see DataViewerActivity 데이터 뷰어 기능
 * @see HighContrastHelper 접근성 지원 도구
 */
public class MainActivity extends AppCompatActivity {

    private static final long AUTO_SLIDE_DELAY = 8000;
    private static final int BANNER_COUNT = 3;

    private FirebaseAnalytics mFirebaseAnalytics;
    private BottomNavigationView bottomNavigation;
    private ViewPager2 bannerViewPager;
    private Handler autoSlideHandler;
    private Runnable autoSlideRunnable;
    private boolean isAutoSlideStarted = false;
    private ExecutorService executorService;

    // 메인 기능 버튼들
    private LinearLayout btnFunction1;
    private LinearLayout btnFunction2;
    private LinearLayout btnFunction3;
    private LinearLayout btnFunction4;

    // 하단 작은 버튼들
    private LinearLayout btnFunction5;
    private LinearLayout btnFunction6;
    private LinearLayout btnFunction7;
    private LinearLayout btnFunction8;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 임시로 고대비 테마 비활성화 (ANR 문제 디버깅용)
        // HighContrastHelper.applyHighContrastTheme(this);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // ExecutorService 초기화
        executorService = Executors.newFixedThreadPool(2);

        // Firebase 초기화 (백그라운드)
        executorService.execute(() -> {
            try {
                FirebaseApp.initializeApp(this);
                runOnUiThread(() -> {
                    mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
                    Log.d("Firebase", "Firebase 초기화 완료");
                });
            } catch (Exception e) {
                Log.e("Firebase", "Firebase 초기화 실패: " + e.getMessage());
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // 하단 패딩을 제거하여 네비게이션 바가 시스템 네비게이션 바 위에 바로 붙도록 함
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // 뷰 초기화
        initViews();
        setupListeners();

    }

    private void initViews() {
        // 하단 네비게이션
        bottomNavigation = findViewById(R.id.bottom_navigation);
        if (bottomNavigation == null) {
            Log.e("MainActivity", "bottom_navigation not found");
            return;
        }

        // 네비게이션 바에 시스템 네비게이션 바 높이만큼 하단 패딩 추가
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> {
            Insets navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), navigationBars.bottom);
            return insets;
        });

        // 배너 ViewPager 설정
        bannerViewPager = findViewById(R.id.banner_viewpager);
        if (bannerViewPager == null) {
            Log.e("MainActivity", "banner_viewpager not found");
            return;
        }
        setupBannerViewPager();

        // 메인 기능 버튼들
        btnFunction1 = findViewById(R.id.btnFunction1);
        btnFunction2 = findViewById(R.id.btnFunction2);
        btnFunction3 = findViewById(R.id.btnFunction3);
        btnFunction4 = findViewById(R.id.btnFunction4);

        // 하단 작은 버튼들
        btnFunction5 = findViewById(R.id.btnFunction5);
        btnFunction6 = findViewById(R.id.btnFunction6);
        btnFunction7 = findViewById(R.id.btnFunction7);
        btnFunction8 = findViewById(R.id.btnFunction8);


    }

    private void setupBannerViewPager() {
        if (bannerViewPager == null) {
            Log.e("MainActivity", "bannerViewPager is null in setupBannerViewPager");
            return;
        }

        try {
            bannerViewPager.setOffscreenPageLimit(1);
            if (bannerViewPager.getAdapter() == null) {
                BannerAdapter adapter = new BannerAdapter();
                bannerViewPager.setAdapter(adapter);
            }
        } catch (Exception e) {
            Log.e("MainActivity", "ViewPager 설정 실패: " + e.getMessage());
            return;
        }

        // 자동 슬라이드 설정 (이미 시작된 경우 중복 방지)
        if (autoSlideHandler == null && !isAutoSlideStarted) {
            autoSlideHandler = new Handler(Looper.getMainLooper());
            autoSlideRunnable = new AutoSlideRunnable(this);
            autoSlideHandler.postDelayed(autoSlideRunnable, AUTO_SLIDE_DELAY);
            isAutoSlideStarted = true;
        }
    }


    private void setupListeners() {
        // 메인 기능 버튼 클릭 리스너
        if (btnFunction1 != null) {
            btnFunction1.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, GraduationAnalysisActivity.class);
                startActivity(intent);
            });
        }

        if (btnFunction2 != null) {
            btnFunction2.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, DataViewerActivity.class);
                startActivity(intent);
            });
        }

        if (btnFunction3 != null) {
            btnFunction3.setOnClickListener(v ->
                Toast.makeText(this, "기능3 - 준비중", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnFunction4 != null) {
            btnFunction4.setOnClickListener(v -> {
                HighContrastHelper.toggleHighContrast(this);
                boolean isEnabled = HighContrastHelper.isHighContrastEnabled(this);
                String message = isEnabled ? "고대비 모드 활성화됨" : "고대비 모드 비활성화됨";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                recreate();
            });
        }

        // 하단 작은 버튼 클릭 리스너 웹사이트로 넘어가도록 변경
        if (btnFunction5 != null) {
            btnFunction5.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
                intent.putExtra("url", "https://www.kornu.ac.kr/user/academicCalenderList.mbs?academicIdx=3839052&id=kornukr_050100000000"); // 학사일정 URL
                startActivity(intent);
            });
        }

        if (btnFunction6 != null) {
            btnFunction6.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
                intent.putExtra("url", "https://www.kornu.ac.kr/mbs/kornukr/jsp/board/list.jsp?boardId=21&id=kornukr_080103000000"); // 학사공지 URL
                startActivity(intent);
            });
        }

        if (btnFunction7 != null) {
            btnFunction7.setOnClickListener(v ->
                Toast.makeText(this, "기능5 - 준비중", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnFunction8 != null) {
            btnFunction8.setOnClickListener(v ->
                Toast.makeText(this, "기능6 - 준비중", Toast.LENGTH_SHORT).show()
            );
        }

        // 하단 네비게이션 클릭 리스너
        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_button_1) {
                    // 홈 버튼 - 이미 홈 화면이므로 아숭실대학교 무 동작 안 함
                    return true;
                } else if (itemId == R.id.nav_button_2) {
                    // 시간표 화면으로 이동
                    Intent intent = new Intent(this, TimeTableActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_button_3) {
                    Toast.makeText(this, "버튼3", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_button_4) {
                    Toast.makeText(this, "버튼4", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoSlideHandler != null && autoSlideRunnable != null) {
            autoSlideHandler.removeCallbacks(autoSlideRunnable);
            autoSlideHandler = null;
            autoSlideRunnable = null;
            isAutoSlideStarted = false;
        }
        bannerViewPager = null;

        // ExecutorService 정리
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (autoSlideHandler != null && autoSlideRunnable != null) {
            autoSlideHandler.removeCallbacks(autoSlideRunnable);
            isAutoSlideStarted = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (autoSlideHandler != null && autoSlideRunnable != null && !isAutoSlideStarted) {
            autoSlideHandler.postDelayed(autoSlideRunnable, AUTO_SLIDE_DELAY);
            isAutoSlideStarted = true;
        }

        // 홈 화면으로 돌아올 때 네비게이션 상태 초기화
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_button_1);
        }
    }

    // 메모리 누수 방지를 위한 WeakReference 사용 Runnable
    private static class AutoSlideRunnable implements Runnable {
        private final WeakReference<MainActivity> activityRef;

        AutoSlideRunnable(MainActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            MainActivity activity = activityRef.get();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed() && activity.isAutoSlideStarted) {
                try {
                    if (activity.bannerViewPager != null) {
                        int currentItem = activity.bannerViewPager.getCurrentItem();
                        int nextItem = (currentItem + 1) % BANNER_COUNT;
                        activity.bannerViewPager.setCurrentItem(nextItem, true);
                        if (activity.autoSlideHandler != null && activity.isAutoSlideStarted) {
                            activity.autoSlideHandler.postDelayed(this, AUTO_SLIDE_DELAY);
                        }
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "자동 슬라이드 실행 오류: " + e.getMessage());
                }
            }
        }
    }

    // 배너 어댑터 클래스
    private static class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
        private final int[] bannerImages = {
                R.drawable.banner_image_1,
                R.drawable.banner_image_2,
                R.drawable.banner_image_3
        };

        @NonNull
        @Override
        public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.banner_item, parent, false);
            return new BannerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
            holder.bannerImage.setImageResource(bannerImages[position]);
        }

        @Override
        public int getItemCount() {
            return bannerImages.length;
        }

        static class BannerViewHolder extends RecyclerView.ViewHolder {
            ImageView bannerImage;

            BannerViewHolder(@NonNull View itemView) {
                super(itemView);
                bannerImage = itemView.findViewById(R.id.banner_image);
            }
        }
    }
}