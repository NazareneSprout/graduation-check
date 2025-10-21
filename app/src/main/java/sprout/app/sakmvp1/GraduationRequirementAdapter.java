package sprout.app.sakmvp1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 졸업요건 목록 RecyclerView Adapter
 */
public class GraduationRequirementAdapter extends RecyclerView.Adapter<GraduationRequirementAdapter.ViewHolder> {

    private List<GraduationRequirement> requirements = new ArrayList<>();
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;
    private OnSelectionChangedListener selectionChangedListener;

    // 삭제 모드 관련
    private boolean deleteMode = false;
    private Set<String> selectedIds = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClick(GraduationRequirement requirement);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(GraduationRequirement requirement);
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    public void setRequirements(List<GraduationRequirement> requirements) {
        this.requirements = requirements;
        notifyDataSetChanged();
    }

    /**
     * 삭제 모드 설정
     */
    public void setDeleteMode(boolean deleteMode) {
        this.deleteMode = deleteMode;
        if (!deleteMode) {
            // 삭제 모드 해제 시 선택 초기화
            selectedIds.clear();
        }
        notifyDataSetChanged();
    }

    /**
     * 삭제 모드 상태 반환
     */
    public boolean isDeleteMode() {
        return deleteMode;
    }

    /**
     * 선택된 항목 ID 목록 반환
     */
    public Set<String> getSelectedIds() {
        return new HashSet<>(selectedIds);
    }

    /**
     * 선택된 항목 개수 반환
     */
    public int getSelectedCount() {
        return selectedIds.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_graduation_requirement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GraduationRequirement requirement = requirements.get(position);
        holder.bind(requirement);
    }

