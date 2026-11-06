# ربات ۲ (Robat2)

ربات ۲ یک ابزار اندرویدی برای ارسال فرمان اصلاح کد به GitHub Actions است. موتور اصلاح کد در سمت GitHub اجرا می‌شود و از Gemini API برای پیشنهاد تغییرات استفاده می‌کند.

## قابلیت‌ها
- ارسال فرمان `/fix` برای یک Pull Request از برنامه اندروید.
- بررسی فایل‌های Java تغییرکرده در PR.
- پیشنهاد اصلاح با Gemini 3.8 Flash.
- اجرای تست پروژه Maven یا Gradle پیش از ثبت تغییرات.
- فقط پس از موفقیت اعتبارسنجی، commit و push روی شاخه همان PR.
- جلوگیری از اصلاح PRهای Fork و فایل‌های خارج از مخزن.

## راه‌اندازی ضروری برای موتور هوش مصنوعی
در GitHub وارد مخزن شوید:
**Settings → Secrets and variables → Actions → New repository secret**

یک secret با نام دقیق زیر بسازید:
- `GEMINI_API_KEY`: کلید API از Google AI Studio.

اختیاری:
- `GEMINI_MODEL`: شناسه مدل؛ مقدار پیش‌فرض `gemini-3.8-flash` است.

بدون `GEMINI_API_KEY`، فرمان `/fix` ارسال می‌شود اما موتور هوش مصنوعی نمی‌تواند اجرا شود.

## مجوزهای GitHub Actions
در **Settings → Actions → General → Workflow permissions** گزینه **Read and write permissions** را فعال کنید تا workflow بتواند تغییرات معتبر را به شاخه PR push کند.

## استفاده از برنامه اندروید
1. یک GitHub Personal Access Token بسازید که روی مخزن موردنظر مجوز **Issues: Read and write** داشته باشد.
2. نام مخزن را به شکل `owner/repository` وارد کنید.
3. شماره Pull Request و توکن را در برنامه وارد کنید.
4. دکمه ارسال فرمان اصلاح را بزنید.
5. اجرای workflow را در زبانه Actions بررسی کنید.

توکن GitHub فقط برای همان درخواست در حافظه برنامه استفاده می‌شود و در برنامه ذخیره نمی‌شود. توکن یا کلید Gemini را در پیام‌ها یا کد عمومی قرار ندهید.

## محدودیت‌های فعلی
- موتور اصلاح فعلاً فقط فایل‌های Java تغییرکرده در PR را ویرایش می‌کند.
- فقط پروژه‌هایی که دستور تست Maven یا Gradle شناخته‌شده دارند اجازه push خودکار می‌گیرند.
- اصلاح تولیدشده تضمیناً بی‌نقص نیست؛ diff و نتیجه تست‌ها را پیش از Merge بازبینی کنید.
- نسخه اندروید فعلی فرمان را ارسال می‌کند؛ پردازش AI روی GitHub Actions انجام می‌شود، نه روی خود گوشی.

## ساخت APK
از GitHub Actions، workflow با نام **Build Android APK** را اجرا کنید. پس از موفقیت، artifact با نام `Robat2-Android-APK` در همان اجرای workflow قابل دریافت است.
