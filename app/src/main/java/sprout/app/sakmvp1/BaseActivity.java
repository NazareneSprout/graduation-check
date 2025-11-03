package sprout.app.sakmvp1;

// 설정값을 저장하고 읽어오기 위한 SharedPreferences 클래스
import android.content.SharedPreferences;
// 색상 변환을 위한 ColorMatrix 클래스 (색상을 수학적으로 변환)
import android.graphics.ColorMatrix;
// ColorMatrix를 실제 필터로 만들어주는 클래스
import android.graphics.ColorMatrixColorFilter;
// 그림을 그릴 때 사용하는 붓 역할을 하는 Paint 클래스
import android.graphics.Paint;
// Activity 생명주기를 관리하는 Bundle 클래스
import android.os.Bundle;
// 화면의 View (모든 화면 요소의 기본 클래스)
import android.view.View;

// 안드로이드의 기본 Activity 클래스 (모든 화면의 기반)
import androidx.appcompat.app.AppCompatActivity;

/**
 * 모든 Activity의 기본 클래스
 * 접근성 기능(색약 모드 등)을 일괄 적용
 *
 * 이 클래스를 상속받은 모든 Activity는 자동으로 색약 모드를 지원합니다.
 */
public class BaseActivity extends AppCompatActivity {

    /**
     * onCreate: Activity가 생성될 때 자동으로 호출되는 메서드
     * 화면이 처음 만들어질 때 딱 한 번 실행됩니다.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 부모 클래스(AppCompatActivity)의 onCreate를 먼저 실행
        // 이것은 기본적인 화면 설정을 완료하는 작업입니다
        super.onCreate(savedInstanceState);

        // 접근성 설정을 확인하고 적용합니다
        // 색약 모드가 켜져있으면 흑백 필터를 적용합니다
        applyAccessibilitySettings();
    }

    /**
     * onResume: Activity가 화면에 표시될 때 자동으로 호출되는 메서드
     * 다른 화면에 갔다가 다시 돌아올 때마다 실행됩니다.
     */
    @Override
    protected void onResume() {
        // 부모 클래스의 onResume을 먼저 실행
        super.onResume();

        // 다시 화면에 돌아왔을 때도 설정을 다시 확인하고 적용합니다
        // 혹시 다른 화면에서 설정이 바뀌었을 수도 있으니까요
        applyAccessibilitySettings();
    }

    /**
     * 접근성 설정 적용
     *
     * 이 메서드는 저장된 설정을 확인해서
     * 색약 모드가 켜져있으면 흑백 필터를 적용하고,
     * 꺼져있으면 필터를 제거합니다.
     */
    private void applyAccessibilitySettings() {
        // SharedPreferences: 앱의 설정을 저장하는 저장소
        // "accessibility_prefs"라는 이름의 저장소를 엽니다
        // MODE_PRIVATE: 이 앱만 접근할 수 있도록 설정
        SharedPreferences prefs = getSharedPreferences("accessibility_prefs", MODE_PRIVATE);

        // 저장소에서 "color_blind_mode"라는 이름의 설정을 읽어옵니다
        // 만약 설정이 없으면 기본값 false를 사용합니다
        // true = 색약 모드 켜짐, false = 색약 모드 꺼짐
        boolean colorBlindMode = prefs.getBoolean("color_blind_mode", false);

        // 색약 모드가 켜져있는지 확인합니다
        if (colorBlindMode) {
            // 켜져있으면: 흑백 필터를 적용합니다
            applyGrayscaleFilter();
        } else {
            // 꺼져있으면: 흑백 필터를 제거합니다 (원래 색상으로 복원)
            removeGrayscaleFilter();
        }
    }

    /**
     * 흑백 필터 적용
     *
     * ColorMatrix를 사용해서 화면의 모든 색상을 회색으로 변환합니다.
     * 채도(saturation)를 0으로 만들어서 색을 제거하는 원리입니다.
     */
    private void applyGrayscaleFilter() {
        // 1단계: ColorMatrix 생성
        // ColorMatrix는 색상을 수학적으로 변환하는 도구입니다
        // 빨강(R), 초록(G), 파랑(B) 값을 조작할 수 있습니다
        ColorMatrix colorMatrix = new ColorMatrix();

        // 2단계: 채도를 0으로 설정
        // setSaturation(0): 모든 색의 채도를 0으로 만듭니다
        // 채도 = 색의 선명함, 진함
        // 채도 0 = 색이 없음 = 회색
        //
        // 예시:
        // - 빨강(R=255, G=0, B=0) → 회색(R=85, G=85, B=85)
        // - 파랑(R=0, G=0, B=255) → 회색(R=85, G=85, B=85)
        // - 노랑(R=255, G=255, B=0) → 밝은 회색(R=170, G=170, B=170)
        colorMatrix.setSaturation(0);

        // 3단계: ColorMatrix를 실제 필터로 변환
        // ColorMatrixColorFilter: ColorMatrix를 화면에 적용 가능한 필터로 만듭니다
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);

        // 4단계: Paint 객체 생성
        // Paint는 화면에 그림을 그릴 때 사용하는 "붓"입니다
        // 이 붓에 필터를 설정하면 그려지는 모든 것에 필터가 적용됩니다
        Paint paint = new Paint();

        // 5단계: Paint에 필터 적용
        // setColorFilter: 이 붓으로 그릴 때 색상을 어떻게 변환할지 설정합니다
        paint.setColorFilter(filter);

        // 6단계: 화면의 최상위 View 가져오기
        // getWindow(): 현재 Activity의 Window 객체를 가져옵니다
        // getDecorView(): Window의 최상위 View를 가져옵니다
        //
        // DecorView란?
        // - 화면의 모든 내용을 담는 최상위 컨테이너입니다
        // - 상태바, 툴바, 내용, 버튼 등 모든 것이 이 안에 들어있습니다
        // - 여기에 필터를 적용하면 화면 전체가 영향을 받습니다
        View decorView = getWindow().getDecorView();

        // 7단계: DecorView에 필터 적용
        // setLayerType: View를 그리는 방식을 설정합니다
        //
        // LAYER_TYPE_HARDWARE: GPU(그래픽 카드)를 사용해서 그립니다
        // - 장점: 빠르고 부드러운 화면 표시
        // - 장점: 복잡한 효과(필터)를 적용할 때 효율적
        // - 장점: 배터리 효율적
        //
        // paint: 위에서 만든 필터가 적용된 붓
        //
        // 결과: 화면 전체가 이 필터를 거쳐서 그려집니다
        //       → 모든 색상이 회색으로 표시됩니다!
        decorView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    /**
     * 흑백 필터 제거
     *
     * 색약 모드를 끄면 이 메서드가 호출됩니다.
     * 화면을 원래 색상으로 복원합니다.
     */
    private void removeGrayscaleFilter() {
        // 1단계: 화면의 최상위 View 가져오기
        View decorView = getWindow().getDecorView();

        // 2단계: 레이어 타입을 NONE으로 설정
        // LAYER_TYPE_NONE: 특별한 효과 없이 기본 방식으로 그립니다
        // null: 필터를 적용하지 않습니다 (Paint 없음)
        //
        // 결과: 원래 색상으로 화면이 표시됩니다!
        decorView.setLayerType(View.LAYER_TYPE_NONE, null);
    }
}
