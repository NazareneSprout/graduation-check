package sprout.app.sakmvp1.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import sprout.app.sakmvp1.GraduationRequirementEditActivity;
import sprout.app.sakmvp1.R;
import sprout.app.sakmvp1.adapters.GeneralCourseGroupAdapter;
import sprout.app.sakmvp1.models.GeneralCourseGroup;
import sprout.app.sakmvp1.models.GraduationRules;
import sprout.app.sakmvp1.models.RequirementCategory;
import sprout.app.sakmvp1.models.CourseRequirement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 교양과목 편집 Fragment (리뉴얼)
 * - 단일 과목과 선택 과목을 구분하여 표시
 * - 선택 과목은 그룹으로 묶어서 표시
 */
public class GeneralCoursesFragment extends Fragment {

    private static final String TAG = "GeneralCoursesFragment";

    private MaterialButton btnLoadFromFirestore;
    private MaterialButton btnAddGeneralCourse;
    private TextView tvCurrentDocument;
    private TextView tvEmptyGeneralCourses;

    private RecyclerView rvGeneralCourses;
    private GeneralCourseGroupAdapter adapter;

    private GraduationRules graduationRules;
    private String loadedDocumentName;  // 실제로 불러온 교양 문서명
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_general_courses, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews(view);
        setupRecyclerView();
        setupListeners();
    }

    private void initViews(View view) {
        btnLoadFromFirestore = view.findViewById(R.id.btn_load_from_firestore);
        btnAddGeneralCourse = view.findViewById(R.id.btn_add_general_course);
        tvCurrentDocument = view.findViewById(R.id.tv_current_document);
        rvGeneralCourses = view.findViewById(R.id.rv_general_courses);
        tvEmptyGeneralCourses = view.findViewById(R.id.tv_empty_general_courses);
    }

    private void setupRecyclerView() {
        adapter = new GeneralCourseGroupAdapter();
        rvGeneralCourses.setLayoutManager(new LinearLayoutManager(getContext()));
        rvGeneralCourses.setAdapter(adapter);
        adapter.setOnGroupActionListener((group, position) -> {
            showDeleteConfirmDialog(group, position);
        });

        updateEmptyState();
    }

    private void setupListeners() {
        btnLoadFromFirestore.setOnClickListener(v -> showLoadDocumentDialog());
        btnAddGeneralCourse.setOnClickListener(v -> showAddGeneralCourseDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.d(TAG, "onResume 호출");

        // Activity로부터 교양 문서 ID 가져와서 자동 로드 (한 번만)
        if (getActivity() instanceof GraduationRequirementEditActivity && loadedDocumentName == null) {
            String generalDocId = ((GraduationRequirementEditActivity) getActivity()).getGeneralEducationDocId();
            android.util.Log.d(TAG, "Activity로부터 교양 문서 ID 가져옴: " + generalDocId);

            if (generalDocId != null && !generalDocId.isEmpty()) {
                android.util.Log.d(TAG, "교양 문서 자동 로드 시작 (onResume): " + generalDocId);
                loadGeneralCoursesFromDocument(generalDocId);
            }
        }
    }

    /**
     * Firestore에서 문서 선택 다이얼로그 표시
     */
    private void showLoadDocumentDialog() {
        // 관리자 모드 확인
        android.content.SharedPreferences prefs = requireContext()
            .getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
        boolean isAdmin = prefs.getBoolean("is_admin", false);

        // 관리자가 아닐 때만 로그인 체크
        if (!isAdmin && auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("graduation_requirements")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<String> docIds = new ArrayList<>();
                List<String> displayNames = new ArrayList<>();

                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    String docId = document.getId();

                    // 교양 문서만 선택 (전공 문서 제외)
                    if (!docId.startsWith("교양_")) {
                        continue;
                    }

                    docIds.add(docId);
                    displayNames.add(docId);
                }

                if (docIds.isEmpty()) {
                    Toast.makeText(getContext(), "불러올 교양 문서가 없습니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("교양과목 불러오기")
                    .setItems(displayNames.toArray(new String[0]), (dialog, which) -> {
                        loadGeneralCoursesFromDocument(docIds.get(which));
                    })
                    .setNegativeButton("취소", null)
                    .show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "문서 목록 로드 실패: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
    }

    /**
     * Firestore에서 교양과목 데이터 로드 (v1 구조 - rules.requirements 구조)
     */
    public void loadGeneralCoursesFromDocument(String docId) {
        db.collection("graduation_requirements")
            .document(docId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    Toast.makeText(getContext(), "문서를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 불러온 문서명 저장
                loadedDocumentName = docId;

                // 문서명 표시
                if (tvCurrentDocument != null) {
                    tvCurrentDocument.setText("현재 문서: " + docId);
                }

                // v1 구조: rules.requirements 구조로 과목 저장되어 있음
                Map<String, Object> data = documentSnapshot.getData();
                if (data == null) {
                    Toast.makeText(getContext(), "문서 데이터가 없습니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                Object rulesObj = data.get("rules");
                if (!(rulesObj instanceof Map)) {
                    android.util.Log.e(TAG, "rules 객체가 Map이 아님");
                    Toast.makeText(getContext(), "교양 문서 구조가 올바르지 않습니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> rules = (Map<String, Object>) rulesObj;
                Object requirementsObj = rules.get("requirements");

                if (!(requirementsObj instanceof List)) {
                    android.util.Log.e(TAG, "requirements가 List가 아님");
                    Toast.makeText(getContext(), "교양 문서 구조가 올바르지 않습니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<?> requirements = (List<?>) requirementsObj;

                if (requirements.isEmpty()) {
                    android.util.Log.w(TAG, "과목 데이터가 없음");
                    Toast.makeText(getContext(), "과목 데이터가 없습니다", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<GeneralCourseGroup> groups = new ArrayList<>();

                // requirements 리스트에서 각 과목 정보 추출
                for (Object reqObj : requirements) {
                    if (reqObj instanceof Map) {
                        Map<String, Object> requirement = (Map<String, Object>) reqObj;

                        // 학점 정보 추출
                        Object creditObj = requirement.get("credit");
                        int credit = (creditObj instanceof Number) ?
                            ((Number) creditObj).intValue() : 3;

                        // options 필드가 있는지 확인 (선택과목)
                        if (requirement.containsKey("options")) {
                            Object optionsObj = requirement.get("options");
                            String type = (String) requirement.get("type");

                            if (optionsObj instanceof List) {
                                List<?> options = (List<?>) optionsObj;
                                List<String> courseNames = new ArrayList<>();

                                for (Object optionObj : options) {
                                    if (optionObj instanceof Map) {
                                        Map<String, Object> option = (Map<String, Object>) optionObj;
                                        String optionCourseName = (String) option.get("name");
                                        if (optionCourseName != null) {
                                            courseNames.add(optionCourseName);
                                        }
                                    }
                                }

                                if (!courseNames.isEmpty()) {
                                    GeneralCourseGroup.Type groupType = "oneOf".equals(type) ?
                                        GeneralCourseGroup.Type.ONE_OF :
                                        GeneralCourseGroup.Type.MULTIPLE;
                                    GeneralCourseGroup group = new GeneralCourseGroup(
                                        groupType, courseNames, credit);
                                    groups.add(group);
                                }
                            }
                        }
                        // 단일 과목
                        else {
                            String courseName = (String) requirement.get("name");
                            if (courseName != null && !courseName.isEmpty()) {
                                GeneralCourseGroup group = new GeneralCourseGroup(courseName, credit);
                                groups.add(group);
                            }
                        }
                    }
                }

                // 어댑터에 데이터 설정
                adapter.setGroups(groups);
                updateEmptyState();

                Toast.makeText(getContext(), "교양과목 " + groups.size() + "개 그룹을 불러왔습니다",
                    Toast.LENGTH_SHORT).show();

                android.util.Log.d(TAG, "과목 로드 완료 - " + groups.size() + "개 그룹");
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "데이터 로드 실패: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
    }

    /**
     * 교양과목 추가 다이얼로그 표시
     */
    private void showAddGeneralCourseDialog() {
        View dialogView = LayoutInflater.from(getContext())
            .inflate(R.layout.dialog_general_course_input, null);

        // Views
        android.widget.RadioGroup rgCourseType = dialogView.findViewById(R.id.rg_course_type);
        android.widget.RadioButton rbSingle = dialogView.findViewById(R.id.rb_single);
        android.widget.RadioButton rbOneOf = dialogView.findViewById(R.id.rb_one_of);

        android.widget.LinearLayout layoutSingleCourse = dialogView.findViewById(R.id.layout_single_course);
        android.widget.LinearLayout layoutMultipleCourses = dialogView.findViewById(R.id.layout_multiple_courses);

        android.widget.EditText etSingleCourseName = dialogView.findViewById(R.id.et_single_course_name);
        android.widget.EditText etMultipleCourseNames = dialogView.findViewById(R.id.et_multiple_course_names);
        android.widget.EditText etCourseCredit = dialogView.findViewById(R.id.et_course_credit);

        // 과목 타입 변경 리스너
        rgCourseType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_single) {
                layoutSingleCourse.setVisibility(View.VISIBLE);
                layoutMultipleCourses.setVisibility(View.GONE);
            } else {
                layoutSingleCourse.setVisibility(View.GONE);
                layoutMultipleCourses.setVisibility(View.VISIBLE);
            }
        });

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("교양과목 추가")
            .setView(dialogView)
            .setPositiveButton("추가", (dialog, which) -> {
                String creditStr = etCourseCredit.getText().toString().trim();

                int credit = 0;
                try {
                    credit = Integer.parseInt(creditStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "학점을 올바르게 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                GeneralCourseGroup group = null;

                if (rbSingle.isChecked()) {
                    // 단일 과목
                    String courseName = etSingleCourseName.getText().toString().trim();
                    if (courseName.isEmpty()) {
                        Toast.makeText(getContext(), "과목명을 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    group = new GeneralCourseGroup(courseName, credit);

                } else if (rbOneOf.isChecked()) {
                    // 선택 과목 (택1)
                    String courseNamesStr = etMultipleCourseNames.getText().toString().trim();
                    if (courseNamesStr.isEmpty()) {
                        Toast.makeText(getContext(), "과목명을 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 쉼표로 구분된 과목명 파싱
                    String[] names = courseNamesStr.split(",");
                    List<String> courseNames = new ArrayList<>();
                    for (String name : names) {
                        String trimmed = name.trim();
                        if (!trimmed.isEmpty()) {
                            courseNames.add(trimmed);
                        }
                    }

                    if (courseNames.isEmpty()) {
                        Toast.makeText(getContext(), "최소 1개 이상의 과목을 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    group = new GeneralCourseGroup(GeneralCourseGroup.Type.ONE_OF, courseNames, credit);
                }

                if (group != null) {
                    adapter.addGroup(group);
                    updateEmptyState();
                    Toast.makeText(getContext(), "교양과목이 추가되었습니다", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * 삭제 확인 다이얼로그 표시
     */
    private void showDeleteConfirmDialog(GeneralCourseGroup group, int position) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("과목 그룹 삭제")
            .setMessage("이 과목 그룹을 삭제하시겠습니까?\n" + group.toString())
            .setPositiveButton("삭제", (dialog, which) -> {
                adapter.removeGroup(position);
                updateEmptyState();
                Toast.makeText(getContext(), "과목 그룹이 삭제되었습니다", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * 빈 상태 TextView 업데이트
     */
    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            tvEmptyGeneralCourses.setVisibility(View.VISIBLE);
            rvGeneralCourses.setVisibility(View.GONE);
        } else {
            tvEmptyGeneralCourses.setVisibility(View.GONE);
            rvGeneralCourses.setVisibility(View.VISIBLE);
        }
    }

    /**
     * GraduationRules 데이터를 UI에 바인딩
     */
    public void bindData(GraduationRules rules) {
        android.util.Log.d(TAG, "bindData 호출");
        if (rules == null) {
            android.util.Log.w(TAG, "bindData: rules가 null입니다");
            return;
        }

        android.util.Log.d(TAG, "bindData: rules 수신 - docId=" + rules.getDocId() +
            ", sourceDocumentName=" + rules.getSourceDocumentName());

        this.graduationRules = rules;

        // 교양 Fragment는 자체적으로 불러온 문서명을 유지
        // loadedDocumentName이 있으면 그것을 표시, 없으면 Activity의 문서명 표시하지 않음
        if (tvCurrentDocument != null && loadedDocumentName != null) {
            tvCurrentDocument.setText("현재 문서: " + loadedDocumentName);
            android.util.Log.d(TAG, "독립적으로 불러온 교양 문서명 유지: " + loadedDocumentName);
        } else if (tvCurrentDocument != null && loadedDocumentName == null) {
            // 아직 교양 문서를 불러오지 않은 경우
            tvCurrentDocument.setText("현재 문서: 선택되지 않음");
            android.util.Log.d(TAG, "교양 문서 미선택 상태");
        }

        // TODO: GraduationRules에서 교양 과목 데이터 추출하여 표시
        // 현재는 Firestore에서 불러오기 기능만 구현
    }

    /**
     * UI에서 데이터 추출하여 GraduationRules에 반영
     */
    public void updateGraduationRules(GraduationRules rules) {
        if (rules == null) {
            return;
        }

        // 어댑터에서 교양 과목 그룹 목록 가져오기
        // 어댑터에 접근할 수 있도록 getGroups() 메서드 필요
        // 현재는 저장 시 어댑터의 데이터를 직접 사용
    }

    /**
     * 저장을 위해 어댑터의 교양 과목 그룹 리스트를 반환
     */
    public List<GeneralCourseGroup> getGeneralCourseGroups() {
        if (adapter != null) {
            return adapter.getGroups();
        }
        return new ArrayList<>();
    }

    /**
     * 불러온 교양 문서명 반환
     */
    public String getLoadedDocumentName() {
        return loadedDocumentName;
    }
}