    @Override
    public int getItemCount() {
        return requirements.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle, tvTrack, tvTotalCredits;
        private TextView tvMajorRequired, tvMajorElective, tvMajorAdvanced;
        private TextView tvGeneralRequired, tvGeneralElective;
        private TextView tvLiberalArts, tvFreeElective, tvDepartmentCommon, tvRemainingCredits;
        private TextView tvUpdatedAt;
        private android.widget.CheckBox cbSelect;

        // 부모 LinearLayout들
        private View layoutMajorAdvanced, layoutDepartmentCommon;
        private View layoutGeneralRequired, layoutGeneralElective;
        private View layoutLiberalArts, layoutFreeElective, layoutRemainingCredits;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvTrack = itemView.findViewById(R.id.tv_track);
            tvTotalCredits = itemView.findViewById(R.id.tv_total_credits);
            tvMajorRequired = itemView.findViewById(R.id.tv_major_required);
            tvMajorElective = itemView.findViewById(R.id.tv_major_elective);
            tvMajorAdvanced = itemView.findViewById(R.id.tv_major_advanced);
            tvGeneralRequired = itemView.findViewById(R.id.tv_general_required);
            tvGeneralElective = itemView.findViewById(R.id.tv_general_elective);
            tvLiberalArts = itemView.findViewById(R.id.tv_liberal_arts);
            tvFreeElective = itemView.findViewById(R.id.tv_free_elective);
            tvDepartmentCommon = itemView.findViewById(R.id.tv_department_common);
            tvRemainingCredits = itemView.findViewById(R.id.tv_remaining_credits);
            tvUpdatedAt = itemView.findViewById(R.id.tv_updated_at);
            cbSelect = itemView.findViewById(R.id.cb_select);

            // 부모 레이아웃 참조
            layoutMajorAdvanced = (View) tvMajorAdvanced.getParent();
            layoutDepartmentCommon = (View) tvDepartmentCommon.getParent();
            layoutGeneralRequired = (View) tvGeneralRequired.getParent();
            layoutGeneralElective = (View) tvGeneralElective.getParent();
            layoutLiberalArts = (View) tvLiberalArts.getParent();
            layoutFreeElective = (View) tvFreeElective.getParent();
            layoutRemainingCredits = (View) tvRemainingCredits.getParent();

            // 체크박스 클릭 리스너
            cbSelect.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    GraduationRequirement requirement = requirements.get(position);
                    String id = requirement.getId();
                    if (cbSelect.isChecked()) {
                        selectedIds.add(id);
                    } else {
                        selectedIds.remove(id);
                    }
                    // 선택 변경 알림
                    if (selectionChangedListener != null) {
                        selectionChangedListener.onSelectionChanged(selectedIds.size());
                    }
                }
            });

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    if (deleteMode) {
                        // 삭제 모드에서는 체크박스 토글
                        cbSelect.setChecked(!cbSelect.isChecked());
                    } else {
                        // 일반 모드에서는 편집 화면으로 이동
                        if (listener != null) {
                            listener.onItemClick(requirements.get(position));
                        }
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null && getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                    return longClickListener.onItemLongClick(requirements.get(getBindingAdapterPosition()));
                }
                return false;
            });
        }

        public void bind(GraduationRequirement requirement) {
            // 삭제 모드에 따라 체크박스 표시/숨김
            if (deleteMode) {
                cbSelect.setVisibility(View.VISIBLE);
                cbSelect.setChecked(selectedIds.contains(requirement.getId()));
            } else {
                cbSelect.setVisibility(View.GONE);
            }

            tvTitle.setText(requirement.getDisplayTitle());
            tvTrack.setText(requirement.getDisplayTrack());

            // 총이수학점은 Firestore에 저장된 값 사용
            int totalCredits = requirement.getTotalCredits();
            tvTotalCredits.setText(totalCredits + "학점");

            // 잔여학점/일반선택은 Firestore에 저장된 값 그대로 사용
            int remainingCredits = requirement.getRemainingCredits();
            int freeElective = requirement.getFreeElective();

            // 전공심화 또는 학부공통 (둘 중 하나만 표시, 부모 레이아웃도 함께 숨김)
            if (requirement.getMajorAdvanced() > 0) {
                layoutMajorAdvanced.setVisibility(View.VISIBLE);
                tvMajorAdvanced.setText(requirement.getMajorAdvanced() + "학점");
            } else {
                layoutMajorAdvanced.setVisibility(View.GONE);
            }

            if (requirement.getDepartmentCommon() > 0) {
                layoutDepartmentCommon.setVisibility(View.VISIBLE);
                tvDepartmentCommon.setText(requirement.getDepartmentCommon() + "학점");
            } else {
                layoutDepartmentCommon.setVisibility(View.GONE);
            }

            // 전공 필수/선택 (항상 표시)
            tvMajorRequired.setText(requirement.getMajorRequired() + "학점");
            tvMajorElective.setText(requirement.getMajorElective() + "학점");

            // 교양 필수/선택 (0이면 부모 레이아웃도 함께 숨김)
            if (requirement.getGeneralRequired() > 0) {
                layoutGeneralRequired.setVisibility(View.VISIBLE);
                tvGeneralRequired.setText(requirement.getGeneralRequired() + "학점");
            } else {
                layoutGeneralRequired.setVisibility(View.GONE);
            }

            if (requirement.getGeneralElective() > 0) {
                layoutGeneralElective.setVisibility(View.VISIBLE);
                tvGeneralElective.setText(requirement.getGeneralElective() + "학점");
            } else {
                layoutGeneralElective.setVisibility(View.GONE);
            }

            // 소양 (0이면 부모 레이아웃도 함께 숨김)
            if (requirement.getLiberalArts() > 0) {
                layoutLiberalArts.setVisibility(View.VISIBLE);
                tvLiberalArts.setText(requirement.getLiberalArts() + "학점");
            } else {
                layoutLiberalArts.setVisibility(View.GONE);
            }

            // 자율선택 또는 잔여학점 (둘 중 하나만 표시, Firestore 값 그대로 사용)
            if (freeElective > 0) {
                layoutFreeElective.setVisibility(View.VISIBLE);
                tvFreeElective.setText(freeElective + "학점");
                layoutRemainingCredits.setVisibility(View.GONE);
            } else if (remainingCredits > 0) {
                layoutFreeElective.setVisibility(View.GONE);
                layoutRemainingCredits.setVisibility(View.VISIBLE);
                tvRemainingCredits.setText(remainingCredits + "학점");
            } else {
                layoutFreeElective.setVisibility(View.GONE);
                layoutRemainingCredits.setVisibility(View.GONE);
            }

            // 참조 문서 ID만 표시 (문서 ID는 제외)
            StringBuilder displayText = new StringBuilder();
            if (requirement.getMajorDocRef() != null && !requirement.getMajorDocRef().isEmpty()) {
                displayText.append("전공문서: ").append(requirement.getMajorDocRef());
            }
            if (requirement.getGeneralDocRef() != null && !requirement.getGeneralDocRef().isEmpty()) {
                if (displayText.length() > 0) {
                    displayText.append("\n");
                }
                displayText.append("교양문서: ").append(requirement.getGeneralDocRef());
            }

            // 전공문서와 교양문서가 모두 없으면 "참조 문서 없음" 표시
            if (displayText.length() == 0) {
                displayText.append("참조 문서 없음");
            }

            tvUpdatedAt.setText(displayText.toString());
        }
    }
}
