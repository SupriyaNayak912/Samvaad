package com.example.samvaad;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * SessionWorker simulates a background 'Data Sync' after a session concludes.
 * It simulates a 3-second delay for processing and then triggers a notification.
 */
public class SessionWorker extends Worker {

    public SessionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("SessionWorker", "Background sync started...");

        try {
            // Simulate 3-second data processing
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Log.e("SessionWorker", "Sync interrupted", e);
            return Result.retry();
        }

        Log.d("SessionWorker", "Background sync complete. Triggering notification.");

        // Trigger the notification
        NotificationHelper.showSessionCompleteNotification(getApplicationContext());

        return Result.success();
    }
}
