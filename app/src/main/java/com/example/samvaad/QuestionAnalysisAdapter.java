package com.example.samvaad;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class QuestionAnalysisAdapter extends RecyclerView.Adapter<QuestionAnalysisAdapter.ViewHolder> {

    private final List<QuestionFeedback> items;

    public QuestionAnalysisAdapter(List<QuestionFeedback> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question_analysis, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuestionFeedback feedback = items.get(position);
        holder.tvQuestion.setText(feedback.getQuestion());
        holder.tvUserSummary.setText(feedback.getWhatYouSaidSummary());
        holder.tvBetterApproach.setText(feedback.getBetterApproach());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion, tvUserSummary, tvBetterApproach;

        ViewHolder(View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tv_qa_question);
            tvUserSummary = itemView.findViewById(R.id.tv_qa_user_summary);
            tvBetterApproach = itemView.findViewById(R.id.tv_qa_better_approach);
        }
    }
}
