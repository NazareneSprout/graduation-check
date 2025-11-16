package sprout.app.sakmvp1;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 배너 관리 화면 (관리자 전용)
 *
 * 기능:
 * - 배너 목록 조회 (우선순위 순)
 * - 배너 추가/수정/삭제
 * - 이미지 업로드
 * - 배너 타입 설정 (INTERNAL/EXTERNAL/NONE)
 * - 활성화 기간 설정
 * - 학부별 타겟팅
 */
public class BannerManagementActivity extends BaseActivity {

    private static final String TAG = "BannerManagement";
    private static final int PICK_IMAGE_REQUEST = 1001;

    private RecyclerView recyclerView;
    private BannerManagementAdapter adapter;
    private List<Banner> bannerList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private Uri selectedImageUri;
    private Banner editingBanner;
    private List<String> departmentList = new ArrayList<>();

    // 현재 열려있는 배너 편집 다이얼로그
    private AlertDialog currentBannerDialog;
    private ImageView currentPreviewImageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_banner_management);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initViews();
        loadDepartments();
        loadBanners();
    }

    private void initViews() {
        // 상단 툴바
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText("배너 관리");

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // 배너 추가 버튼
        Button btnAddBanner = findViewById(R.id.btn_add_banner);
        btnAddBanner.setOnClickListener(v -> showBannerDialog(null));

        // RecyclerView
        recyclerView = findViewById(R.id.recycler_banners);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BannerManagementAdapter();
        recyclerView.setAdapter(adapter);
    }

    /**
     * Firestore에서 학부 목록 로드
     */
    private void loadDepartments() {
        db.collection("graduation_meta").document("catalog")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> departments = (List<String>) documentSnapshot.get("departments");
                        if (departments != null && !departments.isEmpty()) {
                            departmentList.clear();
                            departmentList.add("ALL"); // 전체를 첫 번째 옵션으로 추가
                            departmentList.addAll(departments);
                            Log.d(TAG, "학부 로드 완료: " + departmentList.size() + "개");
                        } else {
                            // Firestore에 데이터가 없으면 기본값 사용
                            setDefaultDepartments();
                        }
                    } else {
                        // 문서가 없으면 기본값 사용
                        setDefaultDepartments();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "학부 로드 실패", e);
                    // 실패 시 기본값 사용
                    setDefaultDepartments();
                });
    }

    /**
     * 기본 학부 목록 설정 (Firestore 로드 실패 시)
     */
    private void setDefaultDepartments() {
        departmentList.clear();
        departmentList.add("ALL");
        departmentList.add("컴퓨터공학부");
        departmentList.add("전기전자공학부");
        departmentList.add("기계공학부");
        departmentList.add("화학공학부");
        Log.d(TAG, "기본 학부 목록 사용");
    }

    /**
     * Firestore에서 배너 목록 로드
     */
    private void loadBanners() {
        db.collection("banners")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    bannerList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Banner banner = doc.toObject(Banner.class);
                        banner.setId(doc.getId());
                        bannerList.add(banner);
                    }

                    // 우선순위 순으로 정렬
                    bannerList.sort(Comparator.comparingInt(Banner::getPriority));

                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "배너 로드 완료: " + bannerList.size() + "개");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "배너 로드 실패", e);
                    Toast.makeText(this, "배너 로드 실패", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 배너 추가/수정 다이얼로그
     */
    private void showBannerDialog(@Nullable Banner banner) {
        editingBanner = banner;
        selectedImageUri = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_banner_edit, null);

        // UI 요소
        ImageView ivPreview = dialogView.findViewById(R.id.iv_banner_preview);
        currentPreviewImageView = ivPreview; // 미리보기 이미지뷰 저장
        Button btnSelectImage = dialogView.findViewById(R.id.btn_select_image);
        EditText etTitle = dialogView.findViewById(R.id.et_banner_title);
        EditText etPriority = dialogView.findViewById(R.id.et_priority);
        CheckBox cbActive = dialogView.findViewById(R.id.cb_active);

        RadioGroup rgType = dialogView.findViewById(R.id.rg_banner_type);
        LinearLayout layoutExternal = dialogView.findViewById(R.id.layout_external_target);

        EditText etTargetUrl = dialogView.findViewById(R.id.et_target_url);

        Spinner spinnerDepartment = dialogView.findViewById(R.id.spinner_department);
        TextView tvStartDate = dialogView.findViewById(R.id.tv_start_date);
        TextView tvEndDate = dialogView.findViewById(R.id.tv_end_date);
        Button btnSetStartDate = dialogView.findViewById(R.id.btn_set_start_date);
        Button btnSetEndDate = dialogView.findViewById(R.id.btn_set_end_date);
        Button btnClearStartDate = dialogView.findViewById(R.id.btn_clear_start_date);
        Button btnClearEndDate = dialogView.findViewById(R.id.btn_clear_end_date);

        // 학부 Spinner 설정 (Firestore에서 로드된 목록 사용)
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                departmentList);
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(deptAdapter);

        // 배너 타입에 따른 UI 표시/숨김
        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_type_external) {
                layoutExternal.setVisibility(View.VISIBLE);
            } else {
                layoutExternal.setVisibility(View.GONE);
            }
        });

        // 기존 배너 데이터 로드
        if (banner != null) {
            etTitle.setText(banner.getTitle());
            etPriority.setText(String.valueOf(banner.getPriority()));
            cbActive.setChecked(banner.isActive());

            if (banner.getImageUrl() != null) {
                Glide.with(this).load(banner.getImageUrl()).into(ivPreview);
            }

            // 타입 설정
            String type = banner.getType() != null ? banner.getType() : "EXTERNAL";
            if ("EXTERNAL".equals(type)) {
                rgType.check(R.id.rb_type_external);
                etTargetUrl.setText(banner.getTargetUrl());
            } else {
                // NONE 또는 기타
                rgType.check(R.id.rb_type_none);
            }

            // 학부 설정
            if (banner.getTargetDepartment() != null) {
                int deptPos = ((ArrayAdapter<String>) spinnerDepartment.getAdapter())
                        .getPosition(banner.getTargetDepartment());
                if (deptPos >= 0) spinnerDepartment.setSelection(deptPos);
            }

            // 날짜 설정
            if (banner.getStartDate() > 0) {
                tvStartDate.setText(formatDate(banner.getStartDate()));
            }
            if (banner.getEndDate() > 0) {
                tvEndDate.setText(formatDate(banner.getEndDate()));
            }
        } else {
            // 새 배너 기본값
            etPriority.setText("99");
            cbActive.setChecked(true);
            rgType.check(R.id.rb_type_external);
            spinnerDepartment.setSelection(0); // ALL
        }

        // 이미지 선택 버튼
        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        // 날짜 설정 버튼
        final long[] startDateMillis = {banner != null ? banner.getStartDate() : 0};
        final long[] endDateMillis = {banner != null ? banner.getEndDate() : 0};

        btnSetStartDate.setOnClickListener(v -> {
            showDatePicker(date -> {
                startDateMillis[0] = date;
                tvStartDate.setText(formatDate(date));
            }, startDateMillis[0]);
        });

        btnSetEndDate.setOnClickListener(v -> {
            showDatePicker(date -> {
                endDateMillis[0] = date;
                tvEndDate.setText(formatDate(date));
            }, endDateMillis[0]);
        });

        btnClearStartDate.setOnClickListener(v -> {
            startDateMillis[0] = 0;
            tvStartDate.setText("제한 없음");
        });

        btnClearEndDate.setOnClickListener(v -> {
            endDateMillis[0] = 0;
            tvEndDate.setText("제한 없음");
        });

        // 다이얼로그 생성
        builder.setView(dialogView)
                .setTitle(banner == null ? "배너 추가" : "배너 수정")
                .setPositiveButton("저장", null)
                .setNegativeButton("취소", (dialog, which) -> {
                    currentBannerDialog = null;
                    currentPreviewImageView = null;
                    dialog.dismiss();
                });

        AlertDialog dialog = builder.create();
        currentBannerDialog = dialog; // 현재 다이얼로그 저장

        // 다이얼로그 닫힐 때 정리
        dialog.setOnDismissListener(d -> {
            currentBannerDialog = null;
            currentPreviewImageView = null;
        });

        dialog.show();

        // 저장 버튼 클릭 처리 (validation 후 저장)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String priorityStr = etPriority.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(this, "배너 제목을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            int priority = 99;
            try {
                priority = Integer.parseInt(priorityStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "우선순위는 숫자여야 합니다", Toast.LENGTH_SHORT).show();
                return;
            }

            // 이미지 확인
            if (banner == null && selectedImageUri == null) {
                Toast.makeText(this, "배너 이미지를 선택하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 타입별 validation
            int selectedTypeId = rgType.getCheckedRadioButtonId();
            String type;
            String targetUrl = null;

            if (selectedTypeId == R.id.rb_type_external) {
                type = "EXTERNAL";
                targetUrl = etTargetUrl.getText().toString().trim();
                if (targetUrl.isEmpty()) {
                    Toast.makeText(this, "외부 링크 URL을 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                type = "NONE";
            }

            boolean active = cbActive.isChecked();
            String department = spinnerDepartment.getSelectedItem().toString();

            // 배너 객체 생성/수정
            Banner bannerToSave = banner != null ? banner : new Banner();
            bannerToSave.setTitle(title);
            bannerToSave.setPriority(priority);
            bannerToSave.setActive(active);
            bannerToSave.setType(type);
            bannerToSave.setTargetScreen(null); // INTERNAL 타입 제거로 항상 null
            bannerToSave.setTargetUrl(targetUrl);
            bannerToSave.setTargetDepartment(department);
            bannerToSave.setStartDate(startDateMillis[0]);
            bannerToSave.setEndDate(endDateMillis[0]);

            // 이미지 업로드 후 저장
            if (selectedImageUri != null) {
                uploadImageAndSaveBanner(bannerToSave, dialog);
            } else {
                saveBanner(bannerToSave, dialog);
            }
        });
    }

    /**
     * 날짜 선택 다이얼로그
     */
    private void showDatePicker(DateSelectedListener listener, long currentMillis) {
        Calendar calendar = Calendar.getInstance();
        if (currentMillis > 0) {
            calendar.setTimeInMillis(currentMillis);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    listener.onDateSelected(selected.getTimeInMillis());
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    interface DateSelectedListener {
        void onDateSelected(long millis);
    }

    /**
     * 이미지 업로드 후 배너 저장
     */
    private void uploadImageAndSaveBanner(Banner banner, AlertDialog dialog) {
        if (selectedImageUri == null) {
            Log.e(TAG, "선택된 이미지가 없습니다");
            Toast.makeText(this, "이미지를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "banner_" + UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = storage.getReference().child("banners/" + fileName);

        Log.d(TAG, "이미지 업로드 시작: " + selectedImageUri);
        Toast.makeText(this, "이미지 업로드 중...", Toast.LENGTH_SHORT).show();

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "이미지 업로드 성공, 다운로드 URL 가져오는 중...");
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        Log.d(TAG, "다운로드 URL 획득 성공: " + uri.toString());
                        banner.setImageUrl(uri.toString());
                        saveBanner(banner, dialog);
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "다운로드 URL 획득 실패", e);
                        Toast.makeText(this, "이미지 URL 획득 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "이미지 업로드 실패: " + e.getClass().getSimpleName(), e);
                    Toast.makeText(this, "이미지 업로드 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * 배너 저장 (Firestore)
     */
    private void saveBanner(Banner banner, AlertDialog dialog) {
        Task<Void> task;

        if (banner.getId() == null) {
            // 새 배너 추가
            String newId = db.collection("banners").document().getId();
            banner.setId(newId);
            task = db.collection("banners").document(newId).set(banner);
        } else {
            // 기존 배너 수정
            task = db.collection("banners").document(banner.getId()).set(banner);
        }

        task.addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "배너 저장 완료", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            loadBanners();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "배너 저장 실패", e);
            Toast.makeText(this, "배너 저장 실패", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 배너 삭제
     */
    private void deleteBanner(Banner banner) {
        new AlertDialog.Builder(this)
                .setTitle("배너 삭제")
                .setMessage("'" + banner.getTitle() + "' 배너를 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    db.collection("banners").document(banner.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "배너 삭제 완료", Toast.LENGTH_SHORT).show();
                                loadBanners();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "배너 삭제 실패", e);
                                Toast.makeText(this, "배너 삭제 실패", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("취소", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();

            // URI 영구 권한 요청 (Firebase Storage 업로드를 위해 필요)
            try {
                getContentResolver().takePersistableUriPermission(
                        selectedImageUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (SecurityException e) {
                Log.w(TAG, "영구 권한 획득 실패 (일시 권한 사용)", e);
            }

            // 미리보기 표시
            if (currentPreviewImageView != null) {
                Glide.with(this)
                        .load(selectedImageUri)
                        .into(currentPreviewImageView);
                Log.d(TAG, "이미지 미리보기 로드 완료: " + selectedImageUri);
            } else {
                Log.w(TAG, "미리보기 ImageView를 찾을 수 없습니다");
            }
        }
    }

    /**
     * 날짜 포맷 (yyyy-MM-dd)
     */
    private String formatDate(long millis) {
        if (millis == 0) return "제한 없음";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        return sdf.format(new Date(millis));
    }

    // =================================================================================
    // RecyclerView Adapter
    // =================================================================================

    private class BannerManagementAdapter extends RecyclerView.Adapter<BannerManagementAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_banner_management, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Banner banner = bannerList.get(position);

            // 배너 이미지
            Glide.with(holder.itemView.getContext())
                    .load(banner.getImageUrl())
                    .into(holder.ivBanner);

            // 배너 정보
            holder.tvTitle.setText(banner.getTitle());
            holder.tvPriority.setText("우선순위: " + banner.getPriority());

            String typeDisplay = BannerRouter.getTypeDisplayName(banner.getType());
            if ("INTERNAL".equals(banner.getType())) {
                typeDisplay += " - " + BannerRouter.getTargetScreenDisplayName(banner.getTargetScreen());
            }
            holder.tvType.setText(typeDisplay);

            // 실제 활성화 상태 확인 (체크박스 + 활성화 기간)
            boolean isReallyActive = banner.isActive() && banner.isInActivePeriod();
            String statusText;
            int statusColor;

            if (!banner.isActive()) {
                statusText = "비활성화 (수동)";
                statusColor = getResources().getColor(android.R.color.darker_gray);
            } else if (!banner.isInActivePeriod()) {
                statusText = "비활성화 (기간)";
                statusColor = getResources().getColor(android.R.color.holo_orange_dark);
            } else {
                statusText = "활성화";
                statusColor = getResources().getColor(android.R.color.holo_green_dark);
            }

            holder.tvStatus.setText(statusText);
            holder.tvStatus.setTextColor(statusColor);

            // 활성화 기간
            String period = "기간: ";
            if (banner.getStartDate() > 0) {
                period += formatDate(banner.getStartDate());
            } else {
                period += "제한없음";
            }
            period += " ~ ";
            if (banner.getEndDate() > 0) {
                period += formatDate(banner.getEndDate());
            } else {
                period += "제한없음";
            }
            holder.tvPeriod.setText(period);

            // 대상 학부
            holder.tvDepartment.setText("대상: " + banner.getTargetDepartment());

            // 수정 버튼
            holder.btnEdit.setOnClickListener(v -> showBannerDialog(banner));

            // 삭제 버튼
            holder.btnDelete.setOnClickListener(v -> deleteBanner(banner));
        }

        @Override
        public int getItemCount() {
            return bannerList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivBanner;
            TextView tvTitle;
            TextView tvPriority;
            TextView tvType;
            TextView tvStatus;
            TextView tvPeriod;
            TextView tvDepartment;
            Button btnEdit;
            Button btnDelete;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivBanner = itemView.findViewById(R.id.iv_banner);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvPriority = itemView.findViewById(R.id.tv_priority);
                tvType = itemView.findViewById(R.id.tv_type);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvPeriod = itemView.findViewById(R.id.tv_period);
                tvDepartment = itemView.findViewById(R.id.tv_department);
                btnEdit = itemView.findViewById(R.id.btn_edit);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }
}
