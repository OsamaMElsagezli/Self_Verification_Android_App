<div align="center">

<img src="https://raw.githubusercontent.com/twitter/twemoji/master/assets/72x72/1f6c2.png" width="96" alt="Self Verification App logo"/>

# 🛡️ Self-Verification Android App

**On-device liveness check + passport scan, uploaded straight to your backend.**

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/language-Java-007396?logo=java&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-24-blue)
![Target SDK](https://img.shields.io/badge/targetSdk-35-blue)
![CameraX](https://img.shields.io/badge/CameraX-1.3.4-4285F4?logo=googlecamera&logoColor=white)
![ML Kit](https://img.shields.io/badge/ML%20Kit-Face%20Detection-FF6F00?logo=googlecloud&logoColor=white)
![Build](https://img.shields.io/badge/build-Gradle%20(Kotlin%20DSL)-02303A?logo=gradle&logoColor=white)
![Status](https://img.shields.io/badge/status-work%20in%20progress-yellow)

</div>

---

## 📖 Overview

This app walks a user through a short, guided identity self-verification flow entirely on-device before handing the results off to your server:

1. 🙂 **Liveness Challenge** — front camera + ML Kit face detection prompts the user to center their face, turn left, then turn right, inside an on-screen oval guide. Each successful step auto-captures a photo.
2. 🛂 **Passport Scan** — back camera opens with a passport outline overlay to guide the user into photographing their passport.
3. ☁️ **Upload** — captured images are packaged into a multipart HTTP request and sent to a configurable backend endpoint.
4. ✅ **Review** — the main screen shows thumbnails and an optional 2×2 collage of the captured images, plus upload status with a retry button.

---

## ✨ Features

| | Feature |
|---|---|
| 🎯 | Real-time face detection & head-pose (yaw) tracking via **ML Kit** |
| 📷 | Dual camera flows via **CameraX** — front camera for selfies, back camera for passport |
| 🖼️ | Visual guide overlays (oval face guide, passport outline) with live success feedback |
| 📤 | Multipart image upload via **Retrofit + OkHttp** |
| 🧩 | Local thumbnail generation and 2×2 image collage |
| 🔐 | Runtime permission handling for camera and media access |

---

## 🧰 Tech Stack

| Layer | Technology |
|---|---|
| 🗣️ Language | Java |
| 🏗️ Build system | Gradle (Kotlin DSL) |
| 📱 Min SDK / Target SDK | 24 / 35 |
| 🎥 Camera | CameraX 1.3.4 |
| 🧠 Face detection | ML Kit Face Detection 16.1.5 |
| 🌐 Networking | Retrofit 2.11.0, OkHttp 4.12.0, Gson |
| 🎨 UI | AndroidX, Material Components, View Binding |

---

app/src/main/java/com/example/myapp/
├── MainActivity.java # 🏠 Entry screen; thumbnails, collage, upload trigger
├── SelfieVerifierActivity.java # 🙂 Camera + liveness challenge state machine
├── LivenessDetectionHelper.java # 🧠 ML Kit face analysis (yaw, blink, smile signals)
├── LivenessChallenge.java # 📋 Enum of challenge steps and prompts
├── PassportScanActivity.java # 🛂 Back-camera passport capture and upload
├── PassportOutlineOverlay.java # 🖼️ Passport guide overlay (vector-based)
├── PassportPngOverlay.java # 🖼️ Passport guide overlay (PNG-based)
├── OverlayView.java # ⭕ Oval face-guide overlay with success checkmark
├── ApiClient.java # 🔌 Retrofit client factory
├── UploadApi.java # 🔌 Retrofit API interface
├── UploadResponse.java # 📦 Upload response model
├── MultipartUtils.java # 🧵 Multipart request body helpers
└── feature/
├── Uploader.java # 📤 Builds multipart parts and issues upload call
└── UploadActions.java # 📤 Convenience wrapper with progress/success/error callbacks



---

## 🌐 App Showcase

## AI-Assisted Face Verification Flow

The following examples demonstrate the face-verification guidance flow using a stylized AI-generated avatar while preserving the original verification UI.

### 1. Start Verification

<p align="center">
  <img
    width="260"
    alt="Start verification screen"
    src=" <img width="499" height="803" alt="Screenshot 2026-09-17 215524" src="https://github.com/user-attachments/assets/25e51597-d462-4f7a-8070-7a51a0424449" />
 "
  />
</p>

<p align="center">
  <em>Begin the identity verification process by selecting Start Verification.</em>
</p>

### 2. Front-Facing Verification

<p align="center">
  <img
    width="260"
    alt="Front-facing face verification"
    src="https://github.com/user-attachments/assets/b945b274-86a3-4f7b-8360-cdd8b24fef1b"
  />
</p>

<p align="center">
  <em>Front-facing pose with the face positioned inside the verification oval.</em>
</p>

### 3. Head Turn - Left

<p align="center">
  <img
    width="260"
    alt="AI face verification with head turned left"
    src="https://github.com/user-attachments/assets/06776a38-deb3-4f98-b3ad-ebbf7d80e19b"
  />
</p>

<p align="center">
  <em>Head-turn guidance showing the user facing left while remaining inside the verification oval.</em>
</p>

### 4. Head Turn - Right

<p align="center">
  <img
    width="260"
    alt="AI face verification with head turned right"
    src="https://github.com/user-attachments/assets/8ab37e3d-8a86-418a-8535-69f2f08f9b33"
  />
</p>

<p align="center">
  <em>Successful side-angle verification with the head turned in the requested direction.</em>
</p>

## 🚀 Setup

1. Clone or open the project in **Android Studio** (Giraffe or newer recommended).
2. Set your backend base URL:
   - In `MainActivity.java` → update `BASE` (defaults to `http://10.0.2.2:8000/`, the Android emulator's loopback to `localhost`).
   - In `PassportScanActivity.java` → replace the placeholder `BASE_URL = "https://YOUR-WEBSITE-BASE-URL/"` with your real endpoint.
3. Make sure your backend exposes an endpoint matching `UploadApi` (e.g. `POST upload/passport`, multipart fields for username + image files).
4. Build and run on a device or emulator with camera support. 📱

---

## 🔑 Permissions

| Permission | Why it's needed |
|---|---|
| `CAMERA` | Selfie and passport capture |
| `INTERNET` | Uploading images to the backend |
| `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` | Thumbnail/collage generation on older SDKs |

---

## ⚠️ Known Limitations

- 🔧 `PassportScanActivity`'s base URL is a placeholder and must be configured before use.
- ⏸️ Passport upload via `Uploader.uploadAll` is currently disabled (`pPassport` passed as `null`), even though `PassportScanActivity` uploads the passport separately — the two upload paths aren't unified yet.
- 🐛 `UploadApi.upload(...)` is missing `@Multipart` / `@POST` annotations required for a valid Retrofit endpoint.
- 🔀 Two overlay implementations (`PassportOutlineOverlay`, `PassportPngOverlay`) exist side by side; one appears to be a work-in-progress replacement for the other.
- 🔓 Cleartext HTTP traffic is enabled (`usesCleartextTraffic="true"`) for local development — disable before any production release.

---


## 📂 Project Structure

**`com.example.myapp/`**

| File | Description |
|---|---|
| 🏠 `MainActivity.java` | Entry screen — thumbnails, collage, upload trigger |
| 🙂 `SelfieVerifierActivity.java` | Camera + liveness challenge state machine |
| 🧠 `LivenessDetectionHelper.java` | ML Kit face analysis (yaw, blink, smile signals) |
| 📋 `LivenessChallenge.java` | Enum of challenge steps and prompts |
| 🛂 `PassportScanActivity.java` | Back-camera passport capture and upload |
| 🖼️ `PassportOutlineOverlay.java` | Passport guide overlay (vector-based) |
| 🖼️ `PassportPngOverlay.java` | Passport guide overlay (PNG-based) |
| ⭕ `OverlayView.java` | Oval face-guide overlay with success checkmark |
| 🔌 `ApiClient.java` | Retrofit client factory |
| 🔌 `UploadApi.java` | Retrofit API interface |
| 📦 `UploadResponse.java` | Upload response model |
| 🧵 `MultipartUtils.java` | Multipart request body helpers |

**`com.example.myapp.feature/`**

| File | Description |
|---|---|
| 📤 `Uploader.java` | Builds multipart parts and issues upload call |
| 📤 `UploadActions.java` | Convenience wrapper with progress/success/error callbacks |

---

## 🚀 Setup

1. Clone or open the project in **Android Studio** (Giraffe or newer recommended).
2. Set your backend base URL:
   - In `MainActivity.java` → update `BASE` (defaults to `http://10.0.2.2:8000/`, the Android emulator's loopback to `localhost`).
   - In `PassportScanActivity.java` → replace the placeholder `BASE_URL = "https://YOUR-WEBSITE-BASE-URL/"` with your real endpoint.
3. Make sure your backend exposes an endpoint matching `UploadApi` (e.g. `POST upload/passport`, multipart fields for username + image files).
4. Build and run on a device or emulator with camera support. 📱

---

## 🔑 Permissions

| Permission | Why it's needed |
|---|---|
| `CAMERA` | Selfie and passport capture |
| `INTERNET` | Uploading images to the backend |
| `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` | Thumbnail/collage generation on older SDKs |

---

## ⚠️ Known Limitations

- 🔧 `PassportScanActivity`'s base URL is a placeholder and must be configured before use.
- ⏸️ Passport upload via `Uploader.uploadAll` is currently disabled (`pPassport` passed as `null`), even though `PassportScanActivity` uploads the passport separately — the two upload paths aren't unified yet.
- 🐛 `UploadApi.upload(...)` is missing `@Multipart` / `@POST` annotations required for a valid Retrofit endpoint.
- 🔀 Two overlay implementations (`PassportOutlineOverlay`, `PassportPngOverlay`) exist side by side; one appears to be a work-in-progress replacement for the other.
- 🔓 Cleartext HTTP traffic is enabled (`usesCleartextTraffic="true"`) for local development — disable before any production release.

---
