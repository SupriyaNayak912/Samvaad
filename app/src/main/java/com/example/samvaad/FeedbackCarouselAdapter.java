package com.example.samvaad;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FeedbackCarouselAdapter extends RecyclerView.Adapter<FeedbackCarouselAdapter.ViewHolder> {

    private final List<String> items;
    private final String iconEmoji;

    public FeedbackCarouselAdapter(List<String> items, String iconEmoji) {
        this.items = items;
        this.iconEmoji = iconEmoji;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feedback_pill, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvIcon.setText(iconEmoji);
        holder.tvText.setText(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvText;

        ViewHolder(View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tv_pill_icon);
            tvText = itemView.findViewById(R.id.tv_pill_text);
        }
    }
}
