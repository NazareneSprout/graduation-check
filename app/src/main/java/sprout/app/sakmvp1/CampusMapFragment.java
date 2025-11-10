package sprout.app.sakmvp1; // 본인의 패키지 이름 확인

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

// OSMDroid 관련 import
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

/**
 * 캠퍼스 지도 Fragment
 * (최종 수정) 사용자가 제공한 정확한 좌표로 업데이트됨
 * (참고) '믿음관', '창학관' 좌표는 누락되어 핀 표시에서 제외됨
 */
public class CampusMapFragment extends Fragment {

    // 위치 권한 요청 코드
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    private MapView mapView = null;
    private RadioGroup radioGroupFacilities;
    private ArrayList<Marker> currentMarkers = new ArrayList<>();
    private MyLocationNewOverlay myLocationOverlay;

    // ---------------------------------------------------
    // [수정됨] 사용자가 제공한 정확한 건물 좌표
    // ---------------------------------------------------
    // (임의) 지도의 초기 중심을 '사랑관'으로 설정
    private static final GeoPoint NAZARETH_UNIVERSITY_CENTER = new GeoPoint(36.795471, 127.118576);

    // --- 사용자가 제공한 좌표 ---
    private static final GeoPoint POS_JALIP_TONGHAP = new GeoPoint(36.796837, 127.118747); // 자립통합학습 생활관
    // private static final GeoPoint POS_OWEN = new GeoPoint(36.796218, 127.118565); // 오온수 기념관 (시설 없음)
    private static final GeoPoint POS_SARANG = new GeoPoint(36.795471, 127.118576); // 사랑관
    private static final GeoPoint POS_NAZARETH = new GeoPoint(36.794681, 127.118790); // 나사렛관
    // private static final GeoPoint POS_BRIDGE = new GeoPoint(36.794148, 127.119327); // 브리지관 (시설 없음)
    // private static final GeoPoint POS_CHANGHAK_2 = new GeoPoint(36.793899, 127.118554); // 제2창학관 (시설 없음)
    private static final GeoPoint POS_GUKJE = new GeoPoint(36.795230, 127.119766); // 국제관

    // --- 이미지 기반 계산 좌표 ---
    // (English Cafe: "나사렛관에서 국제관으로 향하는 계단 우측" -> 두 건물의 중간)
    private static final GeoPoint POS_ENG_CAFE = new GeoPoint(36.794955, 127.119278);


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_campus_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. MapView 초기화
        mapView = view.findViewById(R.id.map_view);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);

        // 2. 지도 컨트롤러 설정 및 초기 위치 이동 (사랑관 기준)
        mapView.getController().setZoom(18.0); // 줌 레벨을 조금 더 당깁니다.
        mapView.getController().setCenter(NAZARETH_UNIVERSITY_CENTER);

        // 3. 나침반 오버레이 추가
        CompassOverlay compassOverlay = new CompassOverlay(requireContext(), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        // 4. 권한 확인 및 현재 위치 오버레이 추가
        checkAndRequestPermissions();

        // 5. 라디오 버튼 리스너 설정
        radioGroupFacilities = view.findViewById(R.id.radio_group_facilities);
        setupRadioButtonListener();

        // (수정) 핀 클릭 시 이동하는 기능은 삭제 (이전 대화에서 삭제함)
    }

    /**
     * 라디오 버튼 리스너 설정 (수정된 좌표 기준)
     */
    private void setupRadioButtonListener() {
        radioGroupFacilities.setOnCheckedChangeListener((group, checkedId) -> {
            // 기존 마커 모두 제거
            clearMarkers();

            if (checkedId == R.id.rb_copier) {
                // 복사기
                // [!] '믿음관'과 '창학관' 좌표가 없어서 해당 핀은 제외되었습니다.
                addMarker(POS_JALIP_TONGHAP, "복사기 (자립통합생활관 1층)");
                addMarker(POS_NAZARETH, "복사기 (나사렛관 3층)");

            } else if (checkedId == R.id.rb_cafeteria) {
                // 식당
                addMarker(POS_SARANG, "학생 식당 (사랑관 2층)");

            } else if (checkedId == R.id.rb_library) {
                // 도서관
                addMarker(POS_NAZARETH, "도서관 (나사렛관 3층)");

            } else if (checkedId == R.id.rb_atm) {
                // ATM기기
                // [!] '창학관' 좌표가 없어서 해당 핀은 제외되었습니다.
                addMarker(POS_JALIP_TONGHAP, "ATM (자립통합생활관 1층)");
                addMarker(POS_NAZARETH, "ATM (나사렛관 1층)");

            } else if (checkedId == R.id.rb_document_printer) {
                // 서류초력기
                addMarker(POS_SARANG, "서류초력기 (사랑관 2층 식당앞)");
                addMarker(POS_NAZARETH, "서류초력기 (나사렛관 1층)");

            } else if (checkedId == R.id.rb_english_cafe) {
                // English Cafe
                addMarker(POS_ENG_CAFE, "English Cafe (나사렛관-국제관 사이)");
            }

            // 'rb_none'이 선택되면 아무것도 호출되지 않아 마커가 모두 지워진 상태가 됨
        });
    }

    /**
     * 지도에 마커를 '추가'하고 리스트에 저장
     */
    private void addMarker(GeoPoint position, String title) {
        Marker marker = new Marker(mapView);
        marker.setPosition(position);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title); // 마커 클릭 시 표시될 타이틀

        // 기본 마커 아이콘 설정
        Drawable icon = ContextCompat.getDrawable(requireContext(), org.osmdroid.library.R.drawable.marker_default);
        marker.setIcon(icon);

        mapView.getOverlays().add(marker);
        currentMarkers.add(marker);

        mapView.invalidate(); // 지도 새로고침

        // (수정) 핀 클릭 시 이동하는 기능은 삭제 (이전 대화에서 삭제함)
        // mapView.getController().animateTo(position);
    }

    /**
     * 지도 위의 모든 시설 마커를 '제거'
     */
    private void clearMarkers() {
        for (Marker marker : currentMarkers) {
            mapView.getOverlays().remove(marker);
        }
        currentMarkers.clear();
        mapView.invalidate();
    }

    /**
     * 현재 위치 표시를 위한 권한 확인 및 요청
     */
    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (permissionsToRequest.isEmpty()) {
            setupMyLocationOverlay();
        } else {
            requestPermissions(permissionsToRequest.toArray(new String[0]), REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * 현재 위치 오버레이 설정
     */
    private void setupMyLocationOverlay() {
        if (mapView == null) return;

        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
        myLocationOverlay.enableMyLocation(); // 1. 내 위치에 파란 점 '표시' (이건 둡니다)

        // ⬇️ 2. 지도가 내 위치를 '따라가도록' 설정 (이 줄을 삭제하거나 주석 처리하세요!)
        // myLocationOverlay.enableFollowLocation();

        mapView.getOverlays().add(myLocationOverlay);
        mapView.invalidate();
    }

    /**
     * 권한 요청 결과 처리
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                setupMyLocationOverlay();
            } else {
                Toast.makeText(requireContext(), "위치 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- (필수) MapView 생명주기 메서드 연결 ---

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
        Configuration.getInstance().save(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
        if (mapView != null) {
            mapView.onDetach();
        }
        mapView = null;
        myLocationOverlay = null;
        currentMarkers.clear();
    }
}