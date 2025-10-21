package sprout.app.sakmvp1.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import sprout.app.sakmvp1.adapters.CourseEditAdapter;
import sprout.app.sakmvp1.models.GraduationRules;
import sprout.app.sakmvp1.models.RequirementCategory;
import sprout.app.sakmvp1.models.CourseRequirement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 전공과목 편집 Fragment
 * - 전공필수, 전공선택, 학부공통 과목 관리
 * - Firestore에서 기존 데이터 불러오기 기능
 */
public class MajorCoursesFragment extends Fragment {

    private static final String TAG = "MajorCoursesFragment";

    private MaterialButton btnLoadFromFirestore;
    private TextView tvCurrentDocument;

    // 전공필수
    private RecyclerView rvMajorRequired;
    private CourseEditAdapter adapterMajorRequired;
    private MaterialButton btnAddMajorRequired;
    private TextView tvEmptyMajorRequired;

    // 전공선택
    private RecyclerView rvMajorElective;
    private CourseEditAdapter adapterMajorElective;
    private MaterialButton btnAddMajorElective;
    private TextView tvEmptyMajorElective;

    // 학부공통 또는 전공심화 (동적으로 사용)
    private RecyclerView rvDeptCommon;
    private CourseEditAdapter adapterDeptCommon;
    private MaterialButton btnAddDeptCommon;
    private TextView tvEmptyDeptCommon;
    private TextView tvDeptCommonTitle;  // 타이틀 TextView

