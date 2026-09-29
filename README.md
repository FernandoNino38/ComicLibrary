# 📚 Comic Library (Android)

> **A native, ultra-responsive, and modern CBZ / Comic / Manga reader for Android built with Jetpack Compose and Material 3.**

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![AMOLED](https://img.shields.io/badge/AMOLED-Pure%20%23000000-black.svg)](#)
[![Version](https://img.shields.io/badge/Version-v1.0-yellow.svg)](#)

---

## 📸 Screenshots

<p align="center">
  <img src="docs/screenshots/library.png" width="30%" />
  <img src="docs/screenshots/reader.png" width="30%" />
  <img src="docs/screenshots/settings.png" width="30%" />
</p>

---

## 🎨 Visual Identity & Comic Book Aesthetic

Comic Library was designed from the ground up to immerse readers in the classic look and feel of comic books:
* **Comic Lettering & Bangers Typography**: Titles, action buttons, counters, and badges are styled using the authentic `Bangers` comic font.
* **Authentic Comic Palette**: Features classic Golden & Silver Age superhero colors (`ComicYellow #FFD600`, `ComicRed #E52521`, `ComicCyan #00E5FF`) on a pure **AMOLED `#000000`** canvas.
* **Comic Panel Geometry**: Shelves, cards, and modal dialogs designed like genuine comic book frames and technical dossier cards.

---

## ✨ Features

- **🚀 Streaming CBZ Archive Engine**: Reads `.cbz` and `.zip` archives on demand directly from Android's Storage Access Framework (SAF) with zero unnecessary disk extraction.
- **🖤 Pure AMOLED Dark Mode (`#000000`) & ☀️ Comic Paper Light Mode**:
  - **AMOLED Dark Mode**: Saves battery on OLED displays and prevents eye strain during extended night reading sessions.
  - **High-Contrast Comic Paper Light Mode**: Crisp vintage paper background with deep comic ink black lettering (`#000000`), vibrant comic action stickers, and deep crimson accents for maximum legibility under bright sunlight.
- **🔍 Advanced Multi-Touch & On-Screen Zoom**:
  - Seamless pinch-to-zoom gesture and double-tap zoom.
  - Dedicated on-screen zoom control bar with instant percentage pills (`100%`, `150%`, `200%`, `400%`) and fit-to-screen reset.
  - Page isolation preventing neighboring pager screens from glitching during zoom.
- **🍃 3D Leaf-Curl Page Turn Animation**: Smooth physics-based page curl responding to swipes and discrete tap zones.
- **🎯 3-Zone Touch Controls**:
  - **Left Tap**: Turn page backward (or forward in Manga mode).
  - **Right Tap**: Turn page forward (or backward in Manga mode).
  - **Center Tap**: Toggle reader chrome and scrubber controls.
- **📖 Manga (RTL) & Western (LTR) Reading Modes**:
  - Automatic detection of manga metadata.
  - Runtime one-tap toggle between Right-to-Left (Manga) and Left-to-Right (Western).
- **📋 Rich Metadata Extraction (`ComicInfo.xml`)**:
  - Reads embedded writer, penciller, inker, publisher, synopsis, and year.
  - **Long-Press Dossier**: Long-pressing any comic opens a detailed comic book technical sheet displaying complete metadata, file size, storage URI, and quick actions.
- **🔍 Instant Library Search & Filters**:
  - Expandable search icon bar in the library header.
  - Action badge filters: *All*, *Reading*, *Unread*, *Completed*, *Manga*, and *Favorites*.
- **🌐 Bilingual Internationalization (i18n)**:
  - English and Portuguese automatically matching Android system settings.
- **📱 Adaptive Form Factor Layouts**:
  - Seamless layout scaling across Phones, Foldables, and Tablets using Android's Canonical Layouts (`NavigableListDetailPaneScaffold`).

---

## 🏗️ Architecture & Tech Stack

The app follows **MVI (Model-View-Intent)** architecture with strict unidirectional data flow and clean separation of concerns:

- **UI Layer**: 100% Jetpack Compose with Material Design 3.
- **State Management**: Kotlin Coroutines `StateFlow` and MVI Intents.
- **Image Pipeline**: Coil with custom CBZ memory decoders and bitmap tiling/subsampling to avoid `OutOfMemoryError` on ultra-high-resolution pages.
- **Storage**: Android Storage Access Framework (SAF) with Scoped Storage persistence (`takePersistableUriPermission`).
- **Build System**: Gradle Version Catalog (`libs.versions.toml`) with Kotlin DSL.

---

## 🛠️ Building & Running

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 36 (compileSdk) / Min SDK 24 (Android 7.0+)
- JDK 17

### Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

### Run Unit Tests
```bash
./gradlew test
```

---

## 👥 Credits & Authors

- **Lead Developer**: **Fernando Nino** ([@FernandoNino38](https://github.com/FernandoNino38))
- **AI Pair Programmer & Architect**: **Antigravity** (Google DeepMind Advanced Agentic Coding Assistant)

---

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
