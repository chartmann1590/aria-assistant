# Aria Privacy Policy

Last updated: July 17, 2026

Aria is developed by Charles Hartmann. Contact: charles.h.hartmann1@gmail.com.

## Core on-device processing

Aria processes wake-word audio, speech recognition, language-model inference, voice synthesis, and downloaded-language interface translation on the device. Voice recordings are not intentionally uploaded. Conversation history is stored in the app's local database until it is cleared or the app is uninstalled.

## Data sent over the network

- AI, speech, voice, and Google ML Kit language models are downloaded when needed. Once a translation model is downloaded, menu translation does not depend on a cloud translation service.
- When web verification is used, the query and requested public page URLs are sent to search/content providers. Those providers may process network information under their own policies.
- Firebase Analytics may process app-instance identifiers, app interactions, and device/app metadata. Firebase Crashlytics may process crash logs, stack traces, device/app state, and diagnostic identifiers. Firebase Performance Monitoring may process timing, device/app metadata, and sanitized network-performance information. Aria does not intentionally attach voice audio or conversation content to these services.
- Google AdMob serves ads to free users after the applicable consent flow and may process advertising identifiers, device information, approximate location derived from IP, ad interactions, and diagnostics under Google's policies. Premium users do not see ads.
- Google Play Billing processes subscription and purchase information. Aria receives purchase status/token information, not payment-card details.
- Support reports submitted from Settings are sent to GitHub and contain only the text, diagnostics, contact details, and attachments the user chooses to provide.
- A response-quality report is sent to an Aria Cloudflare Worker. By default it contains only the selected problem category, app version, language, and response length. Prompt and response text are sent and stored only when the user affirmatively selects “Include my prompt and Aria's response.” Users are warned not to share sensitive information.

## Optional permissions and services

Microphone access supports voice transcription and wake-word detection. Location, contacts, phone, calendar, camera, notification, overlay, usage-access, and system-setting permissions support the specific features described when requested. Non-Play developer builds can additionally contain opt-in SMS features.

The optional Notification Listener can read or act on notifications for user-requested notification features. It is off by default, requires a separate manual Android Settings grant, and is not activated remotely. A prominent in-app disclosure appears before its settings page is opened. The Google Play release does not include an AccessibilityService or restricted SMS/Call Log permissions.

## Retention and deletion

Local conversation history and downloaded models remain until cleared or the app is uninstalled. Firebase, Google, GitHub, Cloudflare, and public web providers retain transmitted data according to their policies and configured retention controls. To request deletion of an explicitly shared quality report or support report, contact the developer with enough information to locate it. Aggregate quality reports intentionally contain no account or advertising identifier.

## Children

Aria is not directed to children under 13, and the developer does not knowingly collect children's personal data.

## Security and changes

Reasonable technical controls are used, but no storage or transmission method is completely secure. Material policy changes will update the date above and be reflected in the published policy.

## Contact

Charles Hartmann  
charles.h.hartmann1@gmail.com  
https://github.com/chartmann1590/aria-assistant