    private GraduationRules graduationRules;
    private String deptCommonCategoryName = "학부공통";  // 기본값: 학부공통
    private String loadedDocumentName;  // 실제로 불러온 전공 문서명
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private boolean isDataLoading = false;  // 데이터 로딩 중인지 플래그

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_major_courses, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews(view);
        setupRecyclerViews();
        setupListeners();
    }

    private void initViews(View view) {
        btnLoadFromFirestore = view.findViewById(R.id.btn_load_from_firestore);
        tvCurrentDocument = view.findViewById(R.id.tv_current_document);

        // 전공필수
        rvMajorRequired = view.findViewById(R.id.rv_major_required);
        btnAddMajorRequired = view.findViewById(R.id.btn_add_major_required);
        tvEmptyMajorRequired = view.findViewById(R.id.tv_empty_major_required);

        // 전공선택
        rvMajorElective = view.findViewById(R.id.rv_major_elective);
        btnAddMajorElective = view.findViewById(R.id.btn_add_major_elective);
        tvEmptyMajorElective = view.findViewById(R.id.tv_empty_major_elective);

        // 학부공통 또는 전공심화
        rvDeptCommon = view.findViewById(R.id.rv_dept_common);
        btnAddDeptCommon = view.findViewById(R.id.btn_add_dept_common);
        tvEmptyDeptCommon = view.findViewById(R.id.tv_empty_dept_common);
        tvDeptCommonTitle = view.findViewById(R.id.tv_dept_common_title);
    }

    private void setupRecyclerViews() {
        // 전공필수 RecyclerView
        adapterMajorRequired = new CourseEditAdapter();
        rvMajorRequired.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMajorRequired.setAdapter(adapterMajorRequired);
        adapterMajorRequired.setOnCourseActionListener((course, position) -> {
            showDeleteConfirmDialog("전공필수", position, adapterMajorRequired);
        });
        adapterMajorRequired.setOnDataChangedListener(() -> {
            if (!isDataLoading && getActivity() instanceof GraduationRequirementEditActivity) {
                ((GraduationRequirementEditActivity) getActivity()).markAsModified();
            }
        });

        // 전공선택 RecyclerView
        adapterMajorElective = new CourseEditAdapter();
        rvMajorElective.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMajorElective.setAdapter(adapterMajorElective);
        adapterMajorElective.setOnCourseActionListener((course, position) -> {
            showDeleteConfirmDialog("전공선택", position, adapterMajorElective);
        });
        adapterMajorElective.setOnDataChangedListener(() -> {
            if (!isDataLoading && getActivity() instanceof GraduationRequirementEditActivity) {
                ((GraduationRequirementEditActivity) getActivity()).markAsModified();
            }
        });

        // 학부공통/전공심화 RecyclerView
        adapterDeptCommon = new CourseEditAdapter();
        rvDeptCommon.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDeptCommon.setAdapter(adapterDeptCommon);
        adapterDeptCommon.setOnCourseActionListener((course, position) -> {
            showDeleteConfirmDialog(deptCommonCategoryName, position, adapterDeptCommon);
        });
        adapterDeptCommon.setOnDataChangedListener(() -> {
            if (!isDataLoading && getActivity() instanceof GraduationRequirementEditActivity) {
                ((GraduationRequirementEditActivity) getActivity()).markAsModified();
            }
        });

        updateEmptyStates();
    }

    private void setupListeners() {
        btnLoadFromFirestore.setOnClickListener(v -> showLoadDocumentDialog());
        btnAddMajorRequired.setOnClickListener(v -> showAddCourseDialog("전공필수"));
        btnAddMajorElective.setOnClickListener(v -> showAddCourseDialog("전공선택"));
        btnAddDeptCommon.setOnClickListener(v -> showAddCourseDialog(deptCommonCategoryName));
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
                bindData(rules);
            }
        }
    }

    /**
     * 현재 문서명 표시 업데이트
     */
    private void updateCurrentDocumentDisplay() {
        android.util.Log.d(TAG, "updateCurrentDocumentDisplay 호출");
        android.util.Log.d(TAG, "tvCurrentDocument: " + (tvCurrentDocument != null ? "존재" : "null"));
        android.util.Log.d(TAG, "graduationRules: " + (graduationRules != null ? "존재" : "null"));
        if (graduationRules != null) {
            android.util.Log.d(TAG, "sourceDocumentName: " + graduationRules.getSourceDocumentName());
        }

        if (tvCurrentDocument != null && graduationRules != null && graduationRules.getSourceDocumentName() != null) {
            String docName = graduationRules.getSourceDocumentName();
            tvCurrentDocument.setText("현재 문서: " + docName);
            android.util.Log.d(TAG, "문서명 설정 완료: " + docName);
        } else {
            android.util.Log.w(TAG, "문서명 설정 실패 - tvCurrentDocument null? " + (tvCurrentDocument == null) +
                ", graduationRules null? " + (graduationRules == null) +
                ", sourceDocumentName null? " + (graduationRules == null || graduationRules.getSourceDocumentName() == null));
        }
    }

    /**
     * Firestore에서 문서 선택 다이얼로그 표시 (v1 구조 사용)
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

                    // 교양 문서는 제외 (전공 문서만)
                    if (docId.startsWith("교양_")) {
                        continue;
                    }

                    docIds.add(docId);
                    displayNames.add(docId);  // v1에서는 docId를 그대로 표시
                }

                if (docIds.isEmpty()) {
                    Toast.makeText(getContext(), "불러올 문서가 없습니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("전공과목 불러오기")
                    .setItems(displayNames.toArray(new String[0]), (dialog, which) -> {
                        loadMajorCoursesFromDocument(docIds.get(which));
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
     * Firestore에서 전공과목 데이터 로드 (v1 구조 - 학기별로 과목 저장)
     */
    public void loadMajorCoursesFromDocument(String docId) {
        android.util.Log.d(TAG, "loadMajorCoursesFromDocument 호출: " + docId);

        db.collection("graduation_requirements")
            .document(docId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                android.util.Log.d(TAG, "문서 로드 성공: " + docId);

                if (!documentSnapshot.exists()) {
                    android.util.Log.w(TAG, "문서가 존재하지 않음: " + docId);
                    Toast.makeText(getContext(), "문서를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 로드된 문서명 저장
                loadedDocumentName = docId;

                // 문서명 표시
                if (tvCurrentDocument != null) {
                    tvCurrentDocument.setText("현재 문서: " + docId);
                }

                // v1 구조: 학기별로 과목 저장되어 있음
                // 문서 전체 데이터 가져오기
                Map<String, Object> data = documentSnapshot.getData();
                if (data == null) {
                    Toast.makeText(getContext(), "문서 데이터가 없습니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 문서 전체 구조 로그 출력 (디버깅용)
                android.util.Log.d(TAG, "=== 문서 전체 키 목록 ===");
                for (String key : data.keySet()) {
                    Object value = data.get(key);
                    String valueType = value != null ? value.getClass().getSimpleName() : "null";
                    android.util.Log.d(TAG, "키: " + key + " | 타입: " + valueType);
                }
                android.util.Log.d(TAG, "===================");

                // rules 객체 확인 (전공 문서는 rules 안에 과목 정보가 있음)
                Object rulesObj = data.get("rules");
                Map<String, Object> rules;

                if (!(rulesObj instanceof Map)) {
                    // rules가 없으면 빈 구조로 초기화 (새 문서이거나 아직 과목이 추가되지 않은 경우)
                    android.util.Log.w(TAG, "rules 필드가 없습니다. 빈 구조로 초기화합니다.");
                    rules = new HashMap<>();
                } else {
                    rules = (Map<String, Object>) rulesObj;
                }
                android.util.Log.d(TAG, "=== rules 내부 키 목록 ===");
                for (String key : rules.keySet()) {
                    Object value = rules.get(key);
                    String valueType = value != null ? value.getClass().getSimpleName() : "null";
                    android.util.Log.d(TAG, "키: " + key + " | 타입: " + valueType);
                }
                android.util.Log.d(TAG, "===================");

                // 중복 체크를 위한 Set
                Set<String> addedMajorRequired = new HashSet<>();
                Set<String> addedMajorElective = new HashSet<>();
                Set<String> addedDeptCommon = new HashSet<>();

                // 어댑터 초기화
                adapterMajorRequired.clearCourses();
                adapterMajorElective.clearCourses();
                adapterDeptCommon.clearCourses();

                // rules 안에서 학기별로 과목 추출
                for (Map.Entry<String, Object> entry : rules.entrySet()) {
                    String key = entry.getKey();
                    android.util.Log.d(TAG, "처리 중인 rules 키: " + key);

                    // 학년학기 키만 처리 (예: 1학년1학기, 2학년1학기 등)
                    if (key.contains("학년") && entry.getValue() instanceof Map) {
                        android.util.Log.d(TAG, "학기 키 발견: " + key);
                        Map<String, Object> semester = (Map<String, Object>) entry.getValue();

                        // 학기 내부 구조 로그
                        android.util.Log.d(TAG, "  학기 내부 키 목록:");
                        for (String semesterKey : semester.keySet()) {
                            Object semesterValue = semester.get(semesterKey);
                            String semesterValueType = semesterValue != null ? semesterValue.getClass().getSimpleName() : "null";
                            android.util.Log.d(TAG, "    - " + semesterKey + " (" + semesterValueType + ")");
                        }

                        // 전공필수 과목 추출
                        Object majorRequiredObj = semester.get("전공필수");
                        android.util.Log.d(TAG, "  전공필수 객체: " + majorRequiredObj);
                        if (majorRequiredObj instanceof List) {
                            List<?> courseList = (List<?>) majorRequiredObj;
                            android.util.Log.d(TAG, "  전공필수 과목 개수: " + courseList.size());
                            for (Object courseObj : courseList) {
                                android.util.Log.d(TAG, "    전공필수 과목 객체 타입: " + (courseObj != null ? courseObj.getClass().getSimpleName() : "null"));
                                if (courseObj instanceof Map) {
                                    Map<String, Object> course = (Map<String, Object>) courseObj;
                                    android.util.Log.d(TAG, "    전공필수 과목 맵 키: " + course.keySet());
                                    String courseName = (String) course.get("과목명");
                                    Object creditObj = course.get("학점");
                                    int credit = (creditObj instanceof Number) ?
                                        ((Number) creditObj).intValue() : 3;

                                    android.util.Log.d(TAG, "    추출된 과목명: " + courseName + ", 학점: " + credit);

                                    if (courseName != null && !addedMajorRequired.contains(courseName)) {
                                        addedMajorRequired.add(courseName);
                                        CourseRequirement courseReq = new CourseRequirement(courseName, credit);
                                        courseReq.setSemester(key);  // 학기 정보 설정
                                        adapterMajorRequired.addCourse(courseReq);
                                        android.util.Log.d(TAG, "    전공필수 과목 추가됨: " + courseName + " (" + key + ")");
                                    }
                                }
                            }
                        } else {
                            android.util.Log.d(TAG, "  전공필수가 List 타입이 아님");
                        }

                        // 전공선택 과목 추출
                        Object majorElectiveObj = semester.get("전공선택");
                        if (majorElectiveObj instanceof List) {
                            List<?> courseList = (List<?>) majorElectiveObj;
                            for (Object courseObj : courseList) {
                                if (courseObj instanceof Map) {
                                    Map<String, Object> course = (Map<String, Object>) courseObj;
                                    String courseName = (String) course.get("과목명");
                                    Object creditObj = course.get("학점");
                                    int credit = (creditObj instanceof Number) ?
                                        ((Number) creditObj).intValue() : 3;

                                    if (courseName != null && !addedMajorElective.contains(courseName)) {
                                        addedMajorElective.add(courseName);
                                        CourseRequirement courseReq = new CourseRequirement(courseName, credit);
                                        courseReq.setSemester(key);  // 학기 정보 설정
                                        adapterMajorElective.addCourse(courseReq);
                                    }
                                }
                            }
                        }

                        // 학부공통 또는 전공심화 과목 추출 (2024-10-19: 학부공통필수 → 학부공통 병합 완료)
                        Object deptCommonObj = semester.get("학부공통");
                        boolean isCommon = (deptCommonObj != null);

                        if (!isCommon) {
                            deptCommonObj = semester.get("전공심화");
                        }

                        if (deptCommonObj instanceof List) {
                            deptCommonCategoryName = isCommon ? "학부공통" : "전공심화";
                            updateDeptCommonTitle();

                            List<?> courseList = (List<?>) deptCommonObj;
                            for (Object courseObj : courseList) {
                                if (courseObj instanceof Map) {
                                    Map<String, Object> course = (Map<String, Object>) courseObj;
                                    String courseName = (String) course.get("과목명");
                                    Object creditObj = course.get("학점");
                                    int credit = (creditObj instanceof Number) ?
                                        ((Number) creditObj).intValue() : 3;

                                    if (courseName != null && !addedDeptCommon.contains(courseName)) {
                                        addedDeptCommon.add(courseName);
                                        CourseRequirement courseReq = new CourseRequirement(courseName, credit);
                                        courseReq.setSemester(key);  // 학기 정보 설정
                                        adapterDeptCommon.addCourse(courseReq);
                                    }
                                }
                            }
                        }
                    }
                }

                updateEmptyStates();

                int totalCourses = addedMajorRequired.size() + addedMajorElective.size() + addedDeptCommon.size();
                Toast.makeText(getContext(), "전공과목 " + totalCourses + "개를 불러왔습니다",
                    Toast.LENGTH_SHORT).show();

                android.util.Log.d(TAG, "과목 로드 완료 - 전공필수: " + addedMajorRequired.size() +
                    ", 전공선택: " + addedMajorElective.size() +
                    ", " + deptCommonCategoryName + ": " + addedDeptCommon.size());
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "데이터 로드 실패: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
    }

    /**
     * 과목 추가 다이얼로그 표시
     */
    private void showAddCourseDialog(String categoryName) {
        View dialogView = LayoutInflater.from(getContext())
            .inflate(R.layout.dialog_course_input, null);

        EditText etCourseName = dialogView.findViewById(R.id.et_course_name);
        EditText etCourseCredit = dialogView.findViewById(R.id.et_course_credit);
        android.widget.Spinner spinnerSemester = dialogView.findViewById(R.id.spinner_semester);

        // 학기 선택 Spinner 설정
        String[] semesters = {
            "1학년 1학기", "1학년 2학기",
            "2학년 1학기", "2학년 2학기",
            "3학년 1학기", "3학년 2학기",
            "4학년 1학기", "4학년 2학기"
        };
        android.widget.ArrayAdapter<String> semesterAdapter = new android.widget.ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            semesters
        );
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemester.setAdapter(semesterAdapter);

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(categoryName + " 과목 추가")
            .setView(dialogView)
            .setPositiveButton("추가", (dialog, which) -> {
                String courseName = etCourseName.getText().toString().trim();
                String creditStr = etCourseCredit.getText().toString().trim();
                String semester = (String) spinnerSemester.getSelectedItem();

                if (courseName.isEmpty()) {
                    Toast.makeText(getContext(), "과목명을 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                int credit = 0;
                try {
                    credit = Integer.parseInt(creditStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "학점을 올바르게 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 학기 정보 포함하여 과목 생성
                CourseRequirement newCourse = new CourseRequirement(courseName, credit);
                newCourse.setSemester(semester);

                // 카테고리별로 추가
                if ("전공필수".equals(categoryName)) {
                    adapterMajorRequired.addCourse(newCourse);
                } else if ("전공선택".equals(categoryName)) {
                    adapterMajorElective.addCourse(newCourse);
                } else if ("학부공통".equals(categoryName) || "전공심화".equals(categoryName)) {
                    adapterDeptCommon.addCourse(newCourse);
                }

                updateEmptyStates();
                Toast.makeText(getContext(), "과목이 추가되었습니다 (" + semester + ")", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * 삭제 확인 다이얼로그 표시
     */
    private void showDeleteConfirmDialog(String categoryName, int position, CourseEditAdapter adapter) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("과목 삭제")
            .setMessage("이 과목을 삭제하시겠습니까?")
            .setPositiveButton("삭제", (dialog, which) -> {
                adapter.removeCourse(position);
                updateEmptyStates();
                Toast.makeText(getContext(), "과목이 삭제되었습니다", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * 학부공통/전공심화 타이틀 업데이트
     */
    private void updateDeptCommonTitle() {
        if (tvDeptCommonTitle != null) {
            tvDeptCommonTitle.setText(deptCommonCategoryName);
        }
        if (btnAddDeptCommon != null) {
            btnAddDeptCommon.setText("+ " + deptCommonCategoryName + " 추가");
        }
    }

    /**
     * 빈 상태 TextView 업데이트
     */
    private void updateEmptyStates() {
        // 전공필수
        if (adapterMajorRequired.getItemCount() == 0) {
            tvEmptyMajorRequired.setVisibility(View.VISIBLE);
            rvMajorRequired.setVisibility(View.GONE);
        } else {
            tvEmptyMajorRequired.setVisibility(View.GONE);
            rvMajorRequired.setVisibility(View.VISIBLE);
        }

        // 전공선택
        if (adapterMajorElective.getItemCount() == 0) {
            tvEmptyMajorElective.setVisibility(View.VISIBLE);
            rvMajorElective.setVisibility(View.GONE);
        } else {
            tvEmptyMajorElective.setVisibility(View.GONE);
            rvMajorElective.setVisibility(View.VISIBLE);
        }

        // 학부공통
        if (adapterDeptCommon.getItemCount() == 0) {
            tvEmptyDeptCommon.setVisibility(View.VISIBLE);
            rvDeptCommon.setVisibility(View.GONE);
        } else {
            tvEmptyDeptCommon.setVisibility(View.GONE);
            rvDeptCommon.setVisibility(View.VISIBLE);
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

        // 데이터 로딩 시작
        isDataLoading = true;

        // 현재 문서명 표시
        updateCurrentDocumentDisplay();

        // 문서 타입 확인 (학부공통 vs 전공심화)
        String categoryType = rules.getMajorCategoryType();
        if (categoryType != null) {
            deptCommonCategoryName = categoryType;
            updateDeptCommonTitle();
        }

        List<RequirementCategory> categories = rules.getCategories();

        if (categories != null) {
            for (RequirementCategory category : categories) {
                String categoryName = category.getName();
                List<CourseRequirement> courses = category.getCourses();

                if (courses != null) {
                    if ("전공필수".equals(categoryName)) {
                        adapterMajorRequired.setCourses(courses);
                    } else if ("전공선택".equals(categoryName)) {
                        adapterMajorElective.setCourses(courses);
                    } else if (deptCommonCategoryName.equals(categoryName)) {
                        adapterDeptCommon.setCourses(courses);
                    }
                }
            }
        }

        updateEmptyStates();

        // 데이터 로딩 완료
        isDataLoading = false;
    }

    /**
     * UI에서 데이터 추출하여 GraduationRules에 반영
     */
    public void updateGraduationRules(GraduationRules rules) {
        if (rules == null) {
            return;
        }

        // 전공 과목 데이터 수집
        List<CourseRequirement> majorRequiredCourses = adapterMajorRequired.getCourses();
        List<CourseRequirement> majorElectiveCourses = adapterMajorElective.getCourses();
        List<CourseRequirement> deptCommonCourses = adapterDeptCommon.getCourses();

        // 전공 과목 데이터가 하나도 없으면 기존 데이터를 건드리지 않음
        // (Firestore에서 불러오지 않은 경우)
        boolean hasAnyCourse = !majorRequiredCourses.isEmpty() ||
                               !majorElectiveCourses.isEmpty() ||
                               !deptCommonCourses.isEmpty();

        if (!hasAnyCourse) {
            android.util.Log.d(TAG, "updateGraduationRules: 전공 과목 데이터가 없어서 categories 업데이트 건너뜀");
            return;
        }

        List<RequirementCategory> categories = rules.getCategories();
        if (categories == null) {
            categories = new ArrayList<>();
            rules.setCategories(categories);
        }

        // 기존 전공 카테고리 제거 (학부공통과 전공심화 모두 제거)
        categories.removeIf(category -> {
            String name = category.getName();
            return "전공필수".equals(name) || "전공선택".equals(name) ||
                   "학부공통".equals(name) || "전공심화".equals(name);
        });

        // 전공필수 추가
        if (!majorRequiredCourses.isEmpty()) {
            RequirementCategory majorRequiredCategory = new RequirementCategory();
            majorRequiredCategory.setName("전공필수");
            majorRequiredCategory.setCourses(majorRequiredCourses);
            categories.add(majorRequiredCategory);
        }

        // 전공선택 추가
        if (!majorElectiveCourses.isEmpty()) {
            RequirementCategory majorElectiveCategory = new RequirementCategory();
            majorElectiveCategory.setName("전공선택");
            majorElectiveCategory.setCourses(majorElectiveCourses);
            categories.add(majorElectiveCategory);
        }

        // 학부공통 또는 전공심화 추가 (동적으로 처리)
        if (!deptCommonCourses.isEmpty()) {
            RequirementCategory deptCommonCategory = new RequirementCategory();
            deptCommonCategory.setName(deptCommonCategoryName);  // 동적 카테고리명
            deptCommonCategory.setCourses(deptCommonCourses);
            categories.add(deptCommonCategory);
        }

        android.util.Log.d(TAG, "updateGraduationRules: 전공 과목 업데이트 완료 - 총 " + categories.size() + "개 카테고리");
    }

    /**
     * 로드된 전공 문서명 반환 (관리자가 선택한 문서)
     */
    public String getLoadedDocumentName() {
        return loadedDocumentName;
    }
}
