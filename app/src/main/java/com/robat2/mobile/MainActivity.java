package com.robat2.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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
    private Button runButton;

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
        intro.setText("دستیار اصلاح کد با هوش مصنوعی\n\nاطلاعات مخزن و شماره Pull Request را وارد کن. ربات فرمان /fix را می‌فرستد؛ موتور هوش مصنوعی در GitHub Actions کدهای Java تغییرکرده را بررسی و اصلاح می‌کند و فقط پس از موفقیت تست‌های پروژه، تغییرات را ثبت می‌کند.");
        intro.setTextSize(15);
        intro.setTextColor(0xFF334155);
        intro.setPadding(0, 12, 0, 20);
        content.addView(intro);

        repoInput = field(content, "نام مخزن (مثلاً hjo16442/Robat2)");
        prInput = field(content, "شماره Pull Request");
        tokenInput = field(content, "GitHub Personal Access Token");
        tokenInput.setInputType(129);

        runButton = new Button(this);
        runButton.setText("شروع بررسی و اصلاح کد");
        content.addView(runButton);

        Button actionsButton = new Button(this);
        actionsButton.setText("مشاهده اجرای ربات در GitHub");
        content.addView(actionsButton);

        status = new TextView(this);
        status.setText("آماده");
        status.setTextSize(15);
        status.setTextColor(0xFF0F5132);
        status.setPadding(0, 20, 0, 0);
        content.addView(status);

        TextView note = new TextView(this);
        note.setText("\nراه‌اندازی یک‌باره: در GitHub بخش Settings → Secrets and variables → Actions یک Secret با نام GEMINI_API_KEY بساز و کلید Google AI Studio را در آن قرار بده. در Workflow permissions نیز Read and write را فعال کن. توکن برنامه باید دسترسی Issues: Read and write داشته باشد. توکن در برنامه ذخیره نمی‌شود.");
        note.setTextSize(13);
        note.setTextColor(0xFF64748B);
        note.setPadding(0, 18, 0, 0);
        content.addView(note);

        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(page);

        runButton.setOnClickListener(v -> submit());
        actionsButton.setOnClickListener(v -> openActions());
    }

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

    private void openActions() {
        String repo = repoInput.getText().toString().trim();
        if (!repo.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
            status.setText("ابتدا نام مخزن را به شکل owner/repository وارد کن.");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/" + repo + "/actions"));
        startActivity(intent);
    }

    private void submit() {
        String repo = repoInput.getText().toString().trim();
        String pr = prInput.getText().toString().trim();
        String token = tokenInput.getText().toString().trim();

        if (!repo.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")
                || !pr.matches("[0-9]+") || token.isEmpty()) {
            status.setText("نام مخزن، شماره عددی PR و توکن را درست وارد کن.");
            return;
        }

        runButton.setEnabled(false);
        status.setText("در حال ارسال درخواست به GitHub...");
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                URL url = new URL("https://api.github.com/repos/" + repo
                        + "/issues/" + pr + "/comments");
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
                InputStream stream = code >= 200 && code < 300
                        ? c.getInputStream() : c.getErrorStream();
                if (stream != null) {
                    try (BufferedReader r = new BufferedReader(
                            new InputStreamReader(stream, "UTF-8"))) {
                        while (r.readLine() != null) { /* consume response */ }
                    }
                }

                final String message = code >= 200 && code < 300
                        ? "درخواست اصلاح با موفقیت ثبت شد. اجرای AI Code Fixer را در GitHub Actions ببین. اگر GEMINI_API_KEY تنظیم نشده باشد، اصلاح انجام نمی‌شود."
                        : "ارسال ناموفق بود (HTTP " + code + "). مجوز Issues: Read and write توکن و شماره PR را بررسی کن.";
                runOnUiThread(() -> {
                    status.setText(message);
                    tokenInput.setText("");
                    runButton.setEnabled(true);
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    status.setText("خطا در اتصال به GitHub: " + ex.getMessage());
                    tokenInput.setText("");
                    runButton.setEnabled(true);
                });
            } finally {
                if (c != null) c.disconnect();
            }
        }).start();
    }
}
