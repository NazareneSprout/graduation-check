package sprout.app.sakmvp1;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 캠퍼스 지도 Fragment
 * 학교 시설 위치를 지도에 표시하는 기능
 */
public class CampusMapFragment extends Fragment {

    private ImageView ivCampusMap;
    private FrameLayout markersContainer;

    // 체크박스들
    private MaterialCheckBox cbCopier;
    private MaterialCheckBox cbCafeteria;
    private MaterialCheckBox cbLibrary;
    private MaterialCheckBox cbAtm;
    private MaterialCheckBox cbDocumentPrinter;

    // 시설 데이터 (좌표 및 정보)
    private Map<String, List<FacilityMarker>> facilityData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_campus_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initFacilityData();
        setupCheckBoxListeners();
    }

    private void initViews(View view) {
        ivCampusMap = view.findViewById(R.id.iv_campus_map);
        markersContainer = view.findViewById(R.id.markers_container);

        cbCopier = view.findViewById(R.id.cb_copier);
        cbCafeteria = view.findViewById(R.id.cb_cafeteria);
        cbLibrary = view.findViewById(R.id.cb_library);
        cbAtm = view.findViewById(R.id.cb_atm);
        cbDocumentPrinter = view.findViewById(R.id.cb_document_printer);

        // 지도 이미지 설정 (임시: 회색 배경)
        // 실제 학교 지도 이미지가 있다면 ivCampusMap.setImageResource(R.drawable.campus_map); 으로 변경
    }

    /**
     * 시설 데이터 초기화
     * 좌표는 지도 이미지 크기(1920x1440)를 기준으로 함
     */
    private void initFacilityData() {
        facilityData = new HashMap<>();

        // 복사기
        List<FacilityMarker> copiers = new ArrayList<>();
        copiers.add(new FacilityMarker("본관 1층", 960, 480, "#FF5722"));
        copiers.add(new FacilityMarker("학생회관 2층", 480, 360, "#FF5722"));
        copiers.add(new FacilityMarker("도서관 1층", 1200, 720, "#FF5722"));
        facilityData.put("copier", copiers);

        // 식당
        List<FacilityMarker> cafeterias = new ArrayList<>();
        cafeterias.add(new FacilityMarker("학생식당", 600, 480, "#4CAF50"));
        cafeterias.add(new FacilityMarker("교직원식당", 1008, 432, "#4CAF50"));
        cafeterias.add(new FacilityMarker("푸드코트", 720, 600, "#4CAF50"));
        facilityData.put("cafeteria", cafeterias);

        // 도서관
        List<FacilityMarker> libraries = new ArrayList<>();
        libraries.add(new FacilityMarker("중앙도서관", 1200, 720, "#2196F3"));
        libraries.add(new FacilityMarker("제2도서관", 1400, 900, "#2196F3"));
        facilityData.put("library", libraries);

        // ATM기기
        List<FacilityMarker> atms = new ArrayList<>();
        atms.add(new FacilityMarker("본관 1층", 960, 480, "#9C27B0"));
        atms.add(new FacilityMarker("학생회관 1층", 504, 384, "#9C27B0"));
        atms.add(new FacilityMarker("기숙사 1층", 1560, 1080, "#9C27B0"));
        facilityData.put("atm", atms);

        // 서류출력기
        List<FacilityMarker> documentPrinters = new ArrayList<>();
        documentPrinters.add(new FacilityMarker("행정관 1층", 800, 300, "#FF9800"));
        documentPrinters.add(new FacilityMarker("본관 민원실", 960, 520, "#FF9800"));
        documentPrinters.add(new FacilityMarker("학생지원센터", 650, 650, "#FF9800"));
        facilityData.put("document_printer", documentPrinters);
    }

    /**
     * 체크박스 리스너 설정
     */
    private void setupCheckBoxListeners() {
        cbCopier.setOnCheckedChangeListener((buttonView, isChecked) -> updateMarkers("copier", isChecked));
        cbCafeteria.setOnCheckedChangeListener((buttonView, isChecked) -> updateMarkers("cafeteria", isChecked));
        cbLibrary.setOnCheckedChangeListener((buttonView, isChecked) -> updateMarkers("library", isChecked));
        cbAtm.setOnCheckedChangeListener((buttonView, isChecked) -> updateMarkers("atm", isChecked));
        cbDocumentPrinter.setOnCheckedChangeListener((buttonView, isChecked) -> updateMarkers("document_printer", isChecked));
    }

    /**
     * 마커 업데이트
     */
    private void updateMarkers(String type, boolean show) {
        // 해당 타입의 기존 마커 제거
        removeMarkersByType(type);

        if (show) {
            // 마커 표시
            List<FacilityMarker> markers = facilityData.get(type);
            if (markers != null) {
                for (FacilityMarker marker : markers) {
                    addMarkerView(marker, type);
                }
            }
        }
    }

    /**
     * 특정 타입의 마커 제거
     */
    private void removeMarkersByType(String type) {
        List<View> toRemove = new ArrayList<>();
        for (int i = 0; i < markersContainer.getChildCount(); i++) {
            View child = markersContainer.getChildAt(i);
            if (type.equals(child.getTag())) {
                toRemove.add(child);
            }
        }
        for (View view : toRemove) {
            markersContainer.removeView(view);
        }
    }

    /**
     * 마커 뷰 추가
     */
    private void addMarkerView(FacilityMarker marker, String type) {
        // 마커 컨테이너 생성
        FrameLayout markerLayout = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = marker.x - 15; // 중앙 정렬을 위한 오프셋
        params.topMargin = marker.y - 30;  // 핀 모양을 위한 오프셋
        markerLayout.setLayoutParams(params);
        markerLayout.setTag(type);

        // 마커 핀 (간단한 원형)
        View pin = new View(requireContext());
        FrameLayout.LayoutParams pinParams = new FrameLayout.LayoutParams(30, 30);
        pin.setLayoutParams(pinParams);
        pin.setBackground(createCircleDrawable(marker.color));

        // 마커 텍스트
        TextView textView = new TextView(requireContext());
        textView.setText(marker.name);
        textView.setTextSize(10);
        textView.setTextColor(Color.WHITE);
        textView.setBackgroundColor(Color.parseColor("#AA000000")); // 반투명 검정
        textView.setPadding(8, 4, 8, 4);
        textView.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.topMargin = 35;
        textParams.gravity = Gravity.CENTER_HORIZONTAL;
        textView.setLayoutParams(textParams);

        markerLayout.addView(pin);
        markerLayout.addView(textView);
        markersContainer.addView(markerLayout);
    }

    /**
     * 원형 Drawable 생성
     */
    private android.graphics.drawable.GradientDrawable createCircleDrawable(String colorHex) {
        android.graphics.drawable.GradientDrawable drawable =
            new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        drawable.setColor(Color.parseColor(colorHex));
        drawable.setStroke(3, Color.WHITE);
        return drawable;
    }

    /**
     * 시설 마커 데이터 클래스
     */
    private static class FacilityMarker {
        String name;
        int x;
        int y;
        String color;

        FacilityMarker(String name, int x, int y, String color) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }
}
