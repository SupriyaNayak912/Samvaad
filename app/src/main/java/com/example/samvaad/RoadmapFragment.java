package com.example.samvaad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RoadmapFragment extends Fragment {

    private RecyclerView rvRoadmap;
    private List<LevelDef> levels = new ArrayList<>();
    private int currentLevel = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_roadmap, container, false);
        rvRoadmap = view.findViewById(R.id.rv_roadmap);
        
        setupLevels();
        loadUserProgress();
        
        rvRoadmap.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRoadmap.setAdapter(new RoadmapAdapter());
        rvRoadmap.addItemDecoration(new SnakePathDecoration(requireContext()));

        return view;
    }

    private void setupLevels() {
        levels.clear();
        for (LevelSystem.Level l : LevelSystem.getLevels()) {
            levels.add(new LevelDef(l.title, l.goal, android.R.drawable.btn_star_big_on));
        }
    }

    private void loadUserProgress() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("SamvaadPrefs", Context.MODE_PRIVATE);
        float bestScore = prefs.getFloat("latest_global_score", 0f);
        LevelSystem.Level current = LevelSystem.getLevelForScore(bestScore);
        currentLevel = current.id - 1;
    }

    private static class LevelDef {
        String name, description;
        int icon;
        LevelDef(String n, String d, int i) { name = n; description = d; icon = i; }
    }

    private class RoadmapAdapter extends RecyclerView.Adapter<RoadmapAdapter.NodeViewHolder> {
        @NonNull @Override
        public NodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_roadmap_node, parent, false);
            return new NodeViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull NodeViewHolder holder, int position) {
            LevelDef level = levels.get(position);
            holder.tvLevel.setText(String.valueOf(position + 1));
            holder.tvTitle.setText(level.name);
            holder.ivIcon.setImageResource(level.icon);

            boolean isUnlocked = position <= currentLevel;
            boolean isCurrent = position == currentLevel;

            if (isUnlocked) {
                holder.ivIcon.setColorFilter(null);
                holder.tvTitle.setTextColor(Color.WHITE);
                holder.ivLock.setVisibility(View.GONE);
                holder.card.setCardBackgroundColor(Color.parseColor("#334DEEEA")); 
                
                if (isCurrent) {
                    holder.glow.setAlpha(1.0f);
                    holder.glow.setBackgroundResource(R.drawable.glow_ring);
                    float density = getResources().getDisplayMetrics().density;
                    holder.card.setCardElevation(24 * density);
                } else {
                    holder.glow.setAlpha(0.2f);
                    float density = getResources().getDisplayMetrics().density;
                    holder.card.setCardElevation(4 * density);
                }
            } else {
                holder.ivIcon.setColorFilter(Color.GRAY, android.graphics.PorterDuff.Mode.SRC_IN);
                holder.tvTitle.setTextColor(Color.parseColor("#8E8E93"));
                holder.ivLock.setVisibility(View.VISIBLE);
                holder.card.setCardBackgroundColor(Color.parseColor("#1C1C1E"));
                holder.glow.setAlpha(0f);
                holder.card.setCardElevation(0f);
            }

            // Clean, perfectly centered vertical timeline layout
            View outerLayout = (View) holder.card.getParent();
            android.widget.FrameLayout.LayoutParams frameLp = (android.widget.FrameLayout.LayoutParams) outerLayout.getLayoutParams();
            
            frameLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
            outerLayout.setPadding(0, 0, 0, 0); // Remove arbitrary S-curve offsets
            outerLayout.setLayoutParams(frameLp);
            
            holder.itemView.setOnClickListener(v -> {
                if (isUnlocked) {
                    Toast.makeText(getContext(), level.description, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Level Locked - Keep practicing!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override public int getItemCount() { return levels.size(); }

        class NodeViewHolder extends RecyclerView.ViewHolder {
            TextView tvLevel, tvTitle;
            ImageView ivIcon, ivLock;
            CardView card;
            View glow;
            NodeViewHolder(@NonNull View itemView) {
                super(itemView);
                tvLevel = itemView.findViewById(R.id.tv_node_level);
                tvTitle = itemView.findViewById(R.id.tv_node_title);
                ivIcon = itemView.findViewById(R.id.iv_node_icon);
                ivLock = itemView.findViewById(R.id.iv_node_lock);
                card = itemView.findViewById(R.id.card_node);
                glow = itemView.findViewById(R.id.view_glow);
            }
        }
    }

    private class SnakePathDecoration extends RecyclerView.ItemDecoration {
        private Paint trackPaint; // The background "locked" path
        private Paint progressPaint; // The foreground "unlocked" path
        private Path path;

        SnakePathDecoration(Context context) {
            trackPaint = new Paint();
            trackPaint.setAntiAlias(true);
            trackPaint.setColor(Color.parseColor("#2A2A2A"));
            trackPaint.setStyle(Paint.Style.STROKE);
            trackPaint.setStrokeWidth(8f);
            trackPaint.setPathEffect(new DashPathEffect(new float[]{15, 15}, 0));

            progressPaint = new Paint();
            progressPaint.setAntiAlias(true);
            progressPaint.setColor(Color.parseColor("#4DEEEA"));
            progressPaint.setStyle(Paint.Style.STROKE);
            progressPaint.setStrokeWidth(12f);
            progressPaint.setStrokeCap(Paint.Cap.ROUND);

            path = new Path();
        }

        @Override
        public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.onDraw(c, parent, state);
            int childCount = parent.getChildCount();
            if (childCount < 2) return;

            for (int i = 0; i < childCount - 1; i++) {
                View startView = parent.getChildAt(i);
                View endView = parent.getChildAt(i + 1);

                View startNode = startView.findViewById(R.id.card_node);
                View endNode = endView.findViewById(R.id.card_node);

                float startX = startView.getWidth() / 2f; // perfectly centered
                View startContainer = (View) startNode.getParent();
                float startY = startView.getTop() + startContainer.getTop() + startNode.getTop() + startNode.getHeight() / 2f;
                
                View endContainer = (View) endNode.getParent();
                float endY = endView.getTop() + endContainer.getTop() + endNode.getTop() + endNode.getHeight() / 2f;

                path.reset();
                path.moveTo(startX, startY);
                
                // Draw a direct vertical line down the center
                path.lineTo(startX, endY);

                // Draw the full track
                c.drawPath(path, trackPaint);

                // If both nodes are unlocked, draw progress
                int startPos = parent.getChildAdapterPosition(startView);
                if (startPos < currentLevel) {
                    c.drawPath(path, progressPaint);
                }
            }
        }
    }
}
