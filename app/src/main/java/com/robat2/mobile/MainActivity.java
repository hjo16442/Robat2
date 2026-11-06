package com.robat2.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private EditText repoInput, prInput, tokenInput;
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF10243A);
        getWindow().setNavigationBarColor(0xFF10243A);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(24, 28, 24, 24);
        page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        page.setBackgroundColor(0xFFF4F7FB);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(content);

        TextView title = new TextView(this);
        title.setText("ربات ۲");
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(0xFF10243A);
        content.addView(title);

        TextView intro = new TextView(this);
        intro.setText("اجرای اصلاح خودکار کد در Pull Request گیت‌هاب\n\nاطلاعات مخزن و شماره PR را وارد کن. برنامه فرمان /fix را به گفت‌وگوی همان PR می‌فرستد تا GitHub Actions موجود در مخزن اجرا شود.");
        intro.setTextSize(15);
        intro.setTextColor(0xFF334155);
        intro.setPadding(0, 12, 0, 20);
        content.addView(intro);

        repoInput = field(content, "نام مخزن (مثلاً hjo16442/Robat2)");
        prInput = field(content, "شماره Pull Request");
        tokenInput = field(content, "GitHub Personal Access Token");
        tokenInput.setInputType( textPasswordType() );

        Button run = new Button(this);
        run.setText("ارسال فرمان اصلاح کد");
        content.addView(run);
        status = new TextView(this);
        status.setText("آماده");
        status.setTextSize(15);
        status.setTextColor(0xFF0F5132);
        status.setPadding(0, 20, 0, 0);
        content.addView(status);
        TextView note = new TextView(this);
        note.setText("\nامنیت: توکن فقط برای همین درخواست در حافظه استفاده می‌شود و در برنامه ذخیره نمی‌شود. برای توکن، دسترسی Issues: Read and write مخزن را فعال کن. مخزن باید workflow اصلاح کد را داشته باشد.");
        note.setTextSize(13);
        note.setTextColor(0xFF64748B);
        note.setPadding(0, 18, 0, 0);
        content.addView(note);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(page);

        run.setOnClickListener(v -> submit());
    }

    private int textPasswordType() { return 129; }

    private EditText field(LinearLayout parent, String hint) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint(hint);
        e.setTextSize(15);
        e.setPadding(14, 10, 14, 10);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = 12;
        parent.addView(e, lp);
        return e;
    }

    private void submit() {
        String repo = repoInput.getText().toString().trim();
        String pr = prInput.getText().toString().trim();
        String token = tokenInput.getText().toString().trim();
        if (!repo.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+") || pr.isEmpty() || token.isEmpty()) {
            status.setText("نام مخزن، شماره PR و توکن را درست وارد کن.");
            return;
        }
        status.setText("در حال ارسال درخواست...");
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                URL url = new URL("https://api.github.com/repos/" + repo + "/issues/" + pr + "/comments");
                c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(15000);
                c.setReadTimeout(20000);
                c.setRequestProperty("Accept", "application/vnd.github+json");
                c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
                c.setRequestProperty("Authorization", "Bearer " + token);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                c.setDoOutput(true);
                JSONObject body = new JSONObject();
                body.put("body", "/fix");
                try (OutputStream os = c.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }
                int code = c.getResponseCode();
                InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
                StringBuilder response = new StringBuilder();
                if (stream != null) {
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
                        String line;
                        while ((line = r.readLine()) != null) response.append(line);
                    }
                }
                final String message = code >= 200 && code < 300
                    ? "فرمان /fix با موفقیت برای PR ارسال شد. اجرای GitHub Actions را در بخش Actions مخزن بررسی کن."
                    : "ارسال ناموفق بود (HTTP " + code + "). توکن و دسترسی Issues را بررسی کن. " + response;
                runOnUiThread(() -> status.setText(message));
            } catch (Exception ex) {
                runOnUiThread(() -> status.setText("خطا در اتصال: " + ex.getMessage()));
            } finally {
                if (c != null) c.disconnect();
            }
        }).start();
    }
}
