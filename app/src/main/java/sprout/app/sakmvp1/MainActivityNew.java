package sprout.app.sakmvp1;

// Context: 앱의 현재 상태나 환경 정보에 접근하기 위한 클래스
import android.content.Context;
// Intent: 화면 전환이나 작업을 요청하는 클래스
import android.content.Intent;
// SharedPreferences: 앱의 설정값을 저장하고 읽는 클래스
import android.content.SharedPreferences;
// Bundle: Activity 생명주기나 데이터 전달에 사용하는 클래스
import android.os.Bundle;

// EdgeToEdge: 화면을 전체(상태바 아래까지) 사용하는 기능
import androidx.activity.EdgeToEdge;
// Insets: 시스템 UI(상태바, 네비게이션바)의 여백 정보
import androidx.core.graphics.Insets;
// ViewCompat: View의 호환성 기능을 제공하는 클래스
import androidx.core.view.ViewCompat;
// WindowInsetsCompat: Window의 여백(insets) 정보를 다루는 클래스
import androidx.core.view.WindowInsetsCompat;
// Fragment: 화면의 일부분을 나타내는 클래스 (재사용 가능한 UI 조각)
import androidx.fragment.app.Fragment;
// FragmentManager: Fragment를 관리하는 클래스
import androidx.fragment.app.FragmentManager;
// FragmentTransaction: Fragment를 추가/제거/교체하는 작업을 수행하는 클래스
import androidx.fragment.app.FragmentTransaction;

// BottomNavigationView: 화면 하단의 탭 메뉴
import com.google.android.material.bottomnavigation.BottomNavigationView;

// Firebase Authentication: 사용자 인증 관리
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
// Firestore: 클라우드 데이터베이스
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

// TimeTableFragment: 시간표 화면 Fragment
import sprout.app.sakmvp1.timetable.TimeTableFragment;

/**
 * Single Activity + Fragment 패턴의 메인 컨테이너 Activity
 *
 * 이 Activity는 앱의 주요 화면들(홈, 시간표, 지도, 내정보)을 담는 컨테이너 역할을 합니다.
 * 하단 네비게이션 바를 통해 Fragment를 전환하며 화면을 보여줍니다.
 */
// ⭐ 중요: BaseActivity를 상속받습니다!
//
// 상속이란?
// - 부모 클래스의 기능을 자식 클래스가 물려받는 것입니다
// - MainActivityNew는 BaseActivity의 "자식"입니다
// - BaseActivity는 MainActivityNew의 "부모"입니다
//
// BaseActivity가 제공하는 기능:
// 1. onCreate(): 화면이 생성될 때 색약 모드 설정 확인
// 2. onResume(): 화면이 표시될 때 색약 모드 설정 확인
// 3. applyAccessibilitySettings(): 색약 모드 적용/해제
// 4. applyGrayscaleFilter(): 흑백 필터 적용
// 5. removeGrayscaleFilter(): 흑백 필터 제거
//
// MainActivityNew가 하는 일:
// - 부모(BaseActivity)의 모든 기능을 자동으로 가지고 있음
// - 추가로 탭 메뉴와 Fragment 관리 기능을 구현함
//
// 결과:
// → MainActivityNew는 자동으로 색약 모드를 지원합니다!
// → 별도의 코드 추가 없이 BaseActivity를 상속받는 것만으로 가능합니다!
//
// 만약 AppCompatActivity를 직접 상속받았다면?
// - 색약 모드 기능이 작동하지 않습니다
// - applyGrayscaleFilter() 같은 메서드가 없기 때문입니다
//
// extends: "확장하다", "상속받다"의 의미
// BaseActivity의 기능을 확장하여 MainActivityNew를 만듭니다
public class MainActivityNew extends BaseActivity {

    private BottomNavigationView bottomNavigation;
    private FragmentManager fragmentManager;

