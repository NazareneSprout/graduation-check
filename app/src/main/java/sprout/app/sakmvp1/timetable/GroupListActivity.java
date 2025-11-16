package sprout.app.sakmvp1.timetable;

import android.app.AlertDialog;
import android.content.Intent;
// [삭제] 스와이프 관련 임포트 (Canvas, Color, Paint, ItemTouchHelper)
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu; // [추가] PopupMenu 임포트
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sprout.app.sakmvp1.BaseActivity;
import sprout.app.sakmvp1.R;

// [수정] 어댑터 리스너 인터페이스를 여기서 바로 구현
public class GroupListActivity extends BaseActivity implements GroupAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private GroupAdapter adapter;
    private List<Object> items = new ArrayList<>();

    private FirebaseFirestore db;
    private String currentUserId;
    private String currentCalendarId;

    // [추가] 뷰들을 멤버 변수로 선언 (모드 변경 시 제어하기 위해)
    private Toolbar toolbar;
    private FloatingActionButton fab;

    // [추가] 현재 모드 (일반/수정/삭제)
    private int currentMode = GroupAdapter.MODE_NORMAL;

    public static final String RESULT_CALENDAR_ID = "RESULT_CALENDAR_ID";

    // [삭제] 스와이프 Paint 객체 삭제

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

        currentCalendarId = getIntent().getStringExtra("CURRENT_CALENDAR_ID");
        if (currentCalendarId == null) {
            currentCalendarId = currentUserId;
        }

        // [수정] 뷰 초기화
        toolbar = findViewById(R.id.toolbar);
        fab = findViewById(R.id.fab_add_group);
        recyclerView = findViewById(R.id.recycler_group_list);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("캘린더 선택");
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // [수정] 어댑터 생성 시 'this'를 리스너로 전달
        adapter = new GroupAdapter(items, currentCalendarId, currentUserId, this);
        recyclerView.setAdapter(adapter);

        // [삭제] setupSwipeToDelete(); 호출 삭제

        loadMyGroups(); // 데이터 로드

        // [수정] FAB 클릭 시 모드에 따라 다르게 동작
        fab.setOnClickListener(v -> {
            if (currentMode != GroupAdapter.MODE_NORMAL) {
                // 수정/삭제 모드일 경우: 'X' 버튼이므로 일반 모드로 복귀
                exitMode();
            } else {
                // 일반 모드일 경우: '메뉴' 띄우기
                showFabPopupMenu(v); // v는 클릭된 FAB 자신 (앵커)
            }
        });
    }

    // [삭제] setupSwipeToDelete() 메서드 전체 삭제
    // [삭제] showLeaveGroupConfirmation() 메서드 삭제 (새로운 메서드로 대체)
    // [삭제] leaveGroup() 메서드 삭제 (새로운 메서드로 대체)

    // [수정] showAddGroupOptionsDialog -> showFabPopupMenu
    /**
     * FAB(+) 버튼 클릭 시, 앵커(anchorView)에 붙어 나오는 PopupMenu를 띄웁니다.
     */
    private void showFabPopupMenu(View anchorView) {
        PopupMenu popup = new PopupMenu(this, anchorView);

        // 메뉴 아이템 동적 추가
        popup.getMenu().add("그룹 생성");
        popup.getMenu().add("그룹 가입");
        popup.getMenu().add("그룹 수정");
        popup.getMenu().add("그룹 삭제");

        // 메뉴 클릭 리스너
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();

            if (title.equals("그룹 생성")) {
                showCreateGroupDialog();
                return true;
            } else if (title.equals("그룹 가입")) {
                showJoinGroupDialog();
                return true;
            } else if (title.equals("그룹 수정")) {
                enterEditMode(); // 수정 모드 진입
                return true;
            } else if (title.equals("그룹 삭제")) {
                enterDeleteMode(); // 삭제 모드 진입
                return true;
            }
            return false;
        });

        popup.show(); // 팝업 메뉴 표시
    }

    // [추가] 수정 모드 진입
    private void enterEditMode() {
        currentMode = GroupAdapter.MODE_EDIT;
        adapter.setMode(currentMode); // 어댑터에게 모드 변경 알림
        toolbar.setTitle("수정할 그룹 선택");
        fab.setImageResource(R.drawable.ic_close); // FAB 아이콘 'X'로 변경
    }

    // [추가] 삭제 모드 진입
    private void enterDeleteMode() {
        currentMode = GroupAdapter.MODE_DELETE;
        adapter.setMode(currentMode);
        toolbar.setTitle("삭제할 그룹 선택");
        fab.setImageResource(R.drawable.ic_close); // FAB 아이콘 'X'로 변경
    }

    // [추가] 수정/삭제 모드에서 빠져나오기 (일반 모드로 복귀)
    private void exitMode() {
        currentMode = GroupAdapter.MODE_NORMAL;
        adapter.setMode(currentMode);
        toolbar.setTitle("캘린더 선택");
        fab.setImageResource(R.drawable.ic_add); // FAB 아이콘 '+'로 복귀
    }

    // (기존 setupListData, loadMyGroups, onOptionsItemSelected는 동일)
    private void setupListData(List<GroupModel> groups) {
        items.clear();
        items.add("MY_CALENDAR");
        items.add("SEPARATOR");
        items.addAll(groups);
        adapter.notifyDataSetChanged();
    }
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
                    setupListData(groups);
                });
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // [수정] 모드 상태일 때 뒤로가기 누르면 모드만 취소
            if (currentMode != GroupAdapter.MODE_NORMAL) {
                exitMode();
                return true;
            }
            finish(); // 일반 모드면 액티비티 종료
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- [수정] 어댑터의 3가지 클릭 리스너 구현 ---

    @Override
    public void onCalendarClick(String calendarId) {
        // 일반 모드: 캘린더 선택
        Intent resultIntent = new Intent();
        resultIntent.putExtra(RESULT_CALENDAR_ID, calendarId);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onEditClick(GroupModel group) {
        // 수정 모드: 그룹 수정 다이얼로그 띄우기
        showEditGroupDialog(group);
    }

    @Override
    public void onDeleteClick(GroupModel group) {
        // 삭제 모드: 그룹 삭제/나가기 다이얼로그 띄우기
        showDeleteGroupConfirmation(group);
    }

    // --- [추가] 수정/삭제 관련 메서드 ---

    /**
     * [추가] 그룹 수정 다이얼로그
     */
    private void showEditGroupDialog(GroupModel group) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_group, null);
        builder.setView(view);

        EditText editName = view.findViewById(R.id.edit_group_name);
        EditText editDesc = view.findViewById(R.id.edit_group_description);
        EditText editPw = view.findViewById(R.id.edit_group_password);

        // 기존 정보 채우기
        editName.setText(group.getGroupName());
        editDesc.setText(group.getDescription());
        editPw.setText(group.getPassword());

        builder.setPositiveButton("수정", (dialog, which) -> {
            String name = editName.getText().toString().trim();
            String desc = editDesc.getText().toString().trim();
            String pw = editPw.getText().toString().trim();

            if (name.isEmpty() || desc.isEmpty() || pw.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }
            updateGroup(group, name, desc, pw);
        });
        builder.setNegativeButton("취소", (dialog, which) -> exitMode()); // 취소 시 모드 복귀
        builder.setOnCancelListener(dialog -> exitMode()); // 바깥 클릭 시 모드 복귀
        builder.show();
    }

    /**
     * [추가] 그룹 정보 Firestore 업데이트
     */
    private void updateGroup(GroupModel group, String name, String desc, String password) {
        db.collection("groups").document(group.getDocumentId())
                .update(
                        "groupName", name,
                        "description", desc,
                        "password", password
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "그룹 정보가 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    exitMode(); // 수정 완료 후 일반 모드로 복귀
                    // (스냅샷 리스너가 자동 갱신)
                })
                .addOnFailureListener(e -> Toast.makeText(this, "수정 실패", Toast.LENGTH_SHORT).show());
    }

    /**
     * [추가] 그룹 삭제/나가기 확인 다이얼로그
     */
    private void showDeleteGroupConfirmation(GroupModel group) {
        // 내가 마지막 멤버인지 확인
        boolean isLastMember = (group.getMembers() != null &&
                group.getMembers().size() == 1 &&
                group.getMembers().contains(currentUserId));

        String title = isLastMember ? "그룹 삭제" : "그룹 나가기";
        String message = isLastMember ? "마지막 멤버입니다. 그룹을 나가면 그룹과 모든 일정이 영구 삭제됩니다." :
                "그룹에서 나가시겠습니까? 그룹 캘린더가 목록에서 사라집니다.";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(isLastMember ? "삭제" : "나가기", (dialog, which) -> {
                    if (isLastMember) {
                        deleteGroup(group); // 그룹 자체를 삭제
                    } else {
                        leaveGroup(group); // 그룹에서 내 ID만 제거
                    }
                })
                .setNegativeButton("취소", (dialog, which) -> exitMode()) // 취소 시 모드 복귀
                .setOnCancelListener(dialog -> exitMode()) // 바깥 클릭 시 모드 복귀
                .show();
    }

    /**
     * [추가] 그룹에서 나가기 (내 ID만 제거)
     */
    private void leaveGroup(GroupModel group) {
        db.collection("groups").document(group.getDocumentId())
                .update("members", FieldValue.arrayRemove(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "그룹에서 나왔습니다.", Toast.LENGTH_SHORT).show();
                    if (currentCalendarId.equals(group.getDocumentId())) {
                        onCalendarClick(currentUserId); // 내 캘린더로 강제 전환
                    }
                    exitMode();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "나가기 실패", Toast.LENGTH_SHORT).show());
    }

    /**
     * [추가] 그룹 자체를 삭제 (마지막 멤버일 때)
     */
    private void deleteGroup(GroupModel group) {
        // 참고: Firestore의 하위 컬렉션(calendar_events)은 자동으로 삭제되지 않습니다.
        // (실제 서비스에서는 Cloud Function으로 삭제 처리가 필요합니다)
        db.collection("groups").document(group.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "그룹이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    if (currentCalendarId.equals(group.getDocumentId())) {
                        onCalendarClick(currentUserId); // 내 캘린더로 강제 전환
                    }
                    exitMode();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "삭제 실패", Toast.LENGTH_SHORT).show());
    }


    // (기존 '그룹 생성/가입' 관련 메서드들은 변경 없이 그대로 유지)

    private void showCreateGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_group, null);
        builder.setView(view);

        EditText editName = view.findViewById(R.id.edit_group_name);
        EditText editDesc = view.findViewById(R.id.edit_group_description);
        EditText editPw = view.findViewById(R.id.edit_group_password);

        builder.setPositiveButton("생성", null);
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
                createGroup(name, desc, pw, dialog);
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
        List<String> members = Collections.singletonList(currentUserId);
        GroupModel newGroup = new GroupModel(name, password, desc, members);

        db.collection("groups").add(newGroup)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "그룹 생성 완료!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
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

                    QueryDocumentSnapshot doc = (QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);
                    GroupModel group = doc.toObject(GroupModel.class);

                    if (group.getPassword() != null && group.getPassword().equals(password)) {
                        if (group.getMembers() != null && group.getMembers().contains(currentUserId)) {
                            Toast.makeText(this, "이미 가입한 그룹입니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }
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
                })
                .addOnFailureListener(e -> Toast.makeText(this, "가입 실패", Toast.LENGTH_SHORT).show());
    }
}