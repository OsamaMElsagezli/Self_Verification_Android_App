## Setup

1. Clone or open the project in Android Studio (Giraffe or newer recommended).
2. Set your backend base URL:
   - In `MainActivity.java`, update `BASE` (defaults to `http://10.0.2.2:8000/` — the Android emulator's loopback to `localhost`).
   - In `PassportScanActivity.java`, replace the placeholder `BASE_URL = "https://YOUR-WEBSITE-BASE-URL/"` with your actual endpoint.
3. Ensure your backend exposes an endpoint matching `UploadApi` (e.g. `POST upload/passport`, multipart fields for username and image files).
4. Build and run on a device or emulator with camera support.

## Permissions

- `CAMERA` — required for selfie and passport capture
- `INTERNET` — required for uploading images
- `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` — required for thumbnail/collage generation on older SDKs

## Known Limitations

- `PassportScanActivity`'s base URL is a placeholder and must be configured before use.
- Passport upload via `Uploader.uploadAll` is currently disabled (`pPassport` is passed as `null`), even though `PassportScanActivity` uploads the passport separately — the two upload paths are not yet unified.
- `UploadApi.upload(...)` is missing `@Multipart`/`@POST` annotations required for a valid Retrofit endpoint.
- Two overlay implementations (`PassportOutlineOverlay`, `PassportPngOverlay`) exist side by side; one appears to be a work-in-progress replacement for the other.
- Cleartext HTTP traffic is enabled (`usesCleartextTraffic="true"`) for local development — this should be disabled before any production release.
