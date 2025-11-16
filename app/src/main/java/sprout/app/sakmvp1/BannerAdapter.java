package sprout.app.sakmvp1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * 홈 화면 배너용 ViewPager2 어댑터
 */
public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private List<Banner> banners = new ArrayList<>();
    private OnBannerClickListener listener;

    public interface OnBannerClickListener {
        void onBannerClick(Banner banner);
    }

    public BannerAdapter(OnBannerClickListener listener) {
        this.listener = listener;
    }

    public void setBanners(List<Banner> banners) {
        this.banners = banners != null ? banners : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.banner_item, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        Banner banner = banners.get(position);

        // Glide로 이미지 로드
        if (banner.getImageUrl() != null && !banner.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(banner.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.banner_image_1)
                    .error(R.drawable.banner_image_1)
                    .into(holder.bannerImage);
        } else {
            holder.bannerImage.setImageResource(R.drawable.banner_image_1);
        }

        // 클릭 리스너 설정
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBannerClick(banner);
            }
        });
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView bannerImage;

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            bannerImage = itemView.findViewById(R.id.banner_image);
        }
    }
}
