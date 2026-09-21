# Language Coach

APK Android cá nhân để học ngoại ngữ bằng tiếng Việt với lộ trình, từ vựng, cấu trúc câu, đọc hiểu và luyện nói. Hiện hỗ trợ tiếng Anh, Trung giản thể/phồn thể, Nhật và Hàn.

## Build nhanh Demo Mode

Project đã giữ sẵn một toolchain portable trong `.toolchain/` trên máy phát triển. Nếu dùng Android Studio, mở thư mục này và để IDE dùng JDK 17+.

```powershell
$env:JAVA_HOME=(Resolve-Path '.toolchain\jdk17').Path
$env:ANDROID_SDK_ROOT=(Resolve-Path '.toolchain\android-sdk').Path
& '.toolchain\gradle-8.13\bin\gradle.bat' :app:testDebugUnitTest
& '.toolchain\gradle-8.13\bin\gradle.bat' :app:assembleRelease
```

APK release nằm ở `app/build/outputs/apk/release/LangV-1.3.apk`. Khi chưa có Firebase credentials, app chạy Demo Mode: đăng nhập demo, sinh bài cục bộ và chấm giọng lập trình sẵn để kiểm tra toàn bộ UI/offline flow.

## Bật Google Sign-In + Gemini thật

1. Tạo Firebase project và đăng ký Android app với package `com.linusv.englishcoach`.
2. Tải `google-services.json` từ Firebase Console và đặt tại `app/google-services.json`. Xem `app/google-services.json.example` để biết cấu trúc cần có; file thật bị loại khỏi Git và không được commit.
3. Thêm SHA-1/SHA-256 của certificate dùng để ký APK vào Firebase Console.
4. Bật Firebase Authentication > Google.
5. Bật Firebase AI Logic và chọn Gemini Developer API.
6. Bật App Check > Play Integrity. Vì APK cài ngoài Play Store, không yêu cầu `PLAY_RECOGNIZED`/`LICENSED`; dùng mức `Device integrity`.
7. Tạo Web OAuth client ID để Credential Manager sinh `default_web_client_id`.
8. Copy `local.properties.example` thành `local.properties` và chỉ điền:

```properties
allowed.email=your-email@gmail.com
firebase.model=gemini-3.8-flash
firebase.appCheckDebug=false
```

Không commit `local.properties`, API credentials hoặc keystore. Firebase AI Logic giữ Gemini API key ở phía proxy Firebase; app không chứa Gemini API key riêng.

### Sửa lỗi App Check khi cài APK thử nghiệm trực tiếp

Nếu APK chưa được phân phối qua Google Play, có thể dùng Debug App Check cho bản thử nghiệm cá nhân:

1. Đặt `firebase.appCheckDebug=true` trong `local.properties`, rồi build và cài lại APK.
2. Mở Logcat và tìm `DebugAppCheckProvider` / `Enter this debug secret`.
3. Vào Firebase Console > App Check > Apps > menu của app > Manage debug tokens và đăng ký token vừa hiện.
4. Buộc dừng rồi mở lại app trước khi tạo bài học.

Không chia sẻ APK được build với Debug App Check và không commit debug token. Với bản phát hành thật, giữ `firebase.appCheckDebug=false`, đăng ký SHA-256 của certificate ký APK, dùng Play Integrity và cấu hình theo kênh phân phối. Nếu chỉ phát hành ngoài Google Play, không yêu cầu `PLAY_RECOGNIZED`/`LICENSED` và chọn mức `Device integrity`.

## Tính năng chính

- Onboarding chọn ngôn ngữ đích, cấp độ theo CEFR/HSK/TOCFL/JLPT/TOPIK, mục tiêu, chủ đề, thời lượng và giọng đọc.
- Gemini trả structured JSON cho từ vựng, cách phát âm/romanization, nghĩa Việt, ví dụ, ngữ pháp, đoạn đọc và speaking prompts.
- Room lưu track độc lập cho từng ngôn ngữ; migration 1→2 giữ lại dữ liệu tiếng Anh cũ.
- Text-to-Speech và SpeechRecognizer dùng locale tương ứng (`en-US`, `zh-CN`, `zh-TW`, `ja-JP`, `ko-KR`).
- Ghi AAC/M4A tạm trong cache, SpeechRecognizer lấy transcript, Gemini phân tích audio khi Firebase đã cấu hình.
- Điểm tổng: 40% content accuracy, 40% pronunciation, 20% fluency.
- Raw audio bị xóa sau khi chấm hoặc khi phiên kết thúc.
- Ôn tập spaced repetition với 4 mức độ nhớ (Quên/Khó/Nhớ/Dễ), lưu nghĩa và ví dụ của từng từ.
- Lưu lịch sử bài học, nội dung AI, phiên học và tổng thời gian học bằng Room; có màn hình ôn tập, lịch sử và tiến độ.
- Có quiz nhanh, đánh dấu từ yêu thích, mục tiêu học mỗi ngày và thông báo nhắc học định kỳ.

## Lưu ý phát hành APK cá nhân

Release hiện dùng signing mặc định của Gradle nếu chưa tạo keystore riêng, đủ để cài thử nhưng cần giữ nguyên certificate nếu muốn cập nhật đè APK sau này. Với bản dùng lâu dài, tạo keystore riêng ngoài repository và cấu hình `keystore.properties`.
