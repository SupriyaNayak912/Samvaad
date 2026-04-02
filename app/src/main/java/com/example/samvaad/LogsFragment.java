package com.example.samvaad;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogsFragment extends Fragment {

    private LinearLayout logsContainer;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logs, container, false);
        logsContainer = view.findViewById(R.id.logs_container);
        db = FirebaseFirestore.getInstance();

        fetchLogs();

        return view;
    }

    private void fetchLogs() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("activity_logs")
                .whereEqualTo("uid", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        logsContainer.removeAllViews();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            addLogToUi(document);
                        }
                    } else {
                        Log.e("LogsFragment", "Error getting documents: ", task.getException());
                    }
                });
    }

    private void addLogToUi(QueryDocumentSnapshot doc) {
        View logView = getLayoutInflater().inflate(R.layout.item_log, logsContainer, false);
        
        TextView tvTitle = logView.findViewById(R.id.tv_log_title);
        TextView tvDesc = logView.findViewById(R.id.tv_log_desc);
        TextView tvTime = logView.findViewById(R.id.tv_log_time);

        tvTitle.setText(doc.getString("event_type"));
        tvDesc.setText("Screen: " + doc.getString("screen_name"));
        
        long timestamp = doc.getLong("timestamp");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        tvTime.setText(sdf.format(new Date(timestamp)));

        logsContainer.addView(logView);
    }
}