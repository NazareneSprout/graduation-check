package sprout.app.sakmvp1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import sprout.app.sakmvp1.timetable.TimeTableFragment;

/**
 * Single Activity + Fragment 패턴의 메인 컨테이너 Activity
 */
public class MainActivityNew extends AppCompatActivity {

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
            loadFragment(new HomeFragment());
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
}
