# 健康沙漏 (Hourglass Health) - 项目状态

## Done

### Project
- GitHub repo: hsx20050520-beep/hourglass-health
- Android project structure (Kotlin + Gradle)
- Hourglass app icon
- GitHub Actions CI
- Fixed gradle-wrapper.jar CI issue

### Features
- Water reminder: foreground service + notification, custom interval (30m/1h/1.5h/2h/3h)
- Sleep analysis engine: scoring based on deep/light/REM/awake ratios + health advice
- Sleep analysis page: score display, stage breakdown, issues, tips
- Demo data loading

### Tools on Desktop
- gadgetbridge-0.92.0.apk
- F-Droid.apk
- adb/ with adb.exe
- abe.jar (Android Backup Extractor)
- extract_auth_key_v2.bat (uses correct package: com.mi.health)

## Todo / Blocked

### Band Data Reading (BLOCKED)
- Mi Band 9 Pro auth key extraction failed (Android 16 blocks adb backup)
- BLE direct connection blocked by auth key requirement
- Health Connect integration failed (health-connect-client only has alpha, API unstable)
- Auth APK not found on GitHub/F-Droid/IzzyOnDroid

### Features
- Manual sleep data input
- Sleep history / trend charts
- Heart rate display
- Steps/calories display
- Chinese UI
- Settings page (reminder hours, daily targets)

### CI
- Build currently failing (Kotlin compilation errors)

## Key Decisions
1. Skipped BLE direct due to auth key requirement
2. Health Connect alpha SDK too unstable
3. Current: water reminder works, sleep analysis works with demo data
4. Next: wait for Health Connect stable, or find Auth APK for Gadgetbridge pairing
