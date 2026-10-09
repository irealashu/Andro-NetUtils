# Andro NetUtils

Enterprise-grade network diagnostic, protocol analysis, and defensive security auditing suite for Android.

Andro NetUtils is a Kotlin-based Android application that brings a collection of network and security utilities into a single, dashboard-driven interface. It is designed for reconnaissance, diagnostics, and defensive analysis across common network services, packet flows, DNS behavior, Wi‑Fi telemetry, and HTTP security posture.

## Highlights

- TCP/UDP port scanning and subnet discovery
- DNS suite analysis including resolver and DoH-oriented checks
- HTTP header auditing and defensive security review
- Packet dissection and protocol inspection workflows
- Traceroute and ping utilities
- Wi‑Fi telemetry and wireless diagnostics
- Historical audit reporting and security summaries
- Security architecture and posture dashboard

## Project structure

```text
Andro-NetUtils/
├── app/
│   ├── src/
│   │   ├── androidTest/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/example/
│   │   │   └── res/
│   │   └── test/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── .gitignore
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
├── .env.example
├── metadata.json
├── README.md
└── .gitignore
```

## Core app modules

- `app/src/main/java/com/example/MainActivity.kt` – main dashboard and navigation shell
- `app/src/main/java/com/example/ui/screens` – feature screens for networking and security tools
- `app/src/main/java/com/example/domain/engine` – engine implementations for scanners, analyzers, and inspectors
- `app/src/main/java/com/example/domain/model` – model definitions for network and security data
- `app/src/main/java/com/example/data` – local persistence and repository layers

## Features in brief

### Network diagnostics
- Port scanning and subnet sweep utilities
- Ping and traceroute investigation
- Packet inspection and protocol parsing

### Security and auditing
- HTTP defensive header checking
- TLS and site inspection review
- DNS suite and DNS security analysis
- Wi‑Fi telemetry and wireless diagnostics
- Security posture and historical audit reporting

## Requirements

- Android Studio
- Android SDK 36 / compile SDK 36
- JDK 11+
- Gradle wrapper included in the repo

## Setup

1. Clone the repository:

   ```bash
   git clone https://github.com/irealashu/Andro-NetUtils.git
   cd Andro-NetUtils
   ```

2. Copy the environment example for app secrets:

   ```bash
   cp .env.example .env
   ```

3. Add your Gemini API key to `.env` if you are using the Gemini-backed features:

   ```dotenv
   GEMINI_API_KEY=YOUR_API_KEY
   ```

4. Open the project in Android Studio and let Gradle sync.

## Build

Debug build:

```bash
./gradlew assembleDebug
```

Install on a connected device or emulator:

```bash
./gradlew installDebug
```

## Notes

This project includes Firebase and Gemini integration settings, with the app configured to use `.env` and `.env.example` via the Secrets Gradle plugin. If a Gemini key is omitted, related API flows may be unavailable or behave as disabled depending on the code path.

## License

This repository does not currently declare a license in the root files. If you plan to distribute or reuse the project, confirm the licensing terms before publishing or sharing externally.
