package sprout.app.sakmvp1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * 캠퍼스 지도 Fragment
 * 학교 시설 위치를 지도에 표시하는 기능
 * 라디오 버튼 선택 시 해당 시설 오버레이 이미지를 표시
 */
public class CampusMapFragment extends Fragment {

    private FrameLayout mapContainer;
    private ImageView ivCampusMap;

    // 오버레이 이미지들
    private ImageView ivOverlayCopier;
    private ImageView ivOverlayCafeteria;
    private ImageView ivOverlayLibrary;
    private ImageView ivOverlayAtm;
    private ImageView ivOverlayDocumentPrinter;
    private ImageView ivOverlayEnglishCafe;

    // 라디오 버튼 그룹
    private RadioGroup radioGroupFacilities;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_campus_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRadioButtonListener();
    }

    private void initViews(View view) {
        // 지도 컨테이너
        mapContainer = view.findViewById(R.id.map_container);

        // 기본 지도 이미지
        ivCampusMap = view.findViewById(R.id.iv_campus_map);
        ivCampusMap.setImageResource(R.drawable.campus_map);

        // 오버레이 이미지들
        ivOverlayCopier = view.findViewById(R.id.iv_overlay_copier);
        ivOverlayCafeteria = view.findViewById(R.id.iv_overlay_cafeteria);
        ivOverlayLibrary = view.findViewById(R.id.iv_overlay_library);
        ivOverlayAtm = view.findViewById(R.id.iv_overlay_atm);
        ivOverlayDocumentPrinter = view.findViewById(R.id.iv_overlay_document_printer);
        ivOverlayEnglishCafe = view.findViewById(R.id.iv_overlay_english_cafe);

        // 오버레이 이미지 리소스 설정
        ivOverlayCopier.setImageResource(R.drawable.campus_map_copier);
        ivOverlayCafeteria.setImageResource(R.drawable.campus_map_cafeteria);
        ivOverlayLibrary.setImageResource(R.drawable.campus_map_library);
        ivOverlayAtm.setImageResource(R.drawable.campus_map_atm);
        ivOverlayDocumentPrinter.setImageResource(R.drawable.campus_map_document_printer);
        ivOverlayEnglishCafe.setImageResource(R.drawable.campus_map_english_cafe);

        // 라디오 버튼 그룹
        radioGroupFacilities = view.findViewById(R.id.radio_group_facilities);

        // 지도 클릭 시 확대 화면으로 이동
        mapContainer.setOnClickListener(v -> openImageZoom());
    }

    /**
     * 라디오 버튼 리스너 설정
     */
    private void setupRadioButtonListener() {
        radioGroupFacilities.setOnCheckedChangeListener((group, checkedId) -> {
            // 모든 오버레이 숨김
            hideAllOverlays();

            // 선택된 라디오 버튼에 따라 해당 오버레이만 표시
            if (checkedId == R.id.rb_copier) {
                ivOverlayCopier.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.rb_cafeteria) {
                ivOverlayCafeteria.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.rb_library) {
                ivOverlayLibrary.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.rb_atm) {
                ivOverlayAtm.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.rb_document_printer) {
                ivOverlayDocumentPrinter.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.rb_english_cafe) {
                ivOverlayEnglishCafe.setVisibility(View.VISIBLE);
            }
            // rb_none이 선택되면 모든 오버레이가 숨겨진 상태 유지
        });
    }

    /**
     * 모든 오버레이 숨김
     */
    private void hideAllOverlays() {
        ivOverlayCopier.setVisibility(View.GONE);
        ivOverlayCafeteria.setVisibility(View.GONE);
        ivOverlayLibrary.setVisibility(View.GONE);
        ivOverlayAtm.setVisibility(View.GONE);
        ivOverlayDocumentPrinter.setVisibility(View.GONE);
        ivOverlayEnglishCafe.setVisibility(View.GONE);
    }

    /**
     * 지도를 확대 화면으로 열기
     */
    private void openImageZoom() {
        try {
            // 지도 컨테이너의 모든 레이어를 합쳐서 Bitmap으로 만들기
            Bitmap bitmap = createBitmapFromView(mapContainer);

            if (bitmap == null) {
                Toast.makeText(requireContext(), "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            // ImageZoomActivity에 Bitmap 전달
            ImageZoomActivity.setTempBitmap(bitmap);

            Intent intent = new Intent(requireContext(), ImageZoomActivity.class);
            intent.putExtra(ImageZoomActivity.EXTRA_TITLE, "캠퍼스 지도");
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(requireContext(), "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * View를 Bitmap으로 변환
     */
    private Bitmap createBitmapFromView(View view) {
        if (view.getWidth() == 0 || view.getHeight() == 0) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }
}
