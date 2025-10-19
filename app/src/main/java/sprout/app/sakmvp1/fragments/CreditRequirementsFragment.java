package sprout.app.sakmvp1.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import sprout.app.sakmvp1.GraduationRequirementEditActivity;
import sprout.app.sakmvp1.R;
import sprout.app.sakmvp1.models.CreditRequirements;
import sprout.app.sakmvp1.models.GraduationRules;

/**
 * 학점요건 편집 Fragment
 * 총 이수학점, 전공학점, 교양학점 등을 입력
 */
public class CreditRequirementsFragment extends Fragment {

    private static final String TAG = "CreditRequirements";

    private TextView tvTotalCredits;
    private TextInputEditText etMajorRequired;
    private TextInputEditText etMajorElective;
    private TextInputEditText etDepartmentCommon;
    private TextInputEditText etGeneralRequired;
    private TextInputEditText etGeneralElective;
    private TextInputEditText etLiberalArts;
    private TextInputEditText etRemainingCredits;

    private GraduationRules graduationRules;
    private boolean isDataLoading = false;  // 데이터 로딩 중인지 플래그

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_credit_requirements, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupTextWatchers();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume 호출");
        // Activity로부터 데이터 가져와서 바인딩
        if (getActivity() instanceof GraduationRequirementEditActivity) {
            GraduationRules rules = ((GraduationRequirementEditActivity) getActivity()).getGraduationRules();
            Log.d(TAG, "Activity로부터 데이터 가져옴: " + (rules != null ? "존재" : "null"));
            if (rules != null) {
                bindData(rules);
            }
        }
    }

    private void initViews(View view) {
        tvTotalCredits = view.findViewById(R.id.tv_total_credits);
        etMajorRequired = view.findViewById(R.id.et_major_required);
        etMajorElective = view.findViewById(R.id.et_major_elective);
        etDepartmentCommon = view.findViewById(R.id.et_department_common);
        etGeneralRequired = view.findViewById(R.id.et_general_required);
        etGeneralElective = view.findViewById(R.id.et_general_elective);
        etLiberalArts = view.findViewById(R.id.et_liberal_arts);
        etRemainingCredits = view.findViewById(R.id.et_remaining_credits);
    }

    /**
     * 입력 필드에 TextWatcher 설정하여 총 이수학점 자동 계산
     */
    private void setupTextWatchers() {
        TextWatcher creditWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateTotalCredits();
                // 데이터 로딩 중이 아닐 때만 변경사항으로 표시
                if (!isDataLoading && getActivity() instanceof GraduationRequirementEditActivity) {
                    ((GraduationRequirementEditActivity) getActivity()).markAsModified();
                }
            }
        };

        if (etMajorRequired != null) etMajorRequired.addTextChangedListener(creditWatcher);
        if (etMajorElective != null) etMajorElective.addTextChangedListener(creditWatcher);
        if (etDepartmentCommon != null) etDepartmentCommon.addTextChangedListener(creditWatcher);
        if (etGeneralRequired != null) etGeneralRequired.addTextChangedListener(creditWatcher);
        if (etGeneralElective != null) etGeneralElective.addTextChangedListener(creditWatcher);
        if (etLiberalArts != null) etLiberalArts.addTextChangedListener(creditWatcher);
        if (etRemainingCredits != null) etRemainingCredits.addTextChangedListener(creditWatcher);
    }

    /**
     * 총 이수학점 계산 및 표시 (모든 학점의 합)
     * - 학부공통과 전공심화는 하나의 필드(departmentCommon)로 관리됨
     * - 자율선택과 잔여학점도 하나의 필드(remainingCredits)로 관리됨
     */
    private void updateTotalCredits() {
        if (tvTotalCredits == null) return;

        try {
            int majorRequired = getIntValue(etMajorRequired);
            int majorElective = getIntValue(etMajorElective);
            int departmentCommonOrAdvanced = getIntValue(etDepartmentCommon);  // 학부공통 또는 전공심화
            int generalRequired = getIntValue(etGeneralRequired);
            int generalElective = getIntValue(etGeneralElective);
            int liberalArts = getIntValue(etLiberalArts);
            int freeElectiveOrRemaining = getIntValue(etRemainingCredits);  // 자율선택 또는 잔여학점

            int total = majorRequired + majorElective + departmentCommonOrAdvanced +
                        generalRequired + generalElective + liberalArts + freeElectiveOrRemaining;

            tvTotalCredits.setText("총 이수학점: " + total + "학점");
        } catch (Exception e) {
            tvTotalCredits.setText("총 이수학점: 0학점");
        }
    }

    /**
     * EditText에서 int 값 추출 (빈 값이면 0 반환)
     */
    private int getIntValue(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) return 0;
        String text = editText.getText().toString().trim();
        if (text.isEmpty()) return 0;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * GraduationRules 데이터를 UI에 바인딩
     */
    public void bindData(GraduationRules rules) {
        Log.d(TAG, "bindData 호출");
        if (rules == null) {
            Log.w(TAG, "bindData: rules가 null입니다");
            return;
        }

        this.graduationRules = rules;
        CreditRequirements creditReqs = rules.getCreditRequirements();

        // 데이터 로딩 시작 (TextWatcher가 변경사항으로 감지하지 않도록)
        isDataLoading = true;

        if (creditReqs != null) {
            Log.d(TAG, "CreditRequirements 데이터 바인딩 시작");

            // 전공 학점
            if (etMajorRequired != null) {
                etMajorRequired.setText(String.valueOf(creditReqs.get전공필수()));
            }
            if (etMajorElective != null) {
                etMajorElective.setText(String.valueOf(creditReqs.get전공선택()));
            }
            // 학부공통 또는 전공심화 (둘 중 하나만 0이 아님)
            if (etDepartmentCommon != null) {
                if (creditReqs.get전공심화() > 0) {
                    etDepartmentCommon.setText(String.valueOf(creditReqs.get전공심화()));
                } else {
                    etDepartmentCommon.setText(String.valueOf(creditReqs.get학부공통()));
                }
            }

            // 교양 학점
            if (etGeneralRequired != null) {
                etGeneralRequired.setText(String.valueOf(creditReqs.get교양필수()));
            }
            if (etGeneralElective != null) {
                etGeneralElective.setText(String.valueOf(creditReqs.get교양선택()));
            }
            if (etLiberalArts != null) {
                etLiberalArts.setText(String.valueOf(creditReqs.get소양()));
            }

            // 자율선택 또는 잔여학점 (둘 중 하나만 0이 아님)
            if (etRemainingCredits != null) {
                if (creditReqs.get일반선택() > 0) {
                    etRemainingCredits.setText(String.valueOf(creditReqs.get일반선택()));
                } else {
                    etRemainingCredits.setText(String.valueOf(creditReqs.get잔여학점()));
                }
            }

            // 총 이수학점 업데이트 (자동 계산)
            updateTotalCredits();

            Log.d(TAG, "CreditRequirements 데이터 바인딩 완료");
        } else {
            Log.w(TAG, "CreditRequirements가 null입니다");
        }

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

        CreditRequirements creditReqs = rules.getCreditRequirements();
        if (creditReqs == null) {
            creditReqs = new CreditRequirements();
            rules.setCreditRequirements(creditReqs);
        }

        // 전공 학점
        if (etMajorRequired != null && etMajorRequired.getText() != null) {
            try {
                int value = Integer.parseInt(etMajorRequired.getText().toString().trim());
                creditReqs.set전공필수(value);
            } catch (NumberFormatException e) {
                creditReqs.set전공필수(0);
            }
        }

        if (etMajorElective != null && etMajorElective.getText() != null) {
            try {
                int value = Integer.parseInt(etMajorElective.getText().toString().trim());
                creditReqs.set전공선택(value);
            } catch (NumberFormatException e) {
                creditReqs.set전공선택(0);
            }
        }

        // 학부공통 또는 전공심화 (기존에 어느 것이 사용되었는지 확인)
        if (etDepartmentCommon != null && etDepartmentCommon.getText() != null) {
            try {
                int value = Integer.parseInt(etDepartmentCommon.getText().toString().trim());
                // 기존 데이터가 전공심화를 사용했는지 확인
                if (creditReqs.get전공심화() > 0 || creditReqs.get학부공통() == 0) {
                    creditReqs.set전공심화(value);
                    creditReqs.set학부공통(0);  // 반대쪽은 0으로 설정
                } else {
                    creditReqs.set학부공통(value);
                    creditReqs.set전공심화(0);  // 반대쪽은 0으로 설정
                }
            } catch (NumberFormatException e) {
                creditReqs.set학부공통(0);
                creditReqs.set전공심화(0);
            }
        }

        // 교양 학점
        if (etGeneralRequired != null && etGeneralRequired.getText() != null) {
            try {
                int value = Integer.parseInt(etGeneralRequired.getText().toString().trim());
                creditReqs.set교양필수(value);
            } catch (NumberFormatException e) {
                creditReqs.set교양필수(0);
            }
        }

        if (etGeneralElective != null && etGeneralElective.getText() != null) {
            try {
                int value = Integer.parseInt(etGeneralElective.getText().toString().trim());
                creditReqs.set교양선택(value);
            } catch (NumberFormatException e) {
                creditReqs.set교양선택(0);
            }
        }

        if (etLiberalArts != null && etLiberalArts.getText() != null) {
            try {
                int value = Integer.parseInt(etLiberalArts.getText().toString().trim());
                creditReqs.set소양(value);
            } catch (NumberFormatException e) {
                creditReqs.set소양(0);
            }
        }

        // 자율선택 또는 잔여학점 (기존에 어느 것이 사용되었는지 확인)
        if (etRemainingCredits != null && etRemainingCredits.getText() != null) {
            try {
                int value = Integer.parseInt(etRemainingCredits.getText().toString().trim());
                // 기존 데이터가 자율선택을 사용했는지 확인
                if (creditReqs.get일반선택() > 0 || creditReqs.get잔여학점() == 0) {
                    creditReqs.set일반선택(value);
                    creditReqs.set잔여학점(0);  // 반대쪽은 0으로 설정
                } else {
                    creditReqs.set잔여학점(value);
                    creditReqs.set일반선택(0);  // 반대쪽은 0으로 설정
                }
            } catch (NumberFormatException e) {
                creditReqs.set잔여학점(0);
                creditReqs.set일반선택(0);
            }
        }

        // 총 이수학점 (자동 계산된 값 저장)
        int total = creditReqs.get전공필수() + creditReqs.get전공선택() + creditReqs.get학부공통() +
                    creditReqs.get전공심화() + creditReqs.get교양필수() + creditReqs.get교양선택() +
                    creditReqs.get소양() + creditReqs.get일반선택() + creditReqs.get잔여학점();
        creditReqs.setTotal(total);
    }
}
