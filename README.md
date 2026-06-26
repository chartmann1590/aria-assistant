<div align="center">

<img src="app/src/main/res/mipmap-hdpi/ic_launcher.png" width="96" alt="Aria icon" />

# Aria

### Your private AI assistant — fully on-device

[![License](https://img.shields.io/badge/license-Apache%202.0-7C5CFF?style=flat-square)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B-22D3C5?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-A78BFA?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2025.05-7C5CFF?style=flat-square)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/min%20SDK-26-F59E0B?style=flat-square)](https://developer.android.com/studio/releases/platforms)

**No cloud. No tracking. No subscription required to use the core assistant.**

[Website](https://chartmann1590.github.io/aria-assistant) · [Privacy Policy](https://chartmann1590.github.io/aria-assistant/privacy.html) · [Download APK](https://github.com/chartmann1590/aria-assistant/releases) · [Report a bug](https://github.com/chartmann1590/aria-assistant/issues)

</div>

---

## What is Aria?

Aria is a fully on-device AI voice assistant for Android. Say "Hey Aria" from any screen and get a natural spoken response — without a single byte of your audio, conversation, or location leaving your phone.

Under the hood it combines three on-device models:

| Component | Model | What it does |
|-----------|-------|-------------|
| **LLM** | Gemma 4 (LiteRT / GPU) | Understands intent, generates responses |
| **Speech-to-Text** | Whisper (ONNX Runtime) | Transcribes your voice locally |
| **Text-to-Speech** | Piper TTS (Sherpa-ONNX) | Natural speech synthesis, multiple voices |
| **Wake word** | OpenWakeWord | Always-on "Hey Aria" detection |

---

## Features

### Free forever
- 💬 Natural conversation with Gemma 4 on-device LLM
- 🎙️ "Hey Aria" wake word (always listening, fully local)
- ⏰ Timers, alarms, and reminders
- 🌐 Web search & Wikipedia (when you need current info)
- 📱 App launch, media control, battery/time queries
- 📩 Read your SMS inbox and dismiss notifications
- 🗺️ Location & navigation queries
- 1 TTS voice included

### Premium ($1.67/mo billed yearly · $2.99/mo)
Everything above, plus:
- 📞 Make phone calls by voice
- ✉️ Send SMS hands-free
- ⚙️ System settings control ("dim the screen", "toggle Wi-Fi")
- 🗓️ Create calendar events and reminders
- 📬 Read and reply to notifications
- 📷 Camera — take photos, describe what you see
- 🖥️ Screen control — tap, scroll, and read UI elements
- 5 additional premium TTS voices
- Gemma E4B enhanced model

---

## Privacy

> **Your voice never leaves your phone.**

All audio processing, speech recognition, LLM inference, and text-to-speech happen entirely on your device. Aria contains no analytics SDKs, no advertising SDKs, and no crash-reporting SDKs.

The only network requests Aria makes:
1. Downloading the AI model on first launch (one-time, ~2.5 GB)
2. Optional web search queries (when you explicitly ask)
3. Google Play Billing to verify subscription status (token only, no personal data)

See the full [Privacy Policy](https://chartmann1590.github.io/aria-assistant/privacy.html) for a per-permission breakdown.

---

## Requirements

| | Minimum | Recommended |
|---|---|---|
| **Android** | 8.0 (API 26) | 12.0+ |
| **RAM** | 4 GB | 8 GB+ |
| **Storage** | 3 GB free | 5 GB free |
| **Chipset** | Any arm64 | Tensor / Snapdragon 8 Gen series |

GPU acceleration for Gemma 4 inference is available on Pixel 6+ (Google Tensor) and Snapdragon 8 Gen 1+ devices. The app falls back to CPU on older hardware.

---

## Building from source

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17
- Android SDK with API 35 build tools

### Clone & build

```bash
git clone https://github.com/chartmann1590/aria-assistant.git
cd aria-assistant
./gradlew :app:assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Install to a connected device

```bash
./gradlew :app:installDebug
```

Or with ADB directly:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Release build (for signing)

```bash
./gradlew :app:bundleRelease   # produces an AAB for Play Store upload
./gradlew :app:assembleRelease  # produces a signed APK
```

Configure your signing keystore in `local.properties`:

```properties
KEYSTORE_PATH=../keystore.jks
KEYSTORE_PASSWORD=your_password
KEY_ALIAS=your_alias
KEY_PASSWORD=your_key_password
```

---

## Project structure

```
app/src/main/java/com/aria/assistant/
│
├── agent/          # AgentRunner — LLM loop, tool dispatch, action parsing
├── billing/        # Google Play Billing integration and feature gating
├── data/           # Room database, DAOs, repository implementations
├── di/             # Hilt dependency injection modules
├── domain/         # Models, repository interfaces, AriaState enum
├── engine/         # LiteRT LLM, Whisper STT, Piper TTS, WakeWordDetector
├── permission/     # Accessibility service, notification listener, PermissionManager
├── presentation/   # Compose UI — screens, components, ViewModels, theme
│   ├── component/  # AriaOrb, GlassCard, NebulaBackground, ConversationBubble
│   ├── screen/     # MainScreen, Settings, Onboarding, History, Premium, Permissions
│   ├── ui/theme/   # Nebula color tokens, Sora + DM Sans typography, Material3 theme
│   └── viewmodel/  # MainViewModel, SettingsViewModel, OnboardingViewModel, …
├── service/        # AriaForegroundService, AriaBootReceiver, AriaServiceController
└── skill/          # Individual capability skills (call, SMS, camera, calendar, …)
```

### Key architectural decisions

- **One-shot agent loop** — `AgentRunner` runs the LLM with a tool-use prompt, parses `<action>` tags from the output, dispatches to the matching `Skill`, then feeds the result back. No streaming multi-turn — keeps latency low on-device.
- **State machine** — `AriaStateManager` holds a single `AriaState` (`IDLE`, `LISTENING`, `PROCESSING`, `SPEAKING`, `MUTED`, `ERROR`, `DOWNLOADING`) that drives all UI and engine transitions.
- **Offline-first** — all models are bundled or downloaded once; no runtime network dependency for inference.
- **Glassmorphism without blur** — `Modifier.blur()` is a no-op below API 31. Glass surfaces use a translucent fill (`#FFFFFF0B`) + hairline border (`#FFFFFF17`) that reads correctly against the aurora mesh on all supported API levels.

---

## Design system

The UI follows the **Nebula** design language — a cosmic dark theme with an animated aurora gradient mesh, glassmorphism surfaces, and state-reactive accent colours.

| Token | Value | Use |
|-------|-------|-----|
| `NebulaBase` | `#06060B` | App background |
| `AuroraViolet` | `#7C5CFF` | Primary / idle state |
| `AuroraTeal` | `#22D3C5` | Listening state / success |
| `AuroraMagenta` | `#C04AE0` | Accents / gradients |
| `AuroraLavender` | `#A78BFA` | Processing state |
| `AuroraAmber` | `#F59E0B` | Speaking state / warnings |

Typography: **Sora** (display, headlines) + **DM Sans** (body, labels) — both bundled as TTFs for offline use.

Design specs live in [`design/aria-nebula/`](design/aria-nebula/) as self-contained HTML preview files.

---

## Contributing

Contributions are welcome! Please open an issue before submitting a PR for large changes.

```bash
# Run unit tests
./gradlew :app:test

# Run lint
./gradlew :app:lint
```

Please follow the existing code style (no comments unless the *why* is non-obvious, no unused imports, Kotlin idioms throughout).

---

## License

```
Copyright 2025 Charles Hartmann

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

See [LICENSE](LICENSE) for the full text.

Third-party components and their licences:
- [Gemma 4](https://ai.google.dev/gemma/terms) — Google Gemma Terms of Use
- [OpenWakeWord](https://github.com/dscripka/openWakeWord) — Apache 2.0
- [Sherpa-ONNX / Piper TTS](https://github.com/k2-fsa/sherpa-onnx) — Apache 2.0
- [ONNX Runtime](https://github.com/microsoft/onnxruntime) — MIT
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — Apache 2.0
- [Hilt](https://dagger.dev/hilt/) — Apache 2.0
- [Room](https://developer.android.com/training/data-storage/room) — Apache 2.0
- [Play Billing Library](https://developer.android.com/google/play/billing) — Android Software Development Kit License

---

<div align="center">

Made with ☕ and too many late nights &nbsp;·&nbsp; [charles.h.hartmann1@gmail.com](mailto:charles.h.hartmann1@gmail.com)

[Website](https://chartmann1590.github.io/aria-assistant) · [Privacy Policy](https://chartmann1590.github.io/aria-assistant/privacy.html) · [Terms of Service](https://chartmann1590.github.io/aria-assistant/terms.html)

</div>
