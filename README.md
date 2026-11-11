# Discord desktop-style for Chromebook — feasibility first

## Goal
Investigate whether a desktop-style Discord experience can be installed on an older Chromebook (reported ChromeOS/Chrome version 103.0.5060.132, user reports 32-bit x86) without enabling Linux.

## Important constraints

- The official Discord client is proprietary. This project does not redistribute, patch, re-sign, decompile, or modify Discord APK/EXE files.
- Discord prohibits unauthorized client modifications and reverse engineering in its Terms/Platform Manipulation policy. Do not use a modified client or user-account token.
- Discord's supported API distinguishes bot accounts from normal user accounts. The public bot API cannot act as a full replacement for a normal user's client: it cannot join arbitrary servers as the user, use friends/Group DMs like a user, or provide a general user session.
- A web wrapper does not make Discord faster; it still runs the web client and inherits browser/ChromeOS performance limits.
- A Windows desktop executable is not directly installable on ChromeOS without a compatibility/runtime layer. Linux desktop packaging is not an option if the user will not enable Linux.

## What must be verified on the target Chromebook

1. ChromeOS device model and CPU architecture (do not infer Android ABI from the Chrome browser version).
2. Android runtime version and supported ABIs: x86, x86_64, armeabi-v7a, arm64-v8a.
3. Whether the installed official Discord Android app is resizable / supports landscape and tablet layouts on this ChromeOS build.
4. Available RAM and storage, since these affect perceived performance.

## Feasible paths

### A. Use the official Android Discord app in ChromeOS windowed/landscape mode
This preserves the genuine Discord account and supported functionality. It does not guarantee a desktop multi-column layout; ChromeOS window controls and Android app behavior determine the result.

### B. Build a separate desktop-style Discord bot client
This can use Discord's supported bot API and OAuth2 where appropriate, but it is NOT a full replacement for a personal Discord account. It cannot offer all user-client features.

### C. Full desktop-style client for a normal Discord user account
There is no supported public API that lets a third-party app reproduce the full normal-user Discord client. Do not promise this by wrapping the website, modifying the APK, or using self-bot tokens.

## Current status
- Repository initialized with this technical feasibility note.
- No APK has been built.
- No Discord binary has been modified.
- A working build must not be advertised until it has been installed and tested on the user's actual Chromebook.

## References
- Discord system requirements: https://support.discord.com/hc/en-us/articles/213491697-What-are-the-OS-system-requirements-for-Discord
- Discord self-bot policy: https://support.discord.com/hc/en-us/articles/115002192352-Automated-User-Accounts-Self-Bots
- ChromeOS Android ABI guidance: https://developer.android.com/develop/devices/chromeos/learn/device-support
- Discord Terms: https://discord.com/terms
