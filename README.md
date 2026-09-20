# English Coach

APK Android cá nhân để học tiếng Anh với lộ trình, từ vựng, cấu trúc câu, đọc hiểu và luyện nói.

## Build nhanh Demo Mode

Project đã giữ sẵn một toolchain portable trong `.toolchain/` trên máy phát triển. Nếu dùng Android Studio, mở thư mục này và để IDE dùng JDK 17+.

```powershell
$env:JAVA_HOME=(Resolve-Path '.toolchain\jdk17').Path
$env:ANDROID_SDK_ROOT=(Resolve-Path '.toolchain\android-sdk').Path
& '.toolchain\gradle-8.13\bin\gradle.bat' :app:testDebugUnitTest
& '.toolchain\gradle-8.13\bin\gradle.bat' :app:assembleRelease
```

APK release nằm ở `app/build/outputs/apk/release/app-release.apk`. Khi chưa có Firebase credentials, app chạy Demo Mode: đăng nhập demo, sinh bài cục bộ và chấm giọng lập trình sẵn để kiểm tra toàn bộ UI/offline flow.

## Bật Google Sign-In + Gemini thật

1. Tạo Firebase project và đăng ký Android app với package `com.linusv.englishcoach`.
2. Thêm SHA-1/SHA-256 của certificate dùng để ký APK vào Firebase Console.
3. Bật Firebase Authentication > Google.
4. Bật Firebase AI Logic và chọn Gemini Developer API.
5. Bật App Check > Play Integrity. Vì APK cài ngoài Play Store, không yêu cầu `PLAY_RECOGNIZED`/`LICENSED`; dùng mức `Device integrity`.
6. Tạo Web OAuth client ID để dùng với Credential Manager.
7. Copy `local.properties.example` thành `local.properties` và điền:

```properties
allowed.email=your-email@gmail.com
firebase.apiKey=...
firebase.appId=1:...:android:...
firebase.projectId=...
firebase.messagingSenderId=...
firebase.storageBucket=...
firebase.webClientId=...apps.googleusercontent.com
firebase.model=gemini-3.8-flash
```

Không commit `local.properties`, API credentials hoặc keystore. Firebase AI Logic giữ Gemini API key ở phía proxy Firebase; app không chứa Gemini API key riêng.

## Tính năng chính

- Onboarding CEFR A1–C1, mục tiêu, chủ đề, thời lượng và giọng US/UK.
- Gemini trả structured JSON cho từ vựng, IPA, nghĩa Việt, ví dụ, ngữ pháp, đoạn đọc và speaking prompts.
- Room lưu profile, bài học, tiến độ từ và speaking attempts trên thiết bị.
- Text-to-Speech cho cách đọc mẫu.
- Ghi AAC/M4A tạm trong cache, SpeechRecognizer lấy transcript, Gemini phân tích audio khi Firebase đã cấu hình.
- Điểm tổng: 40% content accuracy, 40% pronunciation, 20% fluency.
- Raw audio bị xóa sau khi chấm hoặc khi phiên kết thúc.

## Lưu ý phát hành APK cá nhân

Release hiện dùng signing mặc định của Gradle nếu chưa tạo keystore riêng, đủ để cài thử nhưng cần giữ nguyên certificate nếu muốn cập nhật đè APK sau này. Với bản dùng lâu dài, tạo keystore riêng ngoài repository và cấu hình `keystore.properties`.
