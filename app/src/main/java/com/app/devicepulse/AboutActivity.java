package com.app.devicepulse;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

public class AboutActivity extends AppCompatActivity {

    TextView versionText, privacyPolicy, terms, contactEmail, github;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        versionText = findViewById(R.id.versionText);
        privacyPolicy = findViewById(R.id.privacyPolicy);
        terms = findViewById(R.id.terms);
        contactEmail = findViewById(R.id.contactEmail);
        github = findViewById(R.id.github);

        versionText.setText(
                getString(R.string.version_text, BuildConfig.VERSION_NAME)
        );

        privacyPolicy.setOnClickListener(v ->
                openCustomTab(Constants.PRIVACY_POLICY));

        terms.setOnClickListener(v ->
                openCustomTab(Constants.TERMS_AND_CONDITIONS));

        github.setOnClickListener(v ->
                openCustomTab(Constants.GITHUB_PROFILE));

        contactEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:heaticdeveloper@gmail.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "DevicePulse Support");
            startActivity(intent);
        });
    }

    private void openCustomTab(String url) {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
        customTabsIntent.launchUrl(this, Uri.parse(url));
    }
}
