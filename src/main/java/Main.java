import com.google.gson.*;
import okhttp3.*;
import org.kohsuke.github.*;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final OkHttpClient http = new OkHttpClient.Builder()
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    public static void main(String[] args) throws Exception {
        String token     = env("GITHUB_TOKEN");
        String geminiKey = env("GEMINI_API_KEY");
        String eventPath = env("GITHUB_EVENT_PATH");

        JsonObject event   = JsonParser.parseReader(new FileReader(eventPath)).getAsJsonObject();
        JsonObject issue   = event.getAsJsonObject("issue");
        String repoFull    = event.getAsJsonObject("repository").get("full_name").getAsString();
        int prNumber       = issue.get("number").getAsInt();

        GitHub github     = new GitHubBuilder().withOAuthToken(token).build();
        GHRepository repo = github.getRepository(repoFull);
        GHPullRequest pull = repo.getPullRequest(prNumber);
        String branch     = pull.getHead().getRef();

        System.out.println("🔍 PR #" + prNumber + " → برنچ: " + branch);

        List<String> javaFiles = new ArrayList<>();
        for (GHPullRequestFileDetail f : pull.listFiles()) {
            if (f.getFilename().endsWith(".java") && !"removed".equals(f.getStatus())) {
                javaFiles.add(f.getFilename());
            }
        }

        if (javaFiles.isEmpty()) {
            pull.comment("🤖 هیچ فایل `.java` تغییر یافته‌ای پیدا نشد.");
            return;
        }

        System.out.println("📁 فایل‌ها: " + javaFiles);

        Map<String, String> changes = new LinkedHashMap<>();
        for (String path : javaFiles) {
            try {
                String original = Files.readString(Path.of(path), StandardCharsets.UTF_8);
                String fixed = fixWithGemini(original, path, geminiKey);

                if (!fixed.equals(original)) {
                    Files.writeString(Path.of(path), fixed, StandardCharsets.UTF_8);
                    changes.put(path, fixed);
                    System.out.println("✅ اصلاح شد: " + path);
                } else {
                    System.out.println("⏭️ تغییری لازم نبود: " + path);
                }
            } catch (Exception e) {
                System.err.println("❌ خطا در " + path + ": " + e.getMessage());
            }
        }

        if (changes.isEmpty()) {
            pull.comment("🤖 بررسی شد. هیچ تغییری لازم نبود. ✅");
            return;
        }

        run("git", "config", "user.name", "AI Code Fixer");
        run("git", "config", "user.email", "ai-fixer@users.noreply.github.com");
        run("git", "add", ".");
        run("git", "commit", "-m", "🤖 اصلاح خودکار کد توسط AI");
        run("git", "push");

        StringBuilder files = new StringBuilder();
        changes.keySet().forEach(f -> files.append("- `").append(f).append("`\n"));

        pull.comment("## 🤖 ربات اصلاح کد اجرا شد\n\n"
                + "**تعداد فایل‌های اصلاح‌شده:** " + changes.size() + "\n\n"
                + files + "\n---\n"
                + "_این کامیت به صورت خودکار توسط AI ساخته شد. لطفاً Review کن._");

        System.out.println("🎉 تمام شد!");
    }

    private static String fixWithGemini(String code, String path, String key) throws Exception {
        String prompt = """
                تو یه برنامه‌نویس Java حرفه‌ای هستی.
                این فایل رو اصلاح کن:
                - باگ‌ها و خطاها
                - بهینه‌سازی کارایی
                - خوانایی بهتر
                - رفع مشکلات امنیتی

                ⚠️ فقط کد کامل و اصلاح‌شده رو برگردون، بدون توضیح اضافه.
                ⚠️ اگه چیزی برای اصلاح نیست، دقیقاً همون کد رو برگردون.

                نام فایل: %s

                ```
                %s
                ```
                """.formatted(path, code);

        String aiResponse = callGemini(prompt, key);
        return extractCode(aiResponse);
    }

    private static String callGemini(String prompt, String key) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + "gemini-2.0-flash:generateContent?key=" + key;

        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        JsonArray parts = new JsonArray(); parts.add(part);
        JsonObject content = new JsonObject(); content.add("parts", parts);
        JsonArray contents = new JsonArray(); contents.add(content);
        JsonObject body = new JsonObject(); body.add("contents", contents);

        Request req = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toString(),
                        MediaType.parse("application/json")))
                .build();

        try (Response res = http.newCall(req).execute()) {
            String respBody = res.body().string();
            if (!res.isSuccessful()) throw new RuntimeException("Gemini: " + respBody);

            return JsonParser.parseString(respBody).getAsJsonObject()
                    .getAsJsonArray("candidates").get(0).getAsJsonObject()
                    .getAsJsonObject("content").getAsJsonArray("parts")
                    .get(0).getAsJsonObject().get("text").getAsString();
        }
    }

    private static String extractCode(String aiResponse) {
        int start = aiResponse.indexOf("```");
        if (start == -1) return aiResponse.trim();
        int codeStart = aiResponse.indexOf('\n', start) + 1;
        int end = aiResponse.indexOf("```", codeStart);
        if (end == -1) return aiResponse.substring(codeStart).trim();
        return aiResponse.substring(codeStart, end).trim();
    }

    private static String env(String name) {
        String v = System.getenv(name);
        if (v == null || v.isEmpty()) {
            System.err.println("❌ متغیر محیطی " + name + " تنظیم نشده");
            System.exit(1);
        }
        return v;
    }

    private static void run(String... cmd) throws Exception {
        Process p = new ProcessBuilder(cmd).inheritIO().start();
        if (p.waitFor() != 0) throw new RuntimeException("دستور شکست خورد: " + String.join(" ", cmd));
    }
}
