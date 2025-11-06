package sprout.app.sakmvp1;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

/**
 * 챗봇 Activity - "나싹이" 학사 도우미
 */
public class ChatBotActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView recyclerChat;
    private EditText etMessage;
    private MaterialButton btnSend;
    private Chip chipGraduation, chipRecommend, chipTimetable, chipCertificate, chipHelp;

    private ChatAdapter adapter;
    private List<ChatMessage> messages;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        handler = new Handler();
        messages = new ArrayList<>();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();

        // 초기 인사 메시지
        addBotMessage("안녕하세요! 저는 싹싹이에요 🌱\n무엇을 도와드릴까요?");
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerChat = findViewById(R.id.recycler_chat);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        chipGraduation = findViewById(R.id.chip_graduation);
        chipRecommend = findViewById(R.id.chip_recommend);
        chipTimetable = findViewById(R.id.chip_timetable);
        chipCertificate = findViewById(R.id.chip_certificate);
        chipHelp = findViewById(R.id.chip_help);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerChat.setLayoutManager(layoutManager);
        adapter = new ChatAdapter(messages, message -> {
            // 액션 버튼 클릭
            handleAction(message);
        });
        recyclerChat.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> sendMessage());

        chipGraduation.setOnClickListener(v -> processUserMessage("졸업 요건"));
        chipRecommend.setOnClickListener(v -> processUserMessage("과목 추천"));
        chipTimetable.setOnClickListener(v -> processUserMessage("시간표"));
        chipCertificate.setOnClickListener(v -> processUserMessage("자격증"));
        chipHelp.setOnClickListener(v -> processUserMessage("도움말"));
    }

    private void sendMessage() {
        String message = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            return;
        }

        etMessage.setText("");
        processUserMessage(message);
    }

    private void processUserMessage(String message) {
        // 사용자 메시지 추가
        addUserMessage(message);

        // 봇 응답 생성 (딜레이)
        handler.postDelayed(() -> {
            String response = generateBotResponse(message);
            addBotMessage(response);
        }, 500);
    }

    private String generateBotResponse(String userMessage) {
        String msg = userMessage.toLowerCase();

        // 키워드 매칭
        if (msg.contains("안녕") || msg.contains("처음")) {
            return "안녕하세요! 싹싹이에요 😊\n졸업 요건, 과목 추천, 시간표, 자격증, 학식 등을 도와드릴 수 있어요!";
        }

        if (msg.contains("졸업") || msg.contains("요건") || msg.contains("남은")) {
            ChatMessage actionMsg = new ChatMessage(
                "졸업 요건 분석 화면으로 이동하시겠어요?\n현재 이수 현황과 남은 학점을 확인할 수 있어요!",
                ChatMessage.TYPE_BOT,
                "navigate",
                "GraduationAnalysisActivity"
            );
            messages.add(actionMsg);
            adapter.notifyItemInserted(messages.size() - 1);
            scrollToBottom();
            return null;
        }

        if (msg.contains("전공")) {
            return "전공 학점은 졸업 요건 분석에서 확인하실 수 있어요!\n'졸업 요건' 버튼을 눌러보세요.";
        }

        if (msg.contains("교양")) {
            return "교양 학점은 5개 역량(기독교, 인성, 의사소통, 융복합, 글로벌)으로 구성되어 있어요.\n각 역량별로 1과목 이상 필수입니다!";
        }

        if (msg.contains("추천") || msg.contains("과목")) {
            ChatMessage actionMsg = new ChatMessage(
                "과목 추천 기능을 이용하시겠어요?\n부족한 학점에 맞춰 과목을 추천해드려요!",
                ChatMessage.TYPE_BOT,
                "navigate",
                "CourseRecommendationActivity"
            );
            messages.add(actionMsg);
            adapter.notifyItemInserted(messages.size() - 1);
            scrollToBottom();
            return null;
        }

        if (msg.contains("시간표")) {
            ChatMessage actionMsg = new ChatMessage(
                "시간표 화면으로 이동하시겠어요?\n저장된 시간표를 확인하고 수정할 수 있어요!",
                ChatMessage.TYPE_BOT,
                "navigate",
                "SavedTimetablesActivity"
            );
            messages.add(actionMsg);
            adapter.notifyItemInserted(messages.size() - 1);
            scrollToBottom();
            return null;
        }

        if (msg.contains("자격증")) {
            ChatMessage actionMsg = new ChatMessage(
                "자격증 게시판으로 이동하시겠어요?\n학부별 추천 자격증을 확인할 수 있어요!",
                ChatMessage.TYPE_BOT,
                "navigate",
                "CertificateBoardActivity"
            );
            messages.add(actionMsg);
            adapter.notifyItemInserted(messages.size() - 1);
            scrollToBottom();
            return null;
        }

        if (msg.contains("학식") || msg.contains("식단") || msg.contains("급식")) {
            ChatMessage actionMsg = new ChatMessage(
                "오늘의 학식 메뉴를 확인하시겠어요?\n식단표와 영양 정보를 볼 수 있어요!",
                ChatMessage.TYPE_BOT,
                "navigate",
                "MealMenuActivity"
            );
            messages.add(actionMsg);
            adapter.notifyItemInserted(messages.size() - 1);
            scrollToBottom();
            return null;
        }

        if (msg.contains("어시스트") || msg.contains("프로그램") || msg.contains("대학생활")) {
            ChatMessage actionMsg = new ChatMessage(
                "대학생활 지원 프로그램 화면으로 이동하시겠어요?\n유용한 프로그램들을 모아놨어요!",
                ChatMessage.TYPE_BOT,
                "navigate",
                "AssistProgramActivity"
            );
            messages.add(actionMsg);
            adapter.notifyItemInserted(messages.size() - 1);
            scrollToBottom();
            return null;
        }

        if (msg.contains("접근성") || msg.contains("장애") || msg.contains("색약") || msg.contains("지원모드")) {
            ChatMessage actionMsg = new ChatMessage(
                "접근성 지원 모드를 활성화하시겠어요?\n색약 모드 등 다양한 접근성 기능을 제공해요!",
                ChatMessage.TYPE_BOT,
                "navigate",
                "AssistProgramActivity"
            );
            messages.add(actionMsg);
            adapter.notifyItemInserted(messages.size() - 1);
            scrollToBottom();
            return null;
        }

        if (msg.contains("도움") || msg.contains("help") || msg.contains("명령")) {
            return "다음과 같은 키워드를 사용할 수 있어요! 🌱\n\n" +
                   "📚 학업 관련\n" +
                   "• 졸업/요건/남은 → 졸업 요건 분석\n" +
                   "• 전공 → 전공 학점 정보\n" +
                   "• 교양 → 교양 5개 역량 설명\n" +
                   "• 추천/과목 → 과목 추천받기\n" +
                   "• 시간표 → 저장된 시간표 보기\n\n" +
                   "🎯 학교생활\n" +
                   "• 자격증 → 학부별 자격증 정보\n" +
                   "• 학식/식단/급식 → 오늘의 식단표\n" +
                   "• 어시스트/프로그램/대학생활 → 지원 프로그램\n\n" +
                   "♿ 접근성\n" +
                   "• 접근성/장애/색약/지원모드 → 접근성 기능\n\n" +
                   "💬 기타\n" +
                   "• 안녕/처음 → 인사하기\n" +
                   "• 도움/help/명령 → 이 도움말\n\n" +
                   "궁금한 걸 물어보세요!";
        }

        if (msg.contains("고마") || msg.contains("감사")) {
            return "천만에요! 😊 또 필요하신 게 있으면 언제든지 물어보세요!";
        }

        // 기본 응답
        return "죄송해요, 잘 이해하지 못했어요 😅\n'도움말'을 눌러서 사용 가능한 명령어를 확인해보세요!";
    }

    private void addUserMessage(String message) {
        messages.add(new ChatMessage(message, ChatMessage.TYPE_USER));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private void addBotMessage(String message) {
        if (message == null) return;
        messages.add(new ChatMessage(message, ChatMessage.TYPE_BOT));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private void handleAction(ChatMessage message) {
        if ("navigate".equals(message.getActionType())) {
            String activityName = message.getActionData();
            try {
                Class<?> targetClass = Class.forName("sprout.app.sakmvp1." + activityName);
                Intent intent = new Intent(this, targetClass);
                startActivity(intent);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                addBotMessage("죄송해요, 해당 화면을 찾을 수 없어요 😥");
            }
        }
    }

    private void scrollToBottom() {
        if (messages.size() > 0) {
            recyclerChat.smoothScrollToPosition(messages.size() - 1);
        }
    }
}
