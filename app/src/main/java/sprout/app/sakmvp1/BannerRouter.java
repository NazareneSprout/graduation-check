package sprout.app.sakmvp1;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 배너 클릭 시 스마트 라우팅을 처리하는 클래스
 *
 * 배너 타입에 따라 적절한 Activity로 이동:
 * - INTERNAL: 앱 내부 기능
 * - EXTERNAL: 외부 웹페이지 (WebView)
 * - NONE: 클릭 불가
 */
public class BannerRouter {

    private static final String TAG = "BannerRouter";

    /**
     * 배너 클릭 시 호출되는 메인 라우팅 메소드
     *
     * @param context Context
     * @param banner 클릭된 배너
     */
    public static void navigate(Context context, Banner banner) {
        if (context == null || banner == null) {
            Log.w(TAG, "Context 또는 Banner가 null입니다");
            return;
        }

        // 비활성화된 배너는 클릭 불가
        if (!banner.isActive()) {
            Log.d(TAG, "비활성화된 배너: " + banner.getTitle());
            return;
        }

        // 활성화 기간이 아니면 클릭 불가
        if (!banner.isInActivePeriod()) {
            Log.d(TAG, "활성화 기간이 아닌 배너: " + banner.getTitle());
            return;
        }

        String type = banner.getType();
        if (type == null) {
            type = "EXTERNAL"; // 기본값
        }

        Intent intent = null;

        switch (type) {
            case "INTERNAL":
                intent = getInternalIntent(context, banner.getTargetScreen());
                break;

            case "EXTERNAL":
                intent = getExternalIntent(context, banner);
                break;

            case "NONE":
                // 클릭 불가
                Log.d(TAG, "클릭 불가 배너: " + banner.getTitle());
                return;

            default:
                Log.w(TAG, "알 수 없는 배너 타입: " + type);
                return;
        }

        if (intent != null) {
            try {
                context.startActivity(intent);
                Log.d(TAG, "배너 이동 성공: " + banner.getTitle() + " -> " + type);
            } catch (Exception e) {
                Log.e(TAG, "배너 이동 실패", e);
            }
        } else {
            Log.w(TAG, "Intent가 생성되지 않았습니다: " + banner.getTitle());
        }
    }

    /**
     * 앱 내부 화면으로 이동하는 Intent 생성
     *
     * @param context Context
     * @param targetScreen 대상 화면 (graduation, recommendation, certificate, documents, meal, timetable)
     * @return Intent
     */
    private static Intent getInternalIntent(Context context, String targetScreen) {
        if (targetScreen == null) {
            Log.w(TAG, "targetScreen이 null입니다");
            return null;
        }

        switch (targetScreen) {
            case "graduation":
                // 졸업요건 검사
                return new Intent(context, LoadingUserInfoActivity.class);

            case "recommendation":
                // 수강과목 추천
                return new Intent(context, CourseRecommendationActivity.class);

            case "certificate":
                // 자격증 게시판
                return new Intent(context, CertificateBoardActivity.class);

            case "documents":
                // 필수 서류
                return new Intent(context, RequiredDocumentsActivity.class);

            case "meal":
                // 학식 메뉴
                return new Intent(context, MealMenuActivity.class);

            case "timetable":
                // 시간표
                return new Intent(context, sprout.app.sakmvp1.timetable.TimeTableActivity.class);

            default:
                Log.w(TAG, "알 수 없는 targetScreen: " + targetScreen);
                return null;
        }
    }

    /**
     * 외부 웹페이지로 이동하는 Intent 생성 (WebView)
     *
     * @param context Context
     * @param banner 배너
     * @return Intent
     */
    private static Intent getExternalIntent(Context context, Banner banner) {
        String url = banner.getTargetUrl();
        if (url == null || url.isEmpty()) {
            Log.w(TAG, "targetUrl이 비어있습니다");
            return null;
        }

        Intent intent = new Intent(context, WebViewActivity.class);
        intent.putExtra("url", url);

        // 배너 제목이 있으면 WebView 타이틀로 사용
        if (banner.getTitle() != null && !banner.getTitle().isEmpty()) {
            intent.putExtra("title", banner.getTitle());
        }

        return intent;
    }

    /**
     * 배너 타입 표시명 반환 (관리자 UI용)
     *
     * @param type 배너 타입
     * @return 표시명
     */
    public static String getTypeDisplayName(String type) {
        if (type == null) {
            return "외부 링크";
        }
        switch (type) {
            case "INTERNAL":
                return "앱 내부";
            case "EXTERNAL":
                return "외부 링크";
            case "NONE":
                return "클릭 불가";
            default:
                return "외부 링크";
        }
    }

    /**
     * 내부 화면 표시명 반환 (관리자 UI용)
     *
     * @param targetScreen 대상 화면
     * @return 표시명
     */
    public static String getTargetScreenDisplayName(String targetScreen) {
        if (targetScreen == null) {
            return "";
        }
        switch (targetScreen) {
            case "graduation":
                return "졸업요건 검사";
            case "recommendation":
                return "수강과목 추천";
            case "certificate":
                return "자격증 게시판";
            case "documents":
                return "필수 서류";
            case "meal":
                return "학식 메뉴";
            case "timetable":
                return "시간표";
            default:
                return targetScreen;
        }
    }
}
