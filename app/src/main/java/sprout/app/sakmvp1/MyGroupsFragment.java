package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import sprout.app.sakmvp1.timetable.CommonCalendarActivity;

public class MyGroupsFragment extends Fragment {

    private RecyclerView recyclerMyGroups;
    private LinearLayout emptyState;
    private MaterialButton btnJoinGroup;
    private MaterialButton btnCreateGroup;
    private GroupAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_groups, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews(view);
        setupRecyclerView();
        loadMyGroups();

        return view;
    }

    private void initViews(View view) {
        recyclerMyGroups = view.findViewById(R.id.recycler_my_groups);
        emptyState = view.findViewById(R.id.empty_state);
        btnJoinGroup = view.findViewById(R.id.btn_join_group);
        btnCreateGroup = view.findViewById(R.id.btn_create_group);

        btnJoinGroup.setOnClickListener(v -> showJoinGroupDialog());
        btnCreateGroup.setOnClickListener(v -> showCreateGroupDialog());
    }

    private void setupRecyclerView() {
        adapter = new GroupAdapter();
        recyclerMyGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerMyGroups.setAdapter(adapter);
    }

    private void loadMyGroups() {
        if (mAuth.getCurrentUser() == null) {
            showEmptyState(true);
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        List<GroupItem> groups = new ArrayList<>();

        db.collection("groups")
                .whereArrayContains("members", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        GroupItem group = new GroupItem();
                        group.id = doc.getId();
                        group.name = doc.getString("name");
                        group.description = doc.getString("description");
                        group.code = doc.getString("code");

                        List<String> members = (List<String>) doc.get("members");
                        group.memberCount = members != null ? members.size() : 0;

                        groups.add(group);
                    }

                    adapter.setGroups(groups);
                    showEmptyState(groups.isEmpty());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "그룹 로드 실패", Toast.LENGTH_SHORT).show();
                    showEmptyState(true);
                });
    }

    private void showEmptyState(boolean show) {
        if (show) {
            recyclerMyGroups.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerMyGroups.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void showJoinGroupDialog() {
        View dialogView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);

        TextInputEditText etGroupCode = new TextInputEditText(requireContext());
        etGroupCode.setHint("그룹 코드 입력");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("그룹 참가하기")
                .setView(etGroupCode)
                .setPositiveButton("참가", (dialog, which) -> {
                    String code = etGroupCode.getText().toString().trim();

                    if (code.isEmpty()) {
                        Toast.makeText(requireContext(), "그룹 코드를 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    joinGroup(code);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showCreateGroupDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_group, null);

        TextInputEditText etGroupName = dialogView.findViewById(R.id.et_group_name);
        TextInputEditText etGroupDescription = dialogView.findViewById(R.id.et_group_description);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("그룹 생성")
                .setView(dialogView)
                .setPositiveButton("생성", (dialog, which) -> {
                    String name = etGroupName.getText().toString().trim();
                    String description = etGroupDescription.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "그룹 이름을 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createGroup(name, description);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void createGroup(String name, String description) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        String groupCode = generateGroupCode();

        Map<String, Object> group = new HashMap<>();
        group.put("name", name);
        group.put("description", description);
        group.put("code", groupCode);
        group.put("ownerId", userId);

        List<String> members = new ArrayList<>();
        members.add(userId);
        group.put("members", members);

        group.put("createdAt", new Date());

        db.collection("groups")
                .add(group)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(requireContext(), "그룹이 생성되었습니다", Toast.LENGTH_SHORT).show();
                    loadMyGroups();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "그룹 생성 실패", Toast.LENGTH_SHORT).show();
                });
    }

    private void joinGroup(String groupCode) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("groups")
                .whereEqualTo("code", groupCode.toUpperCase())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(requireContext(), "존재하지 않는 그룹 코드입니다", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        List<String> members = (List<String>) doc.get("members");
                        if (members != null && members.contains(userId)) {
                            Toast.makeText(requireContext(), "이미 참여 중인 그룹입니다", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        db.collection("groups").document(doc.getId())
                                .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(requireContext(), "그룹에 참가했습니다", Toast.LENGTH_SHORT).show();
                                    loadMyGroups();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(requireContext(), "그룹 참가 실패", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "그룹 검색 실패", Toast.LENGTH_SHORT).show();
                });
    }

    private String generateGroupCode() {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String numbers = "0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();

        // 영어 3자리 생성
        for (int i = 0; i < 3; i++) {
            code.append(letters.charAt(random.nextInt(letters.length())));
        }

        // 숫자 3자리 생성
        for (int i = 0; i < 3; i++) {
            code.append(numbers.charAt(random.nextInt(numbers.length())));
        }

        return code.toString();
    }

    // GroupItem 클래스
    private static class GroupItem {
        String id;
        String name;
        String description;
        String code;
        int memberCount;
    }

    // Adapter 클래스
    private class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {
        private List<GroupItem> groups = new ArrayList<>();

        void setGroups(List<GroupItem> groups) {
            this.groups = groups;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_group, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GroupItem group = groups.get(position);

            holder.tvGroupName.setText(group.name);
            holder.tvGroupCode.setText(group.code);

            holder.btnViewSchedule.setOnClickListener(v -> {
                // 그룹 일정 보기 (CommonCalendarActivity로 이동)
                Intent intent = new Intent(requireContext(), CommonCalendarActivity.class);
                intent.putExtra("groupId", group.id);
                startActivity(intent);
            });

            holder.btnLeaveGroup.setOnClickListener(v -> {
                showLeaveGroupDialog(group);
            });
        }

        @Override
        public int getItemCount() {
            return groups.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvGroupName;
            TextView tvGroupCode;
            MaterialButton btnViewSchedule;
            MaterialButton btnLeaveGroup;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvGroupName = itemView.findViewById(R.id.tv_group_name);
                tvGroupCode = itemView.findViewById(R.id.tv_group_code);
                btnViewSchedule = itemView.findViewById(R.id.btn_view_schedule);
                btnLeaveGroup = itemView.findViewById(R.id.btn_leave_group);
            }
        }
    }

    private void showLeaveGroupDialog(GroupItem group) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("그룹 나가기")
                .setMessage(group.name + " 그룹에서 나가시겠습니까?")
                .setPositiveButton("나가기", (dialog, which) -> {
                    leaveGroup(group.id);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void leaveGroup(String groupId) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("groups").document(groupId)
                .update("members", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "그룹에서 나갔습니다", Toast.LENGTH_SHORT).show();
                    loadMyGroups();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "그룹 나가기 실패", Toast.LENGTH_SHORT).show();
                });
    }
}
