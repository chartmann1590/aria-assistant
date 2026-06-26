package com.aria.assistant.permission

enum class PhoneCapability(
    val title: String,
    val rationale: String,
    val kind: PermKind
) {
    CALL(
        title = "Phone Calls",
        rationale = "Place phone calls to contacts",
        kind = PermKind.RUNTIME
    ),
    SMS(
        title = "Send SMS",
        rationale = "Send text messages to contacts",
        kind = PermKind.RUNTIME
    ),
    READ_SMS(
        title = "Read SMS",
        rationale = "Read incoming text messages to respond to them hands-free",
        kind = PermKind.RUNTIME
    ),
    CONTACTS(
        title = "Contacts",
        rationale = "Look up contact names and phone numbers",
        kind = PermKind.RUNTIME
    ),
    WRITE_SETTINGS(
        title = "Write Settings",
        rationale = "Adjust system brightness and other device settings",
        kind = PermKind.SPECIAL
    ),
    CALENDAR(
        title = "Calendar",
        rationale = "Read and create calendar events and reminders",
        kind = PermKind.RUNTIME
    ),
    NOTIFICATIONS(
        title = "Notifications",
        rationale = "Show notifications for incoming calls, messages, and reminders",
        kind = PermKind.RUNTIME
    ),
    NOTIFICATION_LISTENER(
        title = "Notification Access",
        rationale = "Read incoming notifications so Aria can respond to messages and alerts",
        kind = PermKind.SPECIAL
    ),
    ACCESSIBILITY(
        title = "Accessibility Service",
        rationale = "Monitor screen content for context-aware assistance",
        kind = PermKind.SPECIAL
    ),
    USAGE_ACCESS(
        title = "Usage Access",
        rationale = "See which apps are running and how they are used",
        kind = PermKind.SPECIAL
    ),
    LOCATION(
        title = "Location",
        rationale = "Provide location-aware responses and weather forecasts",
        kind = PermKind.RUNTIME
    ),
    CAMERA(
        title = "Camera",
        rationale = "Take photos and scan documents or QR codes",
        kind = PermKind.RUNTIME
    ),
    MEDIA_CONTROL(
        title = "Media Control",
        rationale = "Control music and video playback on your device",
        kind = PermKind.RUNTIME
    ),
    BLUETOOTH(
        title = "Bluetooth",
        rationale = "Control Bluetooth settings and connections",
        kind = PermKind.RUNTIME
    ),
    APP_LAUNCH(
        title = "App Launch",
        rationale = "Open other apps on your device",
        kind = PermKind.SPECIAL
    ),
    BATTERY_OPT(
        title = "Battery Optimization",
        rationale = "Stay running in the background to always be ready when called",
        kind = PermKind.SPECIAL
    ),
    DND(
        title = "Do Not Disturb",
        rationale = "Control Do Not Disturb mode and interruption filters",
        kind = PermKind.SPECIAL
    );

    enum class PermKind {
        RUNTIME, SPECIAL
    }
}
