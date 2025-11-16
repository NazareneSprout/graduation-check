package sprout.app.sakmvp1;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * 이미지 전체화면 확대 Activity
 *
 * 기능:
 * - 전달받은 이미지 Bitmap을 전체화면으로 표시
 * - PhotoView를 사용하여 핀치 줌, 더블탭 줌 지원
 */
public class ImageZoomActivity extends BaseActivity {

    private static final String TAG = "ImageZoomActivity";
    public static final String EXTRA_TITLE = "title";

    // Bitmap을 임시로 저장하는 static 변수 (Activity 전환 시 사용)
    private static Bitmap tempBitmap = null;

    private MaterialToolbar toolbar;
    private com.github.chrisbanes.photoview.PhotoView photoView;
    private ProgressBar progressBar;
    private TextView tvErrorMessage;

    /**
     * Bitmap을 임시로 저장 (MealMenuActivity에서 호출)
     */
    public static void setTempBitmap(Bitmap bitmap) {
        tempBitmap = bitmap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_zoom);

        initViews();
        setupToolbar();

        // Intent로부터 제목 가져오기
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        // 툴바에 제목 설정
        if (title != null && !title.isEmpty()) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }
        }

        // Bitmap 확인
        if (tempBitmap == null || tempBitmap.isRecycled()) {
            Log.e(TAG, "이미지 Bitmap이 없습니다");
            Toast.makeText(this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Bitmap 표시
        displayImage(tempBitmap);

        // 메모리 정리를 위해 static 변수 null로 설정
        // (Activity가 종료될 때 자동으로 정리되지만, 명시적으로 정리)
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Activity 종료 시 tempBitmap 해제
        tempBitmap = null;
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        photoView = findViewById(R.id.photoView);
        progressBar = findViewById(R.id.progressBar);
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
     * Bitmap을 PhotoView에 표시합니다
     */
    private void displayImage(Bitmap bitmap) {
        Log.d(TAG, "Bitmap 표시 시작");

        try {
            photoView.setImageBitmap(bitmap);
            photoView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            tvErrorMessage.setVisibility(View.GONE);

            Log.d(TAG, "Bitmap 표시 성공");
        } catch (Exception e) {
            Log.e(TAG, "Bitmap 표시 실패", e);
            showError("이미지를 표시할 수 없습니다");
        }

        // PhotoView는 자동으로 핀치 줌, 더블탭 줌, 드래그 지원
    }


    /**
     * 에러 메시지 표시
     */
    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        photoView.setVisibility(View.GONE);
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
