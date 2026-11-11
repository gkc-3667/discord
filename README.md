# Discord Desktop Layout (Chromebook Android)

A native Android proof-of-concept with a desktop-style, multi-pane layout for ChromeOS Chromebooks.

## Scope and limitations

This is an independent prototype using Discord's documented OAuth2/API. It is not the official Discord app and does not modify, re-sign, decompile, or redistribute Discord binaries.

Implemented in this prototype:
- OAuth2 authorization-code sign-in with PKCE using the documented `identify` and `guilds` scopes.
- Shows the signed-in Discord account and the servers returned by Discord.
- Responsive multi-pane layout intended for landscape / large screens.
- No ABI-specific native code is included; Android packages Java/Kotlin bytecode for the target runtime.

Not implemented:
- Reading or sending messages, channel browsing, voice, calls, friends, DMs, or the full official client feature set. The supported OAuth scopes here do not grant a normal-user client session or general message access.
- A performance patch for the official Discord app. A web wrapper would still run the web client and would not solve browser performance.

## Build

GitHub Actions builds a debug APK on pushes to `main` and on manual workflow dispatch. Download the artifact named `discord-desktop-layout-debug-apk` from the Actions run.

## Discord OAuth setup

1. Create an application in the Discord Developer Portal.
2. In OAuth2 settings, add this exact redirect URI: `discorddesktop://oauth/callback`.
3. Copy the Application / Client ID.
4. Install the APK, enter the Client ID, and sign in.
5. Do not put a bot token or client secret in this repository or APK.

## Chromebook compatibility

The reported Chrome version (103.0.5060.132) does not identify the Android runtime version or supported ABI. This app contains no ABI-specific native libraries, but installation still depends on the Chromebook's Android runtime/API level. The app currently requires Android 8.0 (API 26) or newer. Test the generated APK on the actual Chromebook before treating it as supported.

## Roadmap

A supported bot-backed server view could be explored separately. It requires a hosted backend to protect bot credentials and only works in servers where the bot is invited; it cannot become a full personal Discord client.
