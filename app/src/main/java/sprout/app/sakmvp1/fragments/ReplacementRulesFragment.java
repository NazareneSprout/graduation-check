package sprout.app.sakmvp1.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import sprout.app.sakmvp1.adapters.ReplacementRuleEditAdapter;
import sprout.app.sakmvp1.models.GraduationRules;
import sprout.app.sakmvp1.models.ReplacementRule;
import java.util.ArrayList;
import java.util.List;

/**
 * 대체과목 규칙 편집 Fragment
 * - 폐지된 과목 → 대체 가능한 과목들의 관계 관리
 * - Firestore에서 기존 데이터 불러오기 기능
 */
public class ReplacementRulesFragment extends Fragment {

    private static final String TAG = "ReplacementRules";

    private MaterialButton btnLoadFromFirestore;
    private RecyclerView rvReplacementRules;
    private ReplacementRuleEditAdapter adapter;
    private MaterialButton btnAddRule;
    private TextView tvEmpty;

    private GraduationRules graduationRules;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_replacement_rules, container, false);
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
        rvReplacementRules = view.findViewById(R.id.rv_replacement_rules);
        btnAddRule = view.findViewById(R.id.btn_add_replacement_rule);
        tvEmpty = view.findViewById(R.id.tv_empty_replacement_rules);
    }

    private void setupRecyclerView() {
        adapter = new ReplacementRuleEditAdapter();
        rvReplacementRules.setLayoutManager(new LinearLayoutManager(getContext()));
        rvReplacementRules.setAdapter(adapter);

        adapter.setOnRuleActionListener((rule, position) -> {
            showDeleteConfirmDialog(position);
        });

        updateEmptyState();
    }

    private void setupListeners() {
        btnLoadFromFirestore.setOnClickListener(v -> showLoadDocumentDialog());
        btnAddRule.setOnClickListener(v -> showAddRuleDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.d(TAG, "onResume 호출");
        // Activity로부터 데이터 가져와서 바인딩
        if (getActivity() instanceof GraduationRequirementEditActivity) {
            GraduationRules rules = ((GraduationRequirementEditActivity) getActivity()).getGraduationRules();
            android.util.Log.d(TAG, "Activity로부터 데이터 가져옴: " + (rules != null ? "존재" : "null"));
            if (rules != null) {
                android.util.Log.d(TAG, "대체과목 규칙: " +
                    (rules.getReplacementRules() != null ? rules.getReplacementRules().size() + "개" : "없음"));
                bindData(rules);
            }
        }
    }

    /**
     * Firestore에서 문서 선택 다이얼로그 표시
     */
    private void showLoadDocumentDialog() {
        db.collection("graduation_requirements")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<String> docIds = new ArrayList<>();
                List<String> displayNames = new ArrayList<>();

                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    String docId = document.getId();
                    docIds.add(docId);

                    // 표시용 이름 생성
                    // 문서 ID 형식: "학부_트랙_학번" (예: "IT학부_멀티미디어_2023")
                    String[] parts = docId.split("_");
                    String displayName = docId;
                    if (parts.length >= 3) {
                        String department = parts[0];
                        String track = parts[1];
                        String cohort = parts[2];
                        displayName = cohort + "학번 " + department + " (" + track + ")";
                    }
                    displayNames.add(displayName);
                }

                if (docIds.isEmpty()) {
                    Toast.makeText(getContext(), "불러올 문서가 없습니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("대체과목 규칙 불러오기")
                    .setItems(displayNames.toArray(new String[0]), (dialog, which) -> {
                        loadReplacementRulesFromDocument(docIds.get(which));
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
     * Firestore에서 대체과목 규칙 데이터 로드
     */
    private void loadReplacementRulesFromDocument(String docId) {
        db.collection("graduation_requirements")
            .document(docId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    Toast.makeText(getContext(), "문서를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                // v1 형식에서 replacementRules 파싱
                Object replacementRulesObj = documentSnapshot.get("replacementRules");
                if (replacementRulesObj instanceof java.util.List) {
                    java.util.List<ReplacementRule> replacementRules = parseReplacementRules((java.util.List<?>) replacementRulesObj);

                    if (replacementRules != null && !replacementRules.isEmpty()) {
                        adapter.setRules(replacementRules);
                        updateEmptyState();
                        Toast.makeText(getContext(), "대체과목 규칙을 불러왔습니다 (" + replacementRules.size() + "개)",
                            Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "대체과목 규칙이 없습니다", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "대체과목 규칙이 없습니다", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "데이터 로드 실패: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
    }

    /**
     * v1 replacementRules 데이터 파싱
     */
    private java.util.List<ReplacementRule> parseReplacementRules(java.util.List<?> rulesList) {
        java.util.List<ReplacementRule> replacementRules = new java.util.ArrayList<>();

        for (Object ruleObj : rulesList) {
            if (!(ruleObj instanceof java.util.Map)) {
                continue;
            }

            java.util.Map<String, Object> ruleMap = (java.util.Map<String, Object>) ruleObj;
            ReplacementRule rule = new ReplacementRule();

            // discontinuedCourse 파싱
            Object discontinuedObj = ruleMap.get("discontinuedCourse");
            if (discontinuedObj instanceof java.util.Map) {
                java.util.Map<String, Object> discontinuedMap = (java.util.Map<String, Object>) discontinuedObj;
                ReplacementRule.CourseInfo discontinuedCourse = new ReplacementRule.CourseInfo();
                discontinuedCourse.setName((String) discontinuedMap.get("name"));
                discontinuedCourse.setCategory((String) discontinuedMap.get("category"));
                Object creditsObj = discontinuedMap.get("credits");
                if (creditsObj instanceof Number) {
                    discontinuedCourse.setCredits(((Number) creditsObj).intValue());
                }
                rule.setDiscontinuedCourse(discontinuedCourse);
            }

            // replacementCourses 파싱
            Object replacementCoursesObj = ruleMap.get("replacementCourses");
            if (replacementCoursesObj instanceof java.util.List) {
                java.util.List<?> replacementCoursesList = (java.util.List<?>) replacementCoursesObj;
                java.util.List<ReplacementRule.CourseInfo> replacementCourses = new java.util.ArrayList<>();

                for (Object courseObj : replacementCoursesList) {
                    if (courseObj instanceof java.util.Map) {
                        java.util.Map<String, Object> courseMap = (java.util.Map<String, Object>) courseObj;
                        ReplacementRule.CourseInfo courseInfo = new ReplacementRule.CourseInfo();
                        courseInfo.setName((String) courseMap.get("name"));
                        courseInfo.setCategory((String) courseMap.get("category"));
                        Object creditsObj = courseMap.get("credits");
                        if (creditsObj instanceof Number) {
                            courseInfo.setCredits(((Number) creditsObj).intValue());
                        }
                        replacementCourses.add(courseInfo);
                    }
                }
                rule.setReplacementCourses(replacementCourses);
            }

            // scope 필드 파싱
            String scope = (String) ruleMap.get("scope");
            rule.setScope(scope != null ? scope : "document");

            replacementRules.add(rule);
        }

        return replacementRules;
    }

    /**
     * 대체과목 규칙 추가 다이얼로그 표시
     */
    private void showAddRuleDialog() {
        // 먼저 현재 입력된 전공과목과 교양과목 목록 가져오기
        List<String> allCourseNames = getAllCourseNames();

        if (allCourseNames.isEmpty()) {
            Toast.makeText(getContext(), "먼저 전공과목 또는 교양과목을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // CourseInfo 객체 리스트로 변환
        List<ReplacementRule.CourseInfo> allCourses = new ArrayList<>();
        for (String courseName : allCourseNames) {
            allCourses.add(getCourseInfo(courseName));
        }

        // 1단계: 폐지된 과목 선택 (정렬 기능 있는 다이얼로그)
        showCourseSelectionDialog("폐지된 과목 선택", allCourses, false, selectedCourses -> {
            if (selectedCourses.isEmpty()) {
                return;
            }
            ReplacementRule.CourseInfo discontinuedCourse = selectedCourses.get(0);

            // 2단계: 대체 과목들 선택 (폐지된 과목 제외)
            List<ReplacementRule.CourseInfo> availableCourses = new ArrayList<>();
            for (ReplacementRule.CourseInfo course : allCourses) {
                if (!course.getName().equals(discontinuedCourse.getName())) {
                    availableCourses.add(course);
                }
            }

            showCourseSelectionDialog("대체 가능한 과목들 선택", availableCourses, true, replacementCourses -> {
                if (replacementCourses.isEmpty()) {
                    Toast.makeText(getContext(), "최소 1개 이상의 대체과목을 선택하세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 3단계: 적용 범위 선택
                showScopeSelectionDialog(discontinuedCourse, replacementCourses);
            });
        });
    }

    /**
     * 과목 선택 다이얼로그 콜백 인터페이스
     */
    interface OnCoursesSelectedListener {
        void onCoursesSelected(List<ReplacementRule.CourseInfo> selectedCourses);
    }

    /**
     * 정렬 기능이 있는 과목 선택 다이얼로그
     */
    private void showCourseSelectionDialog(String title, List<ReplacementRule.CourseInfo> courses,
                                          boolean multiSelect, OnCoursesSelectedListener listener) {
        if (courses.isEmpty()) {
            Toast.makeText(getContext(), "선택 가능한 과목이 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_course_selection, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // UI 요소 참조
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        com.google.android.material.chip.ChipGroup chipGroupSort = dialogView.findViewById(R.id.chip_group_sort);
        com.google.android.material.textfield.TextInputEditText etSearch = dialogView.findViewById(R.id.et_search);
        RecyclerView rvCourses = dialogView.findViewById(R.id.rv_courses);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        tvTitle.setText(title);

        // 정렬 타입
        final boolean[] sortAscending = {true};
        final List<ReplacementRule.CourseInfo> displayedCourses = new ArrayList<>(courses);
        final List<ReplacementRule.CourseInfo> selectedCourses = new ArrayList<>();

        // 어댑터 설정
        CourseSelectionAdapter selectionAdapter = new CourseSelectionAdapter(
            displayedCourses, multiSelect, selectedCourses);
        rvCourses.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCourses.setAdapter(selectionAdapter);

        // 정렬 적용
        Runnable applySorting = () -> {
            displayedCourses.clear();

            // 검색 필터
            String query = etSearch.getText().toString().trim().toLowerCase();
            List<ReplacementRule.CourseInfo> filtered = new ArrayList<>();
            for (ReplacementRule.CourseInfo course : courses) {
                if (query.isEmpty() || course.getName().toLowerCase().contains(query)) {
                    filtered.add(course);
                }
            }

            // 정렬
            java.util.Collections.sort(filtered, (c1, c2) -> {
                int result = c1.getName().compareTo(c2.getName());
                return sortAscending[0] ? result : -result;
            });

            displayedCourses.addAll(filtered);
            selectionAdapter.notifyDataSetChanged();
        };

        // 초기 정렬
        applySorting.run();

        // 정렬 칩 리스너
        chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_sort_name_asc)) {
                sortAscending[0] = true;
            } else if (checkedIds.contains(R.id.chip_sort_name_desc)) {
                sortAscending[0] = false;
            }
            applySorting.run();
        });

        // 검색 텍스트 변경 리스너
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySorting.run();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // 버튼 리스너
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            if (selectedCourses.isEmpty()) {
                Toast.makeText(requireContext(), "과목을 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            listener.onCoursesSelected(new ArrayList<>(selectedCourses));
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * 적용 범위 선택 다이얼로그
     */
    private void showScopeSelectionDialog(ReplacementRule.CourseInfo discontinuedCourse,
                                         List<ReplacementRule.CourseInfo> replacementCourses) {
        String[] scopeOptions = new String[]{
            "해당 문서에만 적용",
            "학부 전체에 적용"
        };

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("적용 범위 선택")
            .setItems(scopeOptions, (dialog, which) -> {
                String scope = (which == 0) ? "document" : "department";

                // ReplacementRule 생성
                ReplacementRule newRule = new ReplacementRule();
                newRule.setDiscontinuedCourse(discontinuedCourse);
                newRule.setReplacementCourses(replacementCourses);
                newRule.setScope(scope);

                adapter.addRule(newRule);
                updateEmptyState();

                String scopeText = scope.equals("document") ? "해당 문서에만" : "학부 전체에";
                Toast.makeText(getContext(), "대체과목 규칙이 추가되었습니다 (" + scopeText + " 적용)",
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * 현재 입력된 모든 과목 이름 목록 가져오기
     * categories에서 가져오거나, Fragments의 adapters에서 가져옴
     */
    private List<String> getAllCourseNames() {
        List<String> courseNames = new ArrayList<>();

        // categories 리스트에서 전공과목 가져오기
        if (graduationRules != null && graduationRules.getCategories() != null) {
            for (sprout.app.sakmvp1.models.RequirementCategory category : graduationRules.getCategories()) {
                if (category.getCourses() != null) {
                    for (sprout.app.sakmvp1.models.CourseRequirement course : category.getCourses()) {
                        String courseName = course.getName();
                        if (courseName != null && !courseNames.contains(courseName)) {
                            courseNames.add(courseName);
                        }
                    }
                }
            }
        }

        // Fragments의 adapters에서 직접 과목 가져오기
        if (getActivity() instanceof GraduationRequirementEditActivity) {
            GraduationRequirementEditActivity activity = (GraduationRequirementEditActivity) getActivity();
            if (activity.pagerAdapter != null) {
                // 전공 과목 가져오기
                sprout.app.sakmvp1.fragments.MajorCoursesFragment majorFragment =
                    activity.pagerAdapter.getMajorFragment();
                if (majorFragment != null) {
                    // MajorCoursesFragment의 adapterMajorRequired, adapterMajorElective, adapterDeptCommon에서 가져오기
                    // 이 adapters는 CourseEditAdapter 타입이고 getCourses() 메서드가 있음
                    try {
                        java.lang.reflect.Field field;

                        // 전공필수
                        field = majorFragment.getClass().getDeclaredField("adapterMajorRequired");
                        field.setAccessible(true);
                        sprout.app.sakmvp1.adapters.CourseEditAdapter adapterMajorRequired =
                            (sprout.app.sakmvp1.adapters.CourseEditAdapter) field.get(majorFragment);
                        if (adapterMajorRequired != null) {
                            for (sprout.app.sakmvp1.models.CourseRequirement course : adapterMajorRequired.getCourses()) {
                                String name = course.getName();
                                if (name != null && !courseNames.contains(name)) {
                                    courseNames.add(name);
                                }
                            }
                        }

                        // 전공선택
                        field = majorFragment.getClass().getDeclaredField("adapterMajorElective");
                        field.setAccessible(true);
                        sprout.app.sakmvp1.adapters.CourseEditAdapter adapterMajorElective =
                            (sprout.app.sakmvp1.adapters.CourseEditAdapter) field.get(majorFragment);
                        if (adapterMajorElective != null) {
                            for (sprout.app.sakmvp1.models.CourseRequirement course : adapterMajorElective.getCourses()) {
                                String name = course.getName();
                                if (name != null && !courseNames.contains(name)) {
                                    courseNames.add(name);
                                }
                            }
                        }

                        // 학부공통/전공심화
                        field = majorFragment.getClass().getDeclaredField("adapterDeptCommon");
                        field.setAccessible(true);
                        sprout.app.sakmvp1.adapters.CourseEditAdapter adapterDeptCommon =
                            (sprout.app.sakmvp1.adapters.CourseEditAdapter) field.get(majorFragment);
                        if (adapterDeptCommon != null) {
                            for (sprout.app.sakmvp1.models.CourseRequirement course : adapterDeptCommon.getCourses()) {
                                String name = course.getName();
                                if (name != null && !courseNames.contains(name)) {
                                    courseNames.add(name);
                                }
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.w(TAG, "전공 과목 가져오기 실패 (Reflection): " + e.getMessage());
                    }
                }

                // 교양 과목 가져오기
                sprout.app.sakmvp1.fragments.GeneralCoursesFragment generalFragment =
                    activity.pagerAdapter.getGeneralFragment();
                if (generalFragment != null) {
                    java.util.List<sprout.app.sakmvp1.models.GeneralCourseGroup> groups =
                        generalFragment.getGeneralCourseGroups();
                    if (groups != null) {
                        for (sprout.app.sakmvp1.models.GeneralCourseGroup group : groups) {
                            if (group.getCourseNames() != null) {
                                for (String courseName : group.getCourseNames()) {
                                    if (!courseNames.contains(courseName)) {
                                        courseNames.add(courseName);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return courseNames;
    }

    /**
     * 과목 이름으로 CourseInfo 생성
     * categories와 Fragment adapters에서 과목 정보를 찾음
     */
    private ReplacementRule.CourseInfo getCourseInfo(String courseName) {
        // 1. categories 리스트에서 전공과목 찾기
        if (graduationRules != null && graduationRules.getCategories() != null) {
            for (sprout.app.sakmvp1.models.RequirementCategory category : graduationRules.getCategories()) {
                if (category.getCourses() != null) {
                    for (sprout.app.sakmvp1.models.CourseRequirement course : category.getCourses()) {
                        if (courseName.equals(course.getName())) {
                            return new ReplacementRule.CourseInfo(
                                courseName,
                                category.getName(), // 전공필수, 전공선택, 학부공통, 전공심화
                                course.getCredits()
                            );
                        }
                    }
                }
            }
        }

        // 2. Fragment adapters에서 전공과목 찾기
        if (getActivity() instanceof GraduationRequirementEditActivity) {
            GraduationRequirementEditActivity activity = (GraduationRequirementEditActivity) getActivity();
            if (activity.pagerAdapter != null) {
                sprout.app.sakmvp1.fragments.MajorCoursesFragment majorFragment =
                    activity.pagerAdapter.getMajorFragment();

                if (majorFragment != null) {
                    try {
                        java.lang.reflect.Field field;

                        // 전공필수에서 찾기
                        field = majorFragment.getClass().getDeclaredField("adapterMajorRequired");
                        field.setAccessible(true);
                        sprout.app.sakmvp1.adapters.CourseEditAdapter adapterMajorRequired =
                            (sprout.app.sakmvp1.adapters.CourseEditAdapter) field.get(majorFragment);
                        if (adapterMajorRequired != null) {
                            for (sprout.app.sakmvp1.models.CourseRequirement course : adapterMajorRequired.getCourses()) {
                                if (courseName.equals(course.getName())) {
                                    return new ReplacementRule.CourseInfo(courseName, "전공필수", course.getCredits());
                                }
                            }
                        }

                        // 전공선택에서 찾기
                        field = majorFragment.getClass().getDeclaredField("adapterMajorElective");
                        field.setAccessible(true);
                        sprout.app.sakmvp1.adapters.CourseEditAdapter adapterMajorElective =
                            (sprout.app.sakmvp1.adapters.CourseEditAdapter) field.get(majorFragment);
                        if (adapterMajorElective != null) {
                            for (sprout.app.sakmvp1.models.CourseRequirement course : adapterMajorElective.getCourses()) {
                                if (courseName.equals(course.getName())) {
                                    return new ReplacementRule.CourseInfo(courseName, "전공선택", course.getCredits());
                                }
                            }
                        }

                        // 학부공통/전공심화에서 찾기
                        field = majorFragment.getClass().getDeclaredField("deptCommonCategoryName");
                        field.setAccessible(true);
                        String deptCommonCategoryName = (String) field.get(majorFragment);

                        field = majorFragment.getClass().getDeclaredField("adapterDeptCommon");
                        field.setAccessible(true);
                        sprout.app.sakmvp1.adapters.CourseEditAdapter adapterDeptCommon =
                            (sprout.app.sakmvp1.adapters.CourseEditAdapter) field.get(majorFragment);
                        if (adapterDeptCommon != null) {
                            for (sprout.app.sakmvp1.models.CourseRequirement course : adapterDeptCommon.getCourses()) {
                                if (courseName.equals(course.getName())) {
                                    return new ReplacementRule.CourseInfo(
                                        courseName,
                                        deptCommonCategoryName != null ? deptCommonCategoryName : "학부공통",
                                        course.getCredits()
                                    );
                                }
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.w(TAG, "전공 과목 정보 가져오기 실패 (Reflection): " + e.getMessage());
                    }
                }

                // 3. 교양과목에서 찾기
                sprout.app.sakmvp1.fragments.GeneralCoursesFragment generalFragment =
                    activity.pagerAdapter.getGeneralFragment();
                if (generalFragment != null) {
                    java.util.List<sprout.app.sakmvp1.models.GeneralCourseGroup> groups =
                        generalFragment.getGeneralCourseGroups();
                    if (groups != null) {
                        for (sprout.app.sakmvp1.models.GeneralCourseGroup group : groups) {
                            if (group.getCourseNames() != null && group.getCourseNames().contains(courseName)) {
                                // 교양과목은 groupName과 credit 정보 사용
                                return new ReplacementRule.CourseInfo(
                                    courseName,
                                    group.isSingle() ? "교양" : "교양선택",
                                    group.getCredit()
                                );
                            }
                        }
                    }
                }
            }
        }

        // 못 찾으면 기본값 (과목명, 카테고리 "", 3학점)
        return new ReplacementRule.CourseInfo(courseName, "", 3);
    }

    /**
     * 삭제 확인 다이얼로그 표시
     */
    private void showDeleteConfirmDialog(int position) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("규칙 삭제")
            .setMessage("이 대체과목 규칙을 삭제하시겠습니까?")
            .setPositiveButton("삭제", (dialog, which) -> {
                adapter.removeRule(position);
                updateEmptyState();
                Toast.makeText(getContext(), "규칙이 삭제되었습니다", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * 빈 상태 TextView 업데이트
     */
    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvReplacementRules.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvReplacementRules.setVisibility(View.VISIBLE);
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

        this.graduationRules = rules;
        List<ReplacementRule> replacementRules = rules.getReplacementRules();

        if (replacementRules != null) {
            android.util.Log.d(TAG, "대체과목 규칙 " + replacementRules.size() + "개 바인딩");
            adapter.setRules(replacementRules);
        } else {
            android.util.Log.d(TAG, "대체과목 규칙 없음");
        }

        updateEmptyState();
    }

    /**
     * UI에서 데이터 추출하여 GraduationRules에 반영
     */
    public void updateGraduationRules(GraduationRules rules) {
        if (rules == null) {
            return;
        }

        List<ReplacementRule> replacementRules = adapter.getRules();
        rules.setReplacementRules(replacementRules);
    }

    /**
     * 과목 선택 어댑터 (단일/다중 선택 지원)
     */
    class CourseSelectionAdapter extends RecyclerView.Adapter<CourseSelectionAdapter.ViewHolder> {
        private List<ReplacementRule.CourseInfo> courses;
        private boolean multiSelect;
        private List<ReplacementRule.CourseInfo> selectedCourses;

        CourseSelectionAdapter(List<ReplacementRule.CourseInfo> courses, boolean multiSelect,
                              List<ReplacementRule.CourseInfo> selectedCourses) {
            this.courses = courses;
            this.multiSelect = multiSelect;
            this.selectedCourses = selectedCourses;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course_selectable, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(courses.get(position));
        }

        @Override
        public int getItemCount() {
            return courses.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            com.google.android.material.checkbox.MaterialCheckBox cbSelect;
            TextView tvCourseName, tvCourseCategory, tvCourseCredits;

            ViewHolder(View itemView) {
                super(itemView);
                cbSelect = itemView.findViewById(R.id.cb_select);
                tvCourseName = itemView.findViewById(R.id.tv_course_name);
                tvCourseCategory = itemView.findViewById(R.id.tv_course_category);
                tvCourseCredits = itemView.findViewById(R.id.tv_course_credits);
            }

            void bind(ReplacementRule.CourseInfo course) {
                tvCourseName.setText(course.getName());
                tvCourseCategory.setText(course.getCategory());
                tvCourseCredits.setText(course.getCredits() + "학점");

                cbSelect.setChecked(selectedCourses.contains(course));

                itemView.setOnClickListener(v -> {
                    if (multiSelect) {
                        // 다중 선택
                        if (selectedCourses.contains(course)) {
                            selectedCourses.remove(course);
                            cbSelect.setChecked(false);
                        } else {
                            selectedCourses.add(course);
                            cbSelect.setChecked(true);
                        }
                    } else {
                        // 단일 선택
                        selectedCourses.clear();
                        selectedCourses.add(course);
                        notifyDataSetChanged();
                    }
                });

                cbSelect.setOnClickListener(v -> itemView.performClick());
            }
        }
    }
}
