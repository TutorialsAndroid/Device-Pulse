package com.app.devicepulse;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StatFs;
import android.text.SpannableString;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "devicepulse_prefs";
    private static final String KEY_ACCEPTED = "privacy_accepted";

    private TextView cpuPercent, ramText, cpuTemp;
    private ProgressBar cpuProgress, ramProgress;

    private TextView storageText;
    private ProgressBar storageProgress;

    private final Handler handler = new Handler();
    private Runnable runnable;

    private long lastAppCpuTime = 0;
    private long lastTimeStamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Status bar spacing handled by padding top of the toolbar
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (view, insets) -> {
            Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            int topInset = statusBarInsets.top;
            view.setPadding(0, topInset, 0, 0);
            return insets;
        });

        checkPrivacyAcceptance();

        cpuPercent = findViewById(R.id.cpuPercent);
        ramText = findViewById(R.id.ramText);
        cpuTemp = findViewById(R.id.cpuTemp);
        cpuProgress = findViewById(R.id.cpuProgress);
        ramProgress = findViewById(R.id.ramProgress);
        storageText = findViewById(R.id.storageText);
        storageProgress = findViewById(R.id.storageProgress);

        toolbar.setSubtitle(Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")");
    }

    @Override
    protected void onResume() {
        super.onResume();
        startMonitoring();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopMonitoring();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(MainActivity.this, AboutActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void checkPrivacyAcceptance() {

        android.content.SharedPreferences prefs =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean accepted = prefs.getBoolean(KEY_ACCEPTED, false);

        if (!accepted) {
            showPrivacyDialog();
        }
    }

    private void showPrivacyDialog() {

        SpannableString spannable = getSpannableString();

        androidx.appcompat.app.AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(R.string.privacyDialogTitle)
                        .setMessage(spannable)
                        .setCancelable(false)
                        .setPositiveButton(R.string.privacyDialogPButton, (d, which) -> {

                            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                    .edit()
                                    .putBoolean(KEY_ACCEPTED, true)
                                    .apply();

                            d.dismiss();
                        })
                        .setNegativeButton(R.string.privacyDialogNButton, (d, which) -> {
                            finish();
                        })
                        .create();

        dialog.show();

        ((TextView) dialog.findViewById(android.R.id.message))
                .setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    @NonNull
    private SpannableString getSpannableString() {
        String message = getString(R.string.privacyDialogMessage);

        SpannableString spannable =
                new SpannableString(message);

        int privacyStart = message.indexOf("Privacy Policy");
        int privacyEnd = privacyStart + "Privacy Policy".length();

        int termsStart = message.indexOf("Terms & Conditions");
        int termsEnd = termsStart + "Terms & Conditions".length();

        spannable.setSpan(new android.text.style.ClickableSpan() {
                              @Override
                              public void onClick(@NonNull android.view.View widget) {
                                  openCustomTab(Constants.PRIVACY_POLICY);
                              }
                          }, privacyStart, privacyEnd,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannable.setSpan(new android.text.style.ClickableSpan() {
                              @Override
                              public void onClick(@NonNull android.view.View widget) {
                                  openCustomTab(Constants.TERMS_AND_CONDITIONS);
                              }
                          }, termsStart, termsEnd,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    private void openCustomTab(String url) {
        androidx.browser.customtabs.CustomTabsIntent customTabsIntent =
                new androidx.browser.customtabs.CustomTabsIntent.Builder().build();
        customTabsIntent.launchUrl(this, android.net.Uri.parse(url));
    }

    private void startMonitoring() {
        runnable = new Runnable() {
            @Override
            public void run() {
                updateCPUUsage();
                updateRAM();
                updateStorage();
                updateBatteryTemp();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);
    }

    private void stopMonitoring() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        lastAppCpuTime = 0;
        lastTimeStamp = 0;
    }

    private void updateCPUUsage() {

        long appCpuTime = android.os.Process.getElapsedCpuTime();
        long timeNow = System.currentTimeMillis();

        if (lastTimeStamp != 0) {

            long cpuDiff = appCpuTime - lastAppCpuTime;
            long timeDiff = timeNow - lastTimeStamp;

            if (timeDiff > 0) {

                float cpuUsage = (cpuDiff / (float) timeDiff) * 100f;

                if (cpuUsage > 100) cpuUsage = 100;
                if (cpuUsage < 0) cpuUsage = 0;

                cpuProgress.setProgress((int) cpuUsage);
                cpuPercent.setText(String.format(java.util.Locale.US, "%.1f%% Used", cpuUsage));
            }
        }

        lastAppCpuTime = appCpuTime;
        lastTimeStamp = timeNow;
    }


    private void updateRAM() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        long totalMem = memoryInfo.totalMem;
        long availMem = memoryInfo.availMem;
        long usedMem = totalMem - availMem;

        int percentUsed = (int) ((usedMem * 100) / totalMem);

        ramProgress.setProgress(percentUsed);
        ramText.setText(String.format(java.util.Locale.US, "%d%% Used", percentUsed));
    }

    private void updateStorage() {

        StatFs statFs = new StatFs(getFilesDir().getAbsolutePath());

        long totalBytes = statFs.getTotalBytes();
        long freeBytes = statFs.getAvailableBytes();
        long usedBytes = totalBytes - freeBytes;

        int percentUsed = (int) ((usedBytes * 100) / totalBytes);

        storageProgress.setProgress(percentUsed);

        float used = usedBytes / (1024f * 1024f * 1024f);
        float total = totalBytes / (1024f * 1024f * 1024f);

        storageText.setText(String.format(java.util.Locale.US, "%d%% Used (%.2f GB / %.2f GB)", percentUsed, used, total));
    }

    private void updateBatteryTemp() {

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, intentFilter);

        if (batteryStatus != null) {
            int temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            float tempC = temp / 10f;
            cpuTemp.setText(String.format(java.util.Locale.US, "%.1f °C", tempC));
        } else {
            cpuTemp.setText("N/A");
        }
    }
}