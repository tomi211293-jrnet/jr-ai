# JR AI

JR AI is an Android creative studio for AI-assisted photo and video creation.

## Current milestone
This repository starts with the Android app shell and a dark, mobile-first Compose interface. AI generation is intentionally not connected yet: we will add a secure backend and select genuinely available open-source inference services before enabling generation.

## Android
- Package: `com.jrai.studio`
- Kotlin + Jetpack Compose
- Build: GitHub Actions

## Important
GitHub stores source code and builds APKs; it does not provide unlimited AI compute. The project will avoid paid per-request APIs where possible, but free inference providers and self-hosted hardware can still impose capacity or usage limits.
