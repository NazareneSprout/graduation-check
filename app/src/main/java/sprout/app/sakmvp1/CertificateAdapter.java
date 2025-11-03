package sprout.app.sakmvp1; // 패키지 경로 확인

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import sprout.app.sakmvp1.R;
import sprout.app.sakmvp1.WebViewActivity;

public class CertificateAdapter extends RecyclerView.Adapter<CertificateAdapter.CertificateViewHolder> {

    private final List<Certificate> certificateList;
    private final String currentUserId; // 현재 유저 ID

    // 북마크 클릭 리스너 인터페이스
    public interface OnBookmarkClickListener {
        void onBookmarkClick(Certificate certificate);
    }
    private final OnBookmarkClickListener bookmarkClickListener;

    public CertificateAdapter(List<Certificate> certificateList, OnBookmarkClickListener listener) {
        this.certificateList = certificateList;
        this.bookmarkClickListener = listener;
        // 현재 로그인한 유저 ID 가져오기 (null일 수 있음)
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            this.currentUserId = null;
        }
    }

    @NonNull
    @Override
    public CertificateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_certificate_card, parent, false);
        return new CertificateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CertificateViewHolder holder, int position) {
        Certificate certificate = certificateList.get(position);

        holder.textTitle.setText(certificate.getTitle());
        holder.textIssuer.setText(certificate.getIssuer());


        // 북마크 상태 설정
        if (currentUserId != null && certificate.getBookmarks() != null && certificate.getBookmarks().containsKey(currentUserId)) {
            // 북마크 한 경우: 꽉 찬 아이콘
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark_filled);
        } else {
            // 북마크 안 한 경우: 테두리 아이콘
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark_border);
        }

        // 북마크 버튼 클릭 리스너
        holder.btnBookmark.setOnClickListener(v -> {
            if (bookmarkClickListener != null) {
                bookmarkClickListener.onBookmarkClick(certificate);
            }
        });

        // 카드 전체 클릭 리스너 (WebView로 이동)
        holder.itemView.setOnClickListener(v -> {
            String url = certificate.getTargetUrl();
            if (url != null && !url.isEmpty()) {
                Context context = v.getContext();
                // WebViewActivity 경로는 본인 프로젝트에 맞게 수정 필요
                Intent intent = new Intent(context, WebViewActivity.class);
                intent.putExtra("url", url);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return certificateList.size();
    }

    static class CertificateViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textIssuer, textViewCount;
        ImageButton btnBookmark;

        CertificateViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_title);
            textIssuer = itemView.findViewById(R.id.text_issuer);
            textViewCount = itemView.findViewById(R.id.text_view_count);
            // icon_views ID는 item_certificate_card.xml에 정의된 ID와 일치해야 함
            btnBookmark = itemView.findViewById(R.id.btn_bookmark);
        }
    }
}
