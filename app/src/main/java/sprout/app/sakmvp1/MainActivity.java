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
    private Button btnFunction5;
    private Button btnFunction6;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                Toast.makeText(this, "기능3", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnFunction4 != null) {
            btnFunction4.setOnClickListener(v ->
                Toast.makeText(this, "기능4", Toast.LENGTH_SHORT).show()
            );
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

        // 하단 네비게이션 클릭 리스너
        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_button_1) {
                    // 홈 버튼 - 이미 홈 화면이므로 아무 동작 안 함
                    return true;
                } else if (itemId == R.id.nav_button_2) {
                    // '버튼2' 클릭 시 (고급 시간표 화면) 시작
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