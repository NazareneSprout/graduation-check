package sprout.app.sakmvp1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GroupScheduleFragment extends Fragment {

    private Spinner spinnerGroups;
    private RecyclerView recyclerCommonCalendar;
    private LinearLayout emptyState;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private List<GroupInfo> groupList = new ArrayList<>();
    private ArrayAdapter<String> groupAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_schedule, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews(view);
        setupSpinner();
        loadGroups();

        return view;
    }

    private void initViews(View view) {
        spinnerGroups = view.findViewById(R.id.spinner_groups);
        recyclerCommonCalendar = view.findViewById(R.id.recycler_common_calendar);
        emptyState = view.findViewById(R.id.empty_state);

        recyclerCommonCalendar.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void setupSpinner() {
        List<String> groupNames = new ArrayList<>();
        groupNames.add("그룹을 선택하세요");

        groupAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, groupNames);
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGroups.setAdapter(groupAdapter);

        spinnerGroups.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    GroupInfo selectedGroup = groupList.get(position - 1);
                    loadGroupSchedule(selectedGroup.id);
                } else {
                    showEmptyState(true);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void loadGroups() {
        if (mAuth.getCurrentUser() == null) {
            showEmptyState(true);
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        groupList.clear();

        db.collection("groups")
                .whereArrayContains("members", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> groupNames = new ArrayList<>();
                    groupNames.add("그룹을 선택하세요");

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        GroupInfo group = new GroupInfo();
                        group.id = doc.getId();
                        group.name = doc.getString("name");
                        groupList.add(group);
                        groupNames.add(group.name);
                    }

                    groupAdapter.clear();
                    groupAdapter.addAll(groupNames);
                    groupAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "그룹 로드 실패", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadGroupSchedule(String groupId) {
        // 여기서는 간단히 빈 상태를 숨기고, 실제로는 CommonCalendarActivity의 로직을 사용
        showEmptyState(false);

        // TODO: CommonCalendarActivity의 캘린더 로직을 여기에 통합
        // 현재는 기본 메시지 표시
        Toast.makeText(requireContext(), "그룹 일정 로드 중...", Toast.LENGTH_SHORT).show();
    }

    private void showEmptyState(boolean show) {
        if (show) {
            recyclerCommonCalendar.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerCommonCalendar.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    // GroupInfo 클래스
    private static class GroupInfo {
        String id;
        String name;
    }
}
