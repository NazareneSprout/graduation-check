package sprout.app.sakmvp1;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

/**
 * 수강과목 추천 결과 화면
 *
 * 사용자가 선택한 옵션에 따라 추천된 과목 목록을 표시합니다.
 */
public class RecommendationResultActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView tvRecommendationOptions;
    private RecyclerView recyclerViewRecommendations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation_result);

        initViews();
        loadData();
    }

    private void initViews() {
        // Toolbar 설정
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Views
        tvRecommendationOptions = findViewById(R.id.tvRecommendationOptions);
        recyclerViewRecommendations = findViewById(R.id.recyclerViewRecommendations);

        // RecyclerView 설정
        recyclerViewRecommendations.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadData() {
        // Intent에서 추천 옵션 정보 가져오기
        boolean considerTimetable = getIntent().getBooleanExtra("considerTimetable", false);
        int difficultyLevel = getIntent().getIntExtra("difficultyLevel", 2);

        // 추천 옵션 표시
        String timetableText = considerTimetable ? "고려함" : "안함";
        String difficultyText = difficultyLevel == 1 ? "쉬움" : difficultyLevel == 2 ? "보통" : "어려움";
        tvRecommendationOptions.setText("시간표 고려: " + timetableText + "\n학기 난이도: " + difficultyText);

        // TODO: Firebase에서 추천 과목 데이터 가져오기
        // TODO: RecyclerView Adapter 설정
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
