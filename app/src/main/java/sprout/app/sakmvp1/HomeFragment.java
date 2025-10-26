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

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class HomeFragment extends Fragment {

    private static final long AUTO_SLIDE_DELAY = 8000;

    private ViewPager2 bannerViewPager;
    private BannerAdapter bannerAdapter;
    private Handler autoSlideHandler;
    private Runnable autoSlideRunnable;
    private boolean isAutoSlideStarted = false;
    private ExecutorService executorService;
    private FirebaseFirestore db;

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

        db = FirebaseFirestore.getInstance();

        initViews(view);
        setupListeners();
        loadBannersFromFirestore();
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
                bannerAdapter = new BannerAdapter();
                bannerViewPager.setAdapter(bannerAdapter);
            }
        } catch (Exception e) {
            Log.e("HomeFragment", "ViewPager 설정 실패: " + e.getMessage());
        }
    }

    private void loadBannersFromFirestore() {
        db.collection("banners")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Banner> bannerList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Banner banner = document.toObject(Banner.class);
                            bannerList.add(banner);
                        }
                        if (isAdded()) {
                            bannerAdapter.setData(bannerList);
                            startAutoSlide();
                        }
                    } else {
                        Log.w("Firestore", "Error getting documents.", task.getException());
                    }
                });
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

    private void startAutoSlide() {
        stopAutoSlide();
        if (bannerAdapter.getItemCount() > 1) {
            autoSlideHandler = new Handler(Looper.getMainLooper());
            autoSlideRunnable = new AutoSlideRunnable(this);
            autoSlideHandler.postDelayed(autoSlideRunnable, AUTO_SLIDE_DELAY);
            isAutoSlideStarted = true;
        }
    }

    private void stopAutoSlide() {
        if (autoSlideHandler != null && autoSlideRunnable != null) {
            autoSlideHandler.removeCallbacks(autoSlideRunnable);
        }
        isAutoSlideStarted = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoSlide();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bannerAdapter != null && bannerAdapter.getItemCount() > 0) {
            startAutoSlide();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopAutoSlide();
        autoSlideHandler = null;
        autoSlideRunnable = null;
        bannerViewPager = null;
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
                    if (fragment.bannerViewPager != null && fragment.bannerAdapter != null) {
                        int bannerCount = fragment.bannerAdapter.getItemCount();
                        if (bannerCount == 0) return;

                        int currentItem = fragment.bannerViewPager.getCurrentItem();
                        int nextItem = (currentItem + 1) % bannerCount;
                        fragment.bannerViewPager.setCurrentItem(nextItem, true);

                        if (fragment.autoSlideHandler != null) {
                            fragment.autoSlideHandler.postDelayed(this, AUTO_SLIDE_DELAY);
                        }
                    }
                } catch (Exception e) {
                    Log.e("HomeFragment", "자동 슬라이드 실행 오류: " + e.getMessage());
                }
            }
        }
    }

    // =================================================================================
    // << BannerAdapter 수정 >>
    // =================================================================================
    private static class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
        private final List<Banner> bannerList = new ArrayList<>();

        void setData(List<Banner> banners) {
            this.bannerList.clear();
            this.bannerList.addAll(banners);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.banner_item, parent, false);
            return new BannerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
            Banner currentBanner = bannerList.get(position);

            // 1. 이미지 로드
            if (holder.itemView.getContext() != null) {
                Glide.with(holder.itemView.getContext())
                        .load(currentBanner.getImageUrl())
                        .into(holder.bannerImage);
            }

            // 2. << 추가: 클릭 리스너 설정 >>
            holder.itemView.setOnClickListener(v -> {
                String url = currentBanner.getTargetUrl();
                // targetUrl이 비어있지 않은 경우에만 동작
                if (url != null && !url.isEmpty()) {
                    // 기존에 사용하시던 WebViewActivity를 재사용
                    Intent intent = new Intent(v.getContext(), WebViewActivity.class);
                    intent.putExtra("url", url);
                    v.getContext().startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return bannerList.size();
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