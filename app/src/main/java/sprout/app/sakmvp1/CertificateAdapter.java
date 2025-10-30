package sprout.app.sakmvp1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

/**
 * 자격증 카드 목록을 RecyclerView에 바인딩하는 어댑터
 */
public class CertificateAdapter extends RecyclerView.Adapter<CertificateAdapter.CertificateViewHolder> {

    private List<Certificate> certificateList;

    public CertificateAdapter(List<Certificate> certificateList) {
        this.certificateList = certificateList;
    }

    @NonNull
    @Override
    public CertificateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // item_certificate_card.xml 레이아웃을 inflate
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_certificate_card, parent, false);
        return new CertificateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CertificateViewHolder holder, int position) {
        // 현재 위치의 Certificate 객체를 가져옴
        Certificate certificate = certificateList.get(position);

        // ViewHolder의 뷰들에 데이터를 설정
        holder.textTitle.setText(certificate.getTitle());
        holder.textIssuer.setText(certificate.getIssuer());
        holder.textDDay.setText(certificate.getDDay());

        // 조회수 포맷팅 (e.g., 2098 -> "2,098")
        holder.textViewCount.setText(String.format(Locale.getDefault(), "%,d", certificate.getViewCount()));

        // D-Day가 없거나 "상시"인 경우 D-Day 칩을 숨길 수 있습니다.
        if (certificate.getDDay() == null || certificate.getDDay().isEmpty()) {
            holder.textDDay.setVisibility(View.GONE);
        } else {
            holder.textDDay.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return certificateList == null ? 0 : certificateList.size();
    }

    /**
     * ViewHolder 클래스
     * item_certificate_card.xml의 뷰들을 참조합니다.
     */
    public static class CertificateViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle;
        TextView textIssuer;
        TextView textDDay;
        TextView textViewCount;
        ImageView iconViews; // 아이콘

        public CertificateViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_title);
            textIssuer = itemView.findViewById(R.id.text_issuer);
            textDDay = itemView.findViewById(R.id.text_d_day);
            textViewCount = itemView.findViewById(R.id.text_view_count);

        }
    }

    /**
     * [중요] 필터링된 새 리스트로 데이터를 업데이트하는 메서드
     */
    public void updateData(List<Certificate> newList) {
        this.certificateList.clear();
        this.certificateList.addAll(newList);
        notifyDataSetChanged(); // RecyclerView 갱신
    }
}
