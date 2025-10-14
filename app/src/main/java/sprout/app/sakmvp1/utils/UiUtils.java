package sprout.app.sakmvp1.utils;

import android.content.Context;

/**
 * UI 관련 유틸리티 클래스
 */
public class UiUtils {

    /**
     * DP를 픽셀로 변환
     * @param context Context
     * @param dp DP 값
     * @return 픽셀 값
     */
    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    /**
     * 픽셀을 DP로 변환
     * @param context Context
     * @param px 픽셀 값
     * @return DP 값
     */
    public static int pxToDp(Context context, int px) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) px / density);
    }
}
