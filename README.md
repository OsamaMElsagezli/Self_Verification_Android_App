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

## 📂 Project Structure
