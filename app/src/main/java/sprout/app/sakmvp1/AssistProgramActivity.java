package sprout.app.sakmvp1;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;

public class AssistProgramActivity extends AppCompatActivity {

    private LinearLayout accordionHeader;
    private LinearLayout accordionContent;
    private ImageView expandIcon;
    private Button btnShareUrl;
    private Button btnDownloadMobile;
    private boolean isExpanded = false;
    private static final String DOWNLOAD_URL = "https://firebasestorage.googleapis.com/v0/b/nazarenesprout.firebasestorage.app/o/assist_programs%2FScreenPenTool.exe?alt=media&token=946b59fe-e75e-40b3-90b9-8bbcf6ad818d";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assist_program);

        // 상태바 높이 설정
        View statusBarSpace = findViewById(R.id.status_bar_space);
        int statusBarHeight = getStatusBarHeight();
        ViewGroup.LayoutParams params = statusBarSpace.getLayoutParams();
        params.height = statusBarHeight;
        statusBarSpace.setLayoutParams(params);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("어시스트 프로그램");
        }

        // 아코디언 뷰 초기화
        accordionHeader = findViewById(R.id.accordion_header);
        accordionContent = findViewById(R.id.accordion_content);
        expandIcon = findViewById(R.id.expand_icon);
        btnShareUrl = findViewById(R.id.btn_share_url);
        btnDownloadMobile = findViewById(R.id.btn_download_mobile);

        // 아코디언 헤더 클릭 리스너
        accordionHeader.setOnClickListener(v -> toggleAccordion());

        // URL 공유 버튼 클릭 리스너
        btnShareUrl.setOnClickListener(v -> shareUrl());

        // 모바일 다운로드 버튼 클릭 리스너
        btnDownloadMobile.setOnClickListener(v -> downloadScreenPenTool());
    }

    /**
     * 스크린 펜 툴 다운로드 (모바일에서 직접)
     */
    private void downloadScreenPenTool() {
        // 다운로드 URL 열기
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(DOWNLOAD_URL));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "다운로드 링크를 열 수 없습니다: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * URL 공유하기 (카카오톡, 메일 등)
     */
    private void shareUrl() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "스크린 펜 툴 다운로드");
        shareIntent.putExtra(Intent.EXTRA_TEXT, DOWNLOAD_URL);

        try {
            startActivity(Intent.createChooser(shareIntent, "다운로드 링크 공유"));
        } catch (Exception e) {
            Toast.makeText(this, "공유할 수 없습니다: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 아코디언 토글
     */
    private void toggleAccordion() {
        isExpanded = !isExpanded;
        accordionContent.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        expandIcon.setRotation(isExpanded ? 270 : 90);
    }

    /**
     * 상태바 높이 가져오기
     */
    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
