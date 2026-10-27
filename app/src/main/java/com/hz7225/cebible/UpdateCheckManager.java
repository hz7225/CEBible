package com.hz7225.cebible;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateCheckManager {

    private static final String TAG = "UpdateCheckManager";
    private static boolean hasPromptedThisSession = false;
    
    // ◄── THIS IS THE CONSTANT WHERE THE LAUNCH DELAY IS SET (Change value here!) ──►
    public static final long LAUNCH_CHECK_DELAY_MS = 60 * 1000L; // 5 seconds for testing (change to 60 * 1000L for 1 min)

    private final Activity mActivity;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public UpdateCheckManager(Activity activity) {
        this.mActivity = activity;
    }

    public void checkVersionOnLaunch() {
        if (mActivity == null || mActivity.isFinishing()) return;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1 && mActivity.isDestroyed()) return;
        if (hasPromptedThisSession) return;

        Log.d(TAG, "checkVersionOnLaunch: Triggered in " + mActivity.getClass().getSimpleName() + ". Version=" + BuildConfig.VERSION_NAME);
        Log.d(TAG, "Scheduling background update check to run in " + (LAUNCH_CHECK_DELAY_MS / 1000) + " seconds...");

        // Delay starting the background check after launch
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mActivity == null || mActivity.isFinishing()) return;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1 && mActivity.isDestroyed()) return;
                if (hasPromptedThisSession) return;
                executeBackgroundVersionCheck();
            }
        }, LAUNCH_CHECK_DELAY_MS);
    }

    private void executeBackgroundVersionCheck() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                final String latestVersion = fetchLatestVersionFromPlayStore();
                final String currentVersion = BuildConfig.VERSION_NAME;

                Log.d(TAG, "Background Check Finished -> Current Version: " + currentVersion + ", Play Store Version: " + latestVersion);

                if (latestVersion != null && hasMajorOrMinorUpdate(latestVersion, currentVersion)) {
                    Log.d(TAG, "Major/Minor update detected (" + latestVersion + " > " + currentVersion + "). Posting update dialog to UI thread...");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            showUpdatePromptDialog(latestVersion);
                        }
                    });
                } else {
                    Log.d(TAG, "No Major/Minor update needed for installed version (" + currentVersion + ") vs Play Store (" + latestVersion + ").");
                }
            }
        });
    }

    private String fetchLatestVersionFromPlayStore() {
        HttpURLConnection conn = null;
        try {
            String packageName = mActivity.getPackageName();
            String playStoreUrl = "https://play.google.com/store/apps/details?id=" + packageName + "&hl=en";
            Log.d(TAG, "Fetching Play Store URL: " + playStoreUrl);

            URL url = new URL(playStoreUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "Google Play Store HTTP Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                String html = sb.toString();
                Log.d(TAG, "HTML fetched successfully. Length: " + html.length() + " chars.");

                String highestVersion = null;

                // Pattern 1: Look for double/triple bracket arrays e.g. [["4.3.3"]] or [[["4.3.3"]]]
                Pattern pattern1 = Pattern.compile("\\[\\[\\[?\"(\\d+\\.\\d+\\.\\d+)\"\\]\\]\\]?");
                Matcher matcher1 = pattern1.matcher(html);
                while (matcher1.find()) {
                    String candidate = matcher1.group(1);
                    Log.d(TAG, "Pattern1 candidate found: " + candidate);
                    int major = parseInteger(candidate.split("\\.")[0]);
                    if (major <= 10) { // Filter out system library versions like 24.0.0
                        if (highestVersion == null || isVersionNewer(candidate, highestVersion)) {
                            highestVersion = candidate;
                        }
                    }
                }

                if (highestVersion != null) {
                    Log.d(TAG, "Highest Version found via Pattern1: " + highestVersion);
                    return highestVersion;
                }

                // Pattern 2: Fallback scan all quoted version strings "4.3.3"
                Pattern pattern2 = Pattern.compile("\"(\\d+\\.\\d+\\.\\d+)\"");
                Matcher matcher2 = pattern2.matcher(html);
                while (matcher2.find()) {
                    String candidate = matcher2.group(1);
                    int major = parseInteger(candidate.split("\\.")[0]);
                    if (major <= 10) {
                        if (highestVersion == null || isVersionNewer(candidate, highestVersion)) {
                            highestVersion = candidate;
                        }
                    }
                }

                Log.d(TAG, "Highest Version found via Pattern2: " + highestVersion);
                return highestVersion;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching Play Store version", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    private boolean hasMajorOrMinorUpdate(String latestStr, String currentStr) {
        if (latestStr == null || currentStr == null) return false;

        String[] latestParts = latestStr.split("\\.");
        String[] currentParts = currentStr.split("\\.");

        int latestMajor = latestParts.length > 0 ? parseInteger(latestParts[0]) : 0;
        int currentMajor = currentParts.length > 0 ? parseInteger(currentParts[0]) : 0;

        if (latestMajor > currentMajor) {
            return true;
        } else if (latestMajor < currentMajor) {
            return false;
        }

        int latestMinor = latestParts.length > 1 ? parseInteger(latestParts[1]) : 0;
        int currentMinor = currentParts.length > 1 ? parseInteger(currentParts[1]) : 0;

        return latestMinor > currentMinor;
    }

    private boolean isVersionNewer(String latestStr, String currentStr) {
        if (latestStr == null || currentStr == null) return false;

        String[] latestParts = latestStr.split("\\.");
        String[] currentParts = currentStr.split("\\.");

        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int latestNum = i < latestParts.length ? parseInteger(latestParts[i]) : 0;
            int currentNum = i < currentParts.length ? parseInteger(currentParts[i]) : 0;

            if (latestNum > currentNum) return true;
            if (latestNum < currentNum) return false;
        }
        return false;
    }

    private int parseInteger(String str) {
        try {
            return Integer.parseInt(str.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private void showUpdatePromptDialog(final String latestVersion) {
        if (mActivity == null || mActivity.isFinishing()) return;
        if (mActivity.isDestroyed()) return;
        if (hasPromptedThisSession) return;

        hasPromptedThisSession = true;

        LayoutInflater inflater = mActivity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_update_prompt, null);

        TextView tvTitle = (TextView) dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = (TextView) dialogView.findViewById(R.id.tv_dialog_message);
        Button btnRemindLater = (Button) dialogView.findViewById(R.id.btn_remind_later);
        Button btnUpdateNow = (Button) dialogView.findViewById(R.id.btn_update_now);

        if (tvTitle != null) {
            tvTitle.setText(R.string.update_available_title);
        }
        if (tvMessage != null) {
            tvMessage.setText(mActivity.getString(R.string.update_available_msg, latestVersion));
        }

        final PopupWindow updatePopupWindow = new PopupWindow(dialogView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        updatePopupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        View rootOverlay = dialogView.findViewById(R.id.update_popup_root);
        if (rootOverlay != null) {
            rootOverlay.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        }

        if (btnUpdateNow != null) {
            btnUpdateNow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updatePopupWindow.dismiss();
                    openPlayStorePage();
                }
            });
        }

        if (btnRemindLater != null) {
            btnRemindLater.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updatePopupWindow.dismiss();
                }
            });
        }

        updatePopupWindow.showAtLocation(mActivity.getWindow().getDecorView(), Gravity.CENTER, 0, 0);
    }

    private void openPlayStorePage() {
        if (mActivity == null) return;
        String packageName = mActivity.getPackageName();
        try {
            mActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
        } catch (ActivityNotFoundException e) {
            mActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }
}
