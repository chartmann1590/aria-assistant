# AGENTS.md — Aria Assistant

## Build commands
- Build: `./gradlew assembleDebug`
- Unit tests: `./gradlew test`
- Lint: `./gradlew lint`
- Instrumented tests: `./gradlew connectedAndroidTest` (requires emulator/device)
- Run one test class: `./gradlew test --tests "com.aria.assistant.billing.FeatureGateTest"`
- Robolectric tests: same task (`./gradlew test`); tests declare `@RunWith(RobolectricTestRunner::class)`

## Run before committing
- `./gradlew test` — must be green
- `./gradlew lint` — must be clean of Error severity

## Architecture facts
- Premium gate: `FeatureGate.isAllowed(toolName)` called from `AgentRunner` (single source of truth). Skill layer does NOT re-check.
- `SkillResult.RequiresPremium` is reserved-but-unused; `ToolResult.fromSkillResult` maps it to `Say("That requires a Premium subscription.")`.
- Notification/Accessibility bridges are Hilt singletons; the system-bound services (`AriaNotificationListener`/`AriaAccessibilityService`) are `@AndroidEntryPoint` and inject the bridge.
- Iteration cap is `MAX_ITERATIONS = 4` in `AgentRunner`.
- sherpa-onnx: `com.bihe0832.android:lib-sherpa-onnx:8.5.4` (sealed repackaging).

## When adding a new Tool
1. Add to `agent/Tools.kt` with `requiresPremium` flag.
2. Add the matching literal to `Feature.toolNames` + `isPremium` in `billing/FeatureGate.kt`.
3. Add a vocab entry to `IntentRouter.resolve` JSON switch + `engine/IntentRouterTest`.
4. Add a skill test under `app/src/test/java/com/aria/assistant/skill/`.
5. Add a row to `presentation/screen/PremiumScreen.kt` features table if premium.

## Known limitations
- Special permissions require user toggle in Settings; `PermissionManager.requestIntent` deep-links only.
- Some Android settings (wifi/airplane/hotspot) can't be toggled by non-system apps → `Settings.Panel.ACTION_*` fallback with spoken confirmation.
- `PermissionResult.Denied.permanent` is currently always `false`; do not rely on it.
- Accessibility & notification-listener permissions invite Play Store review — keep opt-in.
