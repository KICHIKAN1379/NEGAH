# نگاه مربی (Negah Coach)

اپلیکیشن آفلاین اندرویدی برای مدیریت حلقه تربیتی توسط مربی.

## وضعیت پروژه

پروژه به‌صورت مستقل و از پایه توسعه داده می‌شود. راهنمای جامع AK1 فقط مرجع قوانین و رفتار محصول است؛ کد یا مخزن قدیمی وابستگی این پروژه نیست.

## معماری و فناوری

- Kotlin 2.2.21 + Jetpack Compose
- Multi-module Gradle
- Clean Architecture + UDF
- Hilt برای Dependency Injection
- Room / SQLite برای ذخیره‌سازی محلی
- WorkManager برای پشتیبان خودکار: نیاز فنی ثبت‌شده، اما هنوز پیاده‌سازی نشده است.
- پشتیبان‌گیری رمزگذاری‌شده با حداکثر دو نسخه در پوشه MRT: الزام محصول، هنوز پیاده‌سازی نشده است.

## دامنه نسخه فعلی

اپلیکیشن مربی، آفلاین‌محور و حلقه‌محور است. حساب ابری، همگام‌سازی، backend و اپ عضو خارج از دامنه این نسخه هستند.

## اصول توسعه

1. هر قابلیت با تست و کنترل کیفیت تحویل می‌شود.
2. UI مستقیماً به دیتابیس یا منطق کسب‌وکار وابسته نمی‌شود.
3. Migrationها باید داده‌های موجود را حفظ کنند.
4. عملیات حساس باید اتمیک و قابل ردیابی باشند.
5. هر تغییر معماری باید در مرز ماژول روشن انجام شود.
6. موفقیت Build، تست واحد، اجرای دستی و تطبیق بصری چهار ادعای جداگانه‌اند.

## مستندات پروژه

- معماری: docs/ARCHITECTURE.md
- مشخصات محصول: docs/PRODUCT_SPEC.md
- نقشه UX: docs/UX_REFERENCE_MAP.md
- برنامه پیاده‌سازی: docs/IMPLEMENTATION_PLAN.md
- وضعیت مأموریت و گزارش پیشرفت: docs/PROGRESS.md
- وضعیت پیاده‌سازی قبلی: docs/IMPLEMENTATION_STATUS.md

## Build

پیکربندی واقعی مخزن در حال حاضر:

- JDK: 17
- Gradle مورد استفاده CI: 8.13
- Android Gradle Plugin: 8.13.2
- Kotlin: 2.2.21
- Compile SDK / Target SDK: Android 36
- Build Tools: 36.0.0
- Min SDK: 26

محدودیت فعلی: فایل gradle/wrapper/gradle-wrapper.properties وجود دارد، اما اسکریپت‌های gradlew و gradlew.bat و فایل gradle-wrapper.jar در مخزن نیستند. تا زمان بازگرداندن Wrapper، برای ساخت محلی باید Gradle 8.13 نصب باشد.

فرمان ساخت APK:
    gradle :app:assembleDebug

فرمان تست‌های فعلی CI:
    gradle :app:assembleDebug :domain:core:test :data:local:testDebugUnitTest :feature:members:testDebugUnitTest :feature:refinery:testDebugUnitTest --stacktrace

خروجی Debug APK:
    app/build/outputs/apk/debug/app-debug.apk

آخرین baseline تأییدشده در CI: run #78 روی commit 0529bb79e99e7675f63cb574fa4efa615eac8c62. این نسخه تنظیم روزهای کلاس را نیز دارد. نتیجه فقط Build و تست‌های مشخص‌شده در workflow را تأیید می‌کند؛ تست UI، اجرای دستگاه و تطبیق بصری هنوز جداگانه لازم‌اند.
