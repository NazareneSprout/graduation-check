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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 홈 화면 Fragment
 */
public class HomeFragment extends Fragment {

    private static final long AUTO_SLIDE_DELAY = 8000;
    private static final int BANNER_COUNT = 3;

    private FirebaseAnalytics mFirebaseAnalytics;
    private ViewPager2 bannerViewPager;
    private Handler autoSlideHandler;
    private Runnable autoSlideRunnable;
    private boolean isAutoSlideStarted = false;
    private ExecutorService executorService;

    private LinearLayout btnFunction1;
    private LinearLayout btnFunction2;
    private LinearLayout btnFunction3;
    private LinearLayout btnFunction4;
    private LinearLayout btnFunction5;
    private LinearLayout btnFunction6;
    private LinearLayout btnFunction7;
    private LinearLayout btnFunction8;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        executorService = Executors.newFixedThreadPool(2);

        // Firebase 초기화 (Context와 Activity를 미리 저장)
        android.content.Context context = requireContext();
        androidx.fragment.app.FragmentActivity activity = requireActivity();
        executorService.execute(() -> {
            try {
                FirebaseApp.initializeApp(context);
                if (isAdded() && activity != null) {
                    activity.runOnUiThread(() -> {
                        if (isAdded()) {
                            mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
                            Log.d("Firebase", "Firebase 초기화 완료");
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("Firebase", "Firebase 초기화 실패: " + e.getMessage());
            }
        });

        initViews(view);
        setupListeners();
    }

    private void initViews(View view) {
        bannerViewPager = view.findViewById(R.id.banner_viewpager);
        setupBannerViewPager();

        btnFunction1 = view.findViewById(R.id.btnFunction1);
        btnFunction2 = view.findViewById(R.id.btnFunction2);
        btnFunction3 = view.findViewById(R.id.btnFunction3);
        btnFunction4 = view.findViewById(R.id.btnFunction4);
        btnFunction5 = view.findViewById(R.id.btnFunction5);
        btnFunction6 = view.findViewById(R.id.btnFunction6);
        btnFunction7 = view.findViewById(R.id.btnFunction7);
        btnFunction8 = view.findViewById(R.id.btnFunction8);
    }

    private void setupBannerViewPager() {
        if (bannerViewPager == null) {
            Log.e("HomeFragment", "bannerViewPager is null");
            return;
        }

        try {
            bannerViewPager.setOffscreenPageLimit(1);
            if (bannerViewPager.getAdapter() == null) {
                BannerAdapter adapter = new BannerAdapter();
                bannerViewPager.setAdapter(adapter);
            }
        } catch (Exception e) {
            Log.e("HomeFragment", "ViewPager 설정 실패: " + e.getMessage());
            return;
        }

        if (autoSlideHandler == null && !isAutoSlideStarted) {
            autoSlideHandler = new Handler(Looper.getMainLooper());
            autoSlideRunnable = new AutoSlideRunnable(this);
            autoSlideHandler.postDelayed(autoSlideRunnable, AUTO_SLIDE_DELAY);
            isAutoSlideStarted = true;
        }
    }

    private void setupListeners() {
        if (btnFunction1 != null) {
            btnFunction1.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), LoadingUserInfoActivity.class);
                startActivity(intent);
            });
        }

        if (btnFunction2 != null) {
            btnFunction2.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), UserInfoActivity.class);
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
            btnFunction4.setOnClickListener(v ->
                Toast.makeText(requireContext(), "기능4 - 준비중", Toast.LENGTH_SHORT).show()
            );
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
            btnFunction7.setOnClickListener(v ->
                Toast.makeText(requireContext(), "기능5 - 준비중", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnFunction8 != null) {
            btnFunction8.setOnClickListener(v ->
                Toast.makeText(requireContext(), "기능6 - 준비중", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void stopAutoSlide() {
        if (autoSlideHandler != null && autoSlideRunnable != null) {
            autoSlideHandler.removeCallbacks(autoSlideRunnable);
            isAutoSlideStarted = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoSlide();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (autoSlideHandler != null && autoSlideRunnable != null && !isAutoSlideStarted) {
            autoSlideHandler.postDelayed(autoSlideRunnable, AUTO_SLIDE_DELAY);
            isAutoSlideStarted = true;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        stopAutoSlide();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopAutoSlide();
        autoSlideHandler = null;
        autoSlideRunnable = null;
        bannerViewPager = null;

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

    private static class AutoSlideRunnable implements Runnable {
        private final WeakReference<HomeFragment> fragmentRef;

        AutoSlideRunnable(HomeFragment fragment) {
            this.fragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public void run() {
            HomeFragment fragment = fragmentRef.get();
            if (fragment != null && fragment.isAdded() && fragment.isAutoSlideStarted) {
                try {
                    if (fragment.bannerViewPager != null) {
                        int currentItem = fragment.bannerViewPager.getCurrentItem();
                        int nextItem = (currentItem + 1) % BANNER_COUNT;
                        fragment.bannerViewPager.setCurrentItem(nextItem, true);
                        if (fragment.autoSlideHandler != null && fragment.isAutoSlideStarted) {
                            fragment.autoSlideHandler.postDelayed(this, AUTO_SLIDE_DELAY);
                        }
                    }
                } catch (Exception e) {
                    Log.e("HomeFragment", "자동 슬라이드 실행 오류: " + e.getMessage());
                }
            }
        }
    }

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