    // Fragment 인스턴스 재사용
    private HomeFragment homeFragment;
    private TimeTableFragment timeTableFragment;
    private CampusMapFragment campusMapFragment;
    private UserProfileFragment userProfileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 관리자 모드 체크 - 관리자라면 AdminActivity로 리다이렉트
        SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        boolean isAdmin = prefs.getBoolean("is_admin", false);
        if (isAdmin) {
            Intent intent = new Intent(this, AdminActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Firestore에서 접근성 설정 로드
        loadAccessibilitySettingsFromFirestore();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_container);

        // 시스템 바 인셋 처리
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        fragmentManager = getSupportFragmentManager();
        bottomNavigation = findViewById(R.id.bottom_navigation);

        // 네비게이션 바에 시스템 네비게이션 바 높이만큼 하단 패딩 추가
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> {
            Insets navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), navigationBars.bottom);
            return insets;
        });

        setupBottomNavigation();

        // 초기 Fragment 설정 (savedInstanceState가 null일 때만)
        if (savedInstanceState == null) {
            // Intent로 전달된 탭 선택 확인
            String selectedTab = getIntent().getStringExtra("selected_tab");
            if ("timetable".equals(selectedTab)) {
                bottomNavigation.setSelectedItemId(R.id.nav_button_2);
                if (timeTableFragment == null) {
                    timeTableFragment = new TimeTableFragment();
                }
                loadFragment(timeTableFragment);
            } else {
                loadFragment(new HomeFragment());
            }
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_button_1) {
                // 홈 Fragment
                if (homeFragment == null) {
                    homeFragment = new HomeFragment();
                }
                fragment = homeFragment;
            } else if (itemId == R.id.nav_button_2) {
                // 시간표 Fragment
                if (timeTableFragment == null) {
                    timeTableFragment = new TimeTableFragment();
                }
                fragment = timeTableFragment;
            } else if (itemId == R.id.nav_button_3) {
                // 캠퍼스 지도 Fragment
                if (campusMapFragment == null) {
                    campusMapFragment = new CampusMapFragment();
                }
                fragment = campusMapFragment;
            } else if (itemId == R.id.nav_button_4) {
                // 내 정보 Fragment
                if (userProfileFragment == null) {
                    userProfileFragment = new UserProfileFragment();
                }
                fragment = userProfileFragment;
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 현재 표시 중인 Fragment에 따라 네비게이션 상태 업데이트
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof HomeFragment) {
            bottomNavigation.setSelectedItemId(R.id.nav_button_1);
        } else if (currentFragment instanceof TimeTableFragment) {
            bottomNavigation.setSelectedItemId(R.id.nav_button_2);
        } else if (currentFragment instanceof CampusMapFragment) {
            bottomNavigation.setSelectedItemId(R.id.nav_button_3);
        } else if (currentFragment instanceof UserProfileFragment) {
            bottomNavigation.setSelectedItemId(R.id.nav_button_4);
        }
    }

    /**
     * Firestore에서 사용자의 접근성 설정을 로드하여 SharedPreferences에 저장
     *
     * 앱 시작 시 호출되어 Firestore에 저장된 설정을 로드합니다.
     * 로드된 설정은 SharedPreferences에 저장되어 BaseActivity가 사용합니다.
     */
    private void loadAccessibilitySettingsFromFirestore() {
        // Firebase 인증에서 현재 로그인된 사용자 가져오기
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        // 로그인하지 않은 경우 설정을 로드하지 않음
        if (user == null) {
            return;
        }

        String userId = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Firestore에서 사용자의 접근성 설정 문서 조회
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // color_blind_mode 필드 읽기 (기본값: false)
                        Boolean colorBlindMode = documentSnapshot.getBoolean("color_blind_mode");
                        if (colorBlindMode == null) {
                            colorBlindMode = false;
                        }

                        // SharedPreferences에 저장하여 BaseActivity가 사용할 수 있도록 함
                        SharedPreferences prefs = getSharedPreferences("accessibility_prefs", Context.MODE_PRIVATE);
                        prefs.edit()
                                .putBoolean("color_blind_mode", colorBlindMode)
                                .apply();

                        android.util.Log.d("MainActivityNew", "Firestore에서 접근성 설정 로드 완료: color_blind_mode = " + colorBlindMode);

                        // BaseActivity의 refreshAccessibilitySettings()를 호출하여 즉시 적용
                        refreshAccessibilitySettings();
                    }
                })
                .addOnFailureListener(e -> {
                    // 로드 실패 시 로그만 출력하고 기본값(색약 모드 꺼짐) 사용
                    android.util.Log.w("MainActivityNew", "접근성 설정 로드 실패 - 기본값 사용", e);
                });
    }
}
