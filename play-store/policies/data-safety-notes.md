# Google Play Data safety worksheet

Use this as a review checklist in Play Console; verify the exact SDK behavior and Console wording before submitting.

- App activity and app info/performance: collected by Firebase Analytics, Crashlytics, and Performance Monitoring for analytics and app functionality.
- Device or other identifiers: Firebase/AdMob may collect identifiers for analytics, diagnostics, fraud prevention, advertising, or consent as applicable.
- Approximate location: AdMob may derive it from IP for advertising and fraud prevention. Precise device location is used only for user-requested app features and is not intentionally sent to Aria servers.
- Advertising data: collected/shared with Google AdMob for free users after consent where required.
- Purchase history: processed through Google Play Billing for app functionality.
- User content: not collected for normal on-device conversations. It is collected only when the user explicitly includes it in a GitHub support report or opts into sharing prompt/response text in an AI quality report.
- Web searches: queries are shared with public search/content providers to provide the requested web-verification feature.
- Data is encrypted in transit via HTTPS.
- Users can delete local conversation data in the app or by uninstalling. Contact the support email for deletion requests concerning explicitly submitted reports.
- The app is not directed to children under 13.
- The Google Play release does not include an AccessibilityService or restricted SMS/Call Log permissions. Notification Access remains optional and disclosed in app.
