package sprout.app.sakmvp1;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CertificateBoardActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private EditText editSearch;
    private ChipGroup chipGroupDepartment;
    private RecyclerView recyclerViewCertificates;

    private CertificateAdapter adapter;
    private List<Certificate> allCertificatesList; // Firestore에서 가져온 원본 리스트
    private List<Certificate> filteredList;      // 필터링된 리스트 (화면에 표시될 리스트)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificate_board);

        // 1. 뷰 초기화
        toolbar = findViewById(R.id.toolbar);
        editSearch = findViewById(R.id.edit_search);
        chipGroupDepartment = findViewById(R.id.chip_group_department);
        recyclerViewCertificates = findViewById(R.id.recycler_view_certificates);

        // 2. 툴바 설정
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("자격증 게시판");
        // 뒤로가기 버튼 (필요한 경우)
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // 3. RecyclerView 설정
        setupRecyclerView();

        // 4. 리스너 설정 (검색, 칩 필터)
        setupListeners();

        // 5. 데이터 로드 (지금은 임시 데이터, 나중에 Firestore에서 가져와야 함)
        loadDummyData();
    }

    private void setupRecyclerView() {
        allCertificatesList = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new CertificateAdapter(filteredList);
        recyclerViewCertificates.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCertificates.setAdapter(adapter);
    }

    private void setupListeners() {
        // 칩 그룹 리스너 (학부 필터)
        chipGroupDepartment.setOnCheckedChangeListener((group, checkedId) -> {
            applyFilters();
        });

        // 검색창 리스너
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * 검색어와 칩 필터를 모두 적용하여 리스트를 갱신합니다.
     */
    private void applyFilters() {
        // 1. 검색어 가져오기 (소문자로)
        String searchQuery = editSearch.getText().toString().toLowerCase().trim();

        // 2. 선택된 칩의 텍스트 가져오기
        String selectedChipText = "전체"; // 기본값
        int checkedChipId = chipGroupDepartment.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            Chip selectedChip = findViewById(checkedChipId);
            selectedChipText = selectedChip.getText().toString();
        }

        final String finalSelectedChipText = selectedChipText;

        // 3. 원본 리스트(allCertificatesList)에서 필터링
        // (Java 8 스트림 사용)
        filteredList = allCertificatesList.stream()
                .filter(certificate -> {
                    // 필터 1: 학부 필터
                    boolean departmentMatch = finalSelectedChipText.equals("전체") ||
                            certificate.getDepartment().equals(finalSelectedChipText);

                    // 필터 2: 검색어 필터 (제목 또는 주최 기관에 포함되는지)
                    boolean searchMatch = searchQuery.isEmpty() ||
                            certificate.getTitle().toLowerCase().contains(searchQuery) ||
                            certificate.getIssuer().toLowerCase().contains(searchQuery);

                    return departmentMatch && searchMatch; // 두 조건 모두 만족
                })
                .collect(Collectors.toList());

        // 4. 어댑터에 갱신된 리스트 적용
        adapter.updateData(filteredList);

        // (선택사항) 검색 결과가 없을 때 처리
        if (filteredList.isEmpty()) {
            // Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
            // 또는 "결과 없음"을 표시할 TextView를 보여줄 수 있습니다.
        }
    }


    /**
     * 임시 데이터를 로드합니다. (테스트용)
     * TODO: 이 메서드를 Firestore에서 데이터를 가져오는 코드로 교체해야 합니다.
     */
    private void loadDummyData() {
        allCertificatesList.clear(); // 원본 리스트 비우기

        // "IT학부" 관련 자격증
        allCertificatesList.add(new Certificate("정보처리기사", "한국산업인력공단", "D-30", 2098, "IT학부"));
        allCertificatesList.add(new Certificate("리눅스마스터 1급", "KAIT", "접수중", 1502, "IT학부"));
        allCertificatesList.add(new Certificate("AWS Certified Cloud Practitioner", "Amazon", "상시", 3012, "IT학부"));

        // "경찰행정학부" 관련 자격증
        allCertificatesList.add(new Certificate("경비지도사", "한국산업인력공단", "D-120", 1800, "경찰행정학부"));
        allCertificatesList.add(new Certificate("신변보호사", "대한경호협회", "상시", 950, "경찰행정학부"));

        // "기독교학부" 관련 자격증
        allCertificatesList.add(new Certificate("청소년상담사 3급", "한국산업인력공단", "D-45", 1100, "기독교학부"));

        // (다른 학부 데이터...)

        // 처음에는 모든 데이터를 보여줌 (필터링 적용)
        applyFilters();
    }

    // (툴바 뒤로가기 버튼 처리 - 필요한 경우)
    // @Override
    // public boolean onSupportNavigateUp() {
    //     onBackPressed();
    //     return true;
    // }
}
