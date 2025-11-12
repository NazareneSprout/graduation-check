package sprout.app.sakmvp1.timetable;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sprout.app.sakmvp1.R;

public class GroupListActivity extends AppCompatActivity implements GroupAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private GroupAdapter adapter;
    private List<Object> items = new ArrayList<>();

    private FirebaseFirestore db;
    private String currentUserId; // 내 ID
    private String currentCalendarId; // 현재 보고 있는 캘린더 ID

    public static final String RESULT_CALENDAR_ID = "RESULT_CALENDAR_ID";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_list);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // CommonCalendarActivity에서 현재 캘린더 ID를 받음
        currentCalendarId = getIntent().getStringExtra("CURRENT_CALENDAR_ID");
        if (currentCalendarId == null) {
            currentCalendarId = currentUserId; // 없으면 내 캘린더로 기본값
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("캘린더 선택");
        }

        recyclerView = findViewById(R.id.recycler_group_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // [수정] 어댑터 생성 시 currentUserId를 (myUserId로) 전달
        adapter = new GroupAdapter(items, currentCalendarId, currentUserId, this);
        recyclerView.setAdapter(adapter);

        loadMyGroups(); // 데이터 로드

        FloatingActionButton fab = findViewById(R.id.fab_add_group);
        fab.setOnClickListener(v -> showAddGroupOptionsDialog());
    }

    /**
     * [수정됨] "items.add(currentUserId)" 코드가 삭제되었습니다.
     */
    private void setupListData(List<GroupModel> groups) {
        items.clear();
        items.add("MY_CALENDAR");
        // items.add(currentUserId); // <-- 이 줄을 삭제함 (오류 원인)
        items.add("SEPARATOR");
        items.addAll(groups);
        adapter.notifyDataSetChanged();
    }

    // 내 그룹 목록 불러오기
    private void loadMyGroups() {
        db.collection("groups")
                .whereArrayContains("members", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("GroupListActivity", "Listen failed.", error);
                        return;
                    }
                    List<GroupModel> groups = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            GroupModel group = doc.toObject(GroupModel.class);
                            group.setDocumentId(doc.getId());
                            groups.add(group);
                        }
                    }
                    setupListData(groups); // 리스트 갱신
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 뒤로가기
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 캘린더 항목(내 캘린더, 그룹) 클릭 시
    @Override
    public void onItemClick(String calendarId) {
        // CommonCalendarActivity로 선택된 캘린더 ID를 돌려줌
        Intent resultIntent = new Intent();
        resultIntent.putExtra(RESULT_CALENDAR_ID, calendarId);
        setResult(RESULT_OK, resultIntent);
        finish(); // 이 화면 닫기
    }

    // (이하 그룹 생성/가입 다이얼로그 로직)

    private void showAddGroupOptionsDialog() {
        // '그룹 생성'과 '그룹 가입' 중 선택하는 다이얼로그
        String[] options = {"그룹 생성하기", "그룹 가입하기"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showCreateGroupDialog();
            } else {
                showJoinGroupDialog();
            }
        });
        builder.show();
    }

    private void showCreateGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_group, null);
        builder.setView(view);

        EditText editName = view.findViewById(R.id.edit_group_name);
        EditText editDesc = view.findViewById(R.id.edit_group_description);
        EditText editPw = view.findViewById(R.id.edit_group_password);

        builder.setPositiveButton("생성", null); // 버튼 리스너는 아래에서 오버라이드
        builder.setNegativeButton("취소", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveBtn.setOnClickListener(v -> {
                String name = editName.getText().toString().trim();
                String desc = editDesc.getText().toString().trim();
                String pw = editPw.getText().toString().trim();

                if (name.isEmpty() || desc.isEmpty() || pw.isEmpty()) {
                    Toast.makeText(this, "모든 항목을 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                createGroup(name, desc, pw, dialog); // 그룹 생성
            });
        });
        dialog.show();
    }

    private void showJoinGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_join_group, null);
        builder.setView(view);

        EditText editName = view.findViewById(R.id.edit_group_name);
        EditText editPw = view.findViewById(R.id.edit_group_password);

        builder.setPositiveButton("가입", (dialog, which) -> {
            String inputName = editName.getText().toString().trim();
            String inputPw = editPw.getText().toString().trim();

            if (inputName.isEmpty() || inputPw.isEmpty()) {
                Toast.makeText(this, "이름과 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }
            joinGroup(inputName, inputPw);
        });

        builder.setNegativeButton("취소", null);
        builder.show();
    }

    private void createGroup(String name, String desc, String password, AlertDialog dialog) {
        // 그룹 생성 시, 생성자인 나를 자동으로 멤버에 추가
        List<String> members = Collections.singletonList(currentUserId);
        GroupModel newGroup = new GroupModel(name, password, desc, members);

        db.collection("groups").add(newGroup)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "그룹 생성 완료!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    // (스냅샷 리스너가 있어서 자동 갱신됨)
                })
                .addOnFailureListener(e -> Toast.makeText(this, "생성 실패", Toast.LENGTH_SHORT).show());
    }

    private void joinGroup(String name, String password) {
        db.collection("groups")
                .whereEqualTo("groupName", name)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "해당 이름의 그룹이 없습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 이름이 일치하는 첫 번째 그룹을 찾음
                    QueryDocumentSnapshot doc = (QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);
                    GroupModel group = doc.toObject(GroupModel.class);

                    if (group.getPassword() != null && group.getPassword().equals(password)) {
                        // 비밀번호 일치
                        // 이미 가입했는지 확인
                        if (group.getMembers() != null && group.getMembers().contains(currentUserId)) {
                            Toast.makeText(this, "이미 가입한 그룹입니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // 멤버에 나를 추가
                        addMeToGroup(doc.getId());
                    } else {
                        Toast.makeText(this, "비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "오류 발생", Toast.LENGTH_SHORT).show());
    }

    private void addMeToGroup(String groupDocId) {
        db.collection("groups").document(groupDocId)
                .update("members", FieldValue.arrayUnion(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "가입 성공!", Toast.LENGTH_SHORT).show();
                    // (스냅샷 리스너가 있어서 자동 갱신됨)
                })
                .addOnFailureListener(e -> Toast.makeText(this, "가입 실패", Toast.LENGTH_SHORT).show());
    }
}