package sprout.app.sakmvp1.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import sprout.app.sakmvp1.MealMenuActivity;
import sprout.app.sakmvp1.R;

/**
 * 학식 메뉴 업데이트 확인 백그라운드 작업
 *
 * 주기적으로 나사렛대학교 홈페이지를 확인하여
 * 새로운 식단표가 업로드되었는지 체크하고
 * 업데이트가 있으면 사용자에게 알림을 보냅니다.
 */
public class MealMenuCheckWorker extends Worker {

    private static final String TAG = "MealMenuCheckWorker";

    // 웹 URL
    private static final String MEAL_MENU_URL = "https://www.kornu.ac.kr/mbs/kornukr/jsp/board/list.jsp?boardId=29&id=kornukr_081300000000";

    // SharedPreferences 키
    private static final String PREFS_NAME = "meal_menu_prefs";
    private static final String KEY_LAST_MENU_TITLE = "last_menu_title";
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";

    // 알림 설정
    private static final String CHANNEL_ID = "meal_menu_updates";
    private static final int NOTIFICATION_ID = 1001;

    public MealMenuCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "학식 메뉴 업데이트 확인 시작");

        // 알림이 활성화되어 있는지 확인
        if (!isNotificationEnabled()) {
            Log.d(TAG, "알림이 비활성화되어 있음 - 작업 중단");
            return Result.success();
        }

        try {
            // 최신 식단표 제목 가져오기
            String latestMenuTitle = fetchLatestMenuTitle();

            if (latestMenuTitle == null) {
                Log.w(TAG, "최신 식단표를 찾을 수 없음");
                return Result.retry();
            }

            Log.d(TAG, "최신 식단표 제목: " + latestMenuTitle);

            // 이전에 저장된 제목과 비교
            String lastMenuTitle = getLastMenuTitle();

            if (lastMenuTitle == null) {
                // 처음 실행 - 현재 제목 저장만 하고 알림 없음
                Log.d(TAG, "처음 실행 - 현재 식단표 저장: " + latestMenuTitle);
                saveLastMenuTitle(latestMenuTitle);
                return Result.success();
            }

            // 제목이 다르면 새로운 식단표
            if (!latestMenuTitle.equals(lastMenuTitle)) {
                Log.d(TAG, "새로운 식단표 발견!");
                Log.d(TAG, "이전: " + lastMenuTitle);
                Log.d(TAG, "현재: " + latestMenuTitle);

                // 알림 전송
                sendNotification(latestMenuTitle);

                // 새 제목 저장
                saveLastMenuTitle(latestMenuTitle);
            } else {
                Log.d(TAG, "식단표 업데이트 없음");
            }

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "식단표 확인 중 오류 발생", e);
            return Result.retry();
        }
    }

    /**
     * 최신 식단표 제목을 가져옵니다
     */
    private String fetchLatestMenuTitle() throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(MEAL_MENU_URL)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "HTTP 요청 실패: " + response.code());
                return null;
            }

            String html = response.body().string();
            Document doc = Jsoup.parse(html);

            // 게시판 목록에서 식단표 게시물 링크 찾기
            Elements links = doc.select("#tableList a[href*=view.jsp]");

            if (links.isEmpty()) {
                Log.e(TAG, "게시물 링크를 찾을 수 없습니다");
                return null;
            }

            // 식단표 게시물 찾기 (제목에 "월"과 "주차" 포함)
            for (Element link : links) {
                String title = link.text().trim();
                if (title.contains("월") && title.contains("주차")) {
                    Log.d(TAG, "식단표 게시물 발견: " + title);
                    return title;
                }
            }

            Log.e(TAG, "식단표 게시물을 찾을 수 없습니다");
            return null;
        }
    }

    /**
     * 알림을 전송합니다
     */
    private void sendNotification(String menuTitle) {
        Context context = getApplicationContext();

        // 알림 채널 생성 (Android 8.0 이상)
        createNotificationChannel();

        // 알림 클릭 시 MealMenuActivity로 이동하는 Intent
        Intent intent = new Intent(context, MealMenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 알림 생성
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_meal)
                .setContentTitle("이번주 식단이 업데이트 되었습니다")
                .setContentText(menuTitle)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(menuTitle + "\n\n탭하여 이번 주 식단표를 확인하세요!"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true) // 알림 클릭 시 자동으로 제거
                .setVibrate(new long[]{0, 500, 200, 500}); // 진동 패턴

        // 알림 표시
        NotificationManager notificationManager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "알림 전송 완료");
        }
    }

    /**
     * 알림 채널을 생성합니다 (Android 8.0 이상)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "학식 메뉴 업데이트";
            String description = "새로운 식단표가 업로드되면 알려드립니다";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});

            NotificationManager notificationManager =
                getApplicationContext().getSystemService(NotificationManager.class);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 마지막으로 확인한 식단표 제목 가져오기
     */
    private String getLastMenuTitle() {
        SharedPreferences prefs = getApplicationContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LAST_MENU_TITLE, null);
    }

    /**
     * 식단표 제목 저장
     */
    private void saveLastMenuTitle(String title) {
        SharedPreferences prefs = getApplicationContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putString(KEY_LAST_MENU_TITLE, title)
            .apply();
    }

    /**
     * 알림 활성화 여부 확인
     */
    private boolean isNotificationEnabled() {
        SharedPreferences prefs = getApplicationContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true); // 기본값: 활성화
    }

    /**
     * 알림 활성화/비활성화 설정 (다른 클래스에서 호출용)
     */
    public static void setNotificationEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean(KEY_NOTIFICATION_ENABLED, enabled)
            .apply();
        Log.d(TAG, "학식 메뉴 알림 설정: " + (enabled ? "활성화" : "비활성화"));
    }

    /**
     * 알림 활성화 여부 확인 (다른 클래스에서 호출용)
     */
    public static boolean isNotificationEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true);
    }
}
