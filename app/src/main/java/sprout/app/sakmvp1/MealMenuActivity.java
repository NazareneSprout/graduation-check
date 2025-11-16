package sprout.app.sakmvp1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 나사렛대학교 구내식당 식단표 조회 Activity
 *
 * 기능:
 * - 나사렛대학교 홈페이지에서 최신 식단표 정보를 스크래핑
 * - 식단표 이미지를 화면에 표시
 */
public class MealMenuActivity extends BaseActivity {

    private static final String TAG = "MealMenuActivity";
    private static final String MEAL_MENU_URL = "https://www.kornu.ac.kr/mbs/kornukr/jsp/board/list.jsp?boardId=29&id=kornukr_081300000000";
    private static final String BASE_URL = "https://www.kornu.ac.kr";

    private MaterialToolbar toolbar;
    private ProgressBar progressBar;
    private com.github.chrisbanes.photoview.PhotoView imgMealMenu;
    private TextView tvMealTitle;
    private TextView tvMealDate;
    private TextView tvErrorMessage;

    private ExecutorService executorService;
    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_menu);

        initViews();
        setupToolbar();

        executorService = Executors.newSingleThreadExecutor();
        httpClient = new OkHttpClient();

        // 식단 정보 가져오기
        loadMealMenu();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        imgMealMenu = findViewById(R.id.imgMealMenu);
        tvMealTitle = findViewById(R.id.tvMealTitle);
        tvMealDate = findViewById(R.id.tvMealDate);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * 식단 메뉴 정보를 로드합니다
     */
    private void loadMealMenu() {
        showLoading(true);

        executorService.execute(() -> {
            try {
                // 1. 목록 페이지에서 최신 게시물 링크 가져오기
                String latestPostUrl = fetchLatestPostUrl();

                if (latestPostUrl == null) {
                    runOnUiThread(() -> {
                        showError("최신 식단 정보를 찾을 수 없습니다.");
                    });
                    return;
                }

                // 2. 상세 페이지에서 이미지 URL 추출
                MealMenuInfo menuInfo = fetchMealMenuInfo(latestPostUrl);

                if (menuInfo == null || menuInfo.imageUrl == null) {
                    runOnUiThread(() -> {
                        showError("식단 이미지를 찾을 수 없습니다.");
                    });
                    return;
                }

                // 3. UI 업데이트
                runOnUiThread(() -> {
                    displayMealMenu(menuInfo);
                });

            } catch (Exception e) {
                Log.e(TAG, "식단 정보 로드 실패", e);
                runOnUiThread(() -> {
                    showError("네트워크 오류가 발생했습니다: " + e.getMessage());
                });
            }
        });
    }

    /**
     * 최신 게시물의 URL을 가져옵니다
     */
    private String fetchLatestPostUrl() throws IOException {
        Request request = new Request.Builder()
                .url(MEAL_MENU_URL)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "HTTP 요청 실패: " + response.code());
                return null;
            }

            String html = response.body().string();
            Document doc = Jsoup.parse(html);

            // 게시판 목록에서 식단표 게시물 링크 찾기 ("월"과 "주차"를 포함하는 제목)
            Elements links = doc.select("#tableList a[href*=view.jsp]");

            if (links.isEmpty()) {
                Log.e(TAG, "게시물 링크를 찾을 수 없습니다");
                return null;
            }

            // 식단표 게시물 찾기 (제목에 "월"과 "주차" 포함)
            String relativeUrl = null;
            for (Element link : links) {
                String title = link.text();
                if (title.contains("월") && title.contains("주차")) {
                    relativeUrl = link.attr("href");
                    Log.d(TAG, "식단표 게시물 발견: " + title);
                    break;
                }
            }

            if (relativeUrl == null) {
                Log.e(TAG, "식단표 게시물을 찾을 수 없습니다");
                return null;
            }

            // 상대 경로를 절대 경로로 변환
            if (relativeUrl.startsWith("/")) {
                return BASE_URL + relativeUrl;
            } else if (!relativeUrl.startsWith("http")) {
                // 상대 경로 처리
                String basePath = MEAL_MENU_URL.substring(0, MEAL_MENU_URL.lastIndexOf("/") + 1);
                return basePath + relativeUrl;
            }

            return relativeUrl;
        }
    }

    /**
     * 식단 정보를 가져옵니다
     */
    private MealMenuInfo fetchMealMenuInfo(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "HTTP 요청 실패: " + response.code());
                return null;
            }

            String html = response.body().string();
            Document doc = Jsoup.parse(html);

            MealMenuInfo info = new MealMenuInfo();

            // 제목 추출
            Element titleElement = doc.selectFirst("h3.view-title, .board-view-title");
            if (titleElement != null) {
                info.title = titleElement.text();
            }

            // 작성일 추출
            Element dateElement = doc.selectFirst(".view-date, .board-view-date");
            if (dateElement != null) {
                info.date = dateElement.text();
            }

            // 이미지 URL 추출 (이벤트 배너 제외)
            Elements imgElements = doc.select(".view-content img, .board-content img, img[src*=upload]");

            // 이벤트 배너가 아닌 실제 학식 메뉴 이미지 찾기
            String imgSrc = null;
            for (Element img : imgElements) {
                String src = img.attr("src");
                // /upload/event/ 경로의 이미지는 이벤트 배너이므로 제외
                if (!src.contains("/upload/event/")) {
                    imgSrc = src;
                    Log.d(TAG, "학식 메뉴 이미지 발견: " + imgSrc);
                    break;
                }
            }

            if (imgSrc != null) {
                // 상대 경로를 절대 경로로 변환
                if (imgSrc.startsWith("/")) {
                    info.imageUrl = BASE_URL + imgSrc;
                } else if (!imgSrc.startsWith("http")) {
                    info.imageUrl = BASE_URL + "/" + imgSrc;
                } else {
                    info.imageUrl = imgSrc;
                }
            }

            Log.d(TAG, "식단 정보 - 제목: " + info.title);
            Log.d(TAG, "식단 정보 - 날짜: " + info.date);
            Log.d(TAG, "식단 정보 - 이미지 URL: " + info.imageUrl);
            return info;
        }
    }

    /**
     * 식단 메뉴를 화면에 표시합니다
     */
    private void displayMealMenu(MealMenuInfo info) {
        showLoading(false);

        if (info.title != null) {
            tvMealTitle.setText(info.title);
            tvMealTitle.setVisibility(View.VISIBLE);
        }

        if (info.date != null) {
            tvMealDate.setText(info.date);
            tvMealDate.setVisibility(View.VISIBLE);
        }

        // Glide로 이미지 로드
        Log.d(TAG, "Glide로 이미지 로드 시작: " + info.imageUrl);
        Glide.with(this)
                .load(info.imageUrl)
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "이미지 로드 실패: " + model, e);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        Log.d(TAG, "이미지 로드 성공: " + model);
                        return false;
                    }
                })
                .into(imgMealMenu);

        // PhotoView이므로 자동으로 핀치 줌/더블탭 줌 지원

        imgMealMenu.setVisibility(View.VISIBLE);
        tvErrorMessage.setVisibility(View.GONE);

        // 이미지 클릭 시 전체화면 확대 Activity 실행
        imgMealMenu.setOnClickListener(v -> {
            // Glide에서 현재 이미지의 Bitmap을 가져와서 전달
            try {
                android.graphics.drawable.Drawable drawable = imgMealMenu.getDrawable();
                if (drawable != null && drawable instanceof android.graphics.drawable.BitmapDrawable) {
                    android.graphics.Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();

                    // Bitmap을 임시로 저장
                    ImageZoomActivity.setTempBitmap(bitmap);

                    android.content.Intent intent = new android.content.Intent(MealMenuActivity.this, ImageZoomActivity.class);
                    intent.putExtra(ImageZoomActivity.EXTRA_TITLE, info.title != null ? info.title : "식단 이미지");
                    startActivity(intent);
                } else {
                    Toast.makeText(MealMenuActivity.this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "이미지 전달 실패", e);
                Toast.makeText(MealMenuActivity.this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 로딩 상태 표시
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        imgMealMenu.setVisibility(show ? View.GONE : View.VISIBLE);
        tvMealTitle.setVisibility(show ? View.GONE : View.VISIBLE);
        tvMealDate.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * 에러 메시지 표시
     */
    private void showError(String message) {
        showLoading(false);
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
        imgMealMenu.setVisibility(View.GONE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * 식단 메뉴 정보를 담는 클래스
     */
    private static class MealMenuInfo {
        String title;
        String date;
        String imageUrl;
    }
}
