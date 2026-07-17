# Aria Google Play release materials

This directory contains the production store listing, policy notes, generated artwork, real-device screenshots, and promo-video source/output.

## Contents

- `store-listing/`: title, descriptions, category, tags, and release notes.
- `graphics/icon/`: 512 × 512 Play icon.
- `graphics/feature-graphic/`: 1024 × 500 feature graphic.
- `graphics/phone-screenshots/raw/`: unmodified Pixel captures.
- `graphics/phone-screenshots/framed/`: captioned Play-ready screenshots.
- `graphics/promo-video/`: 1920 × 1080 narrated promo video.
- `policies/`: privacy policy, terms, and Data safety review notes.
- `contact/`: public listing contact details.

## Play Console checklist

- App is free and contains ads; Premium removes ads.
- Complete Data safety using `policies/data-safety-notes.md` and the current SDK disclosures.
- Confirm the uploaded manifest does not contain AccessibilityService or restricted SMS/Call Log permissions; those capabilities are excluded from the Play release variant.
- Complete content rating, target audience, ads, app access, and privacy-policy declarations.
- Confirm the AdMob Privacy & Messaging consent message is published for the app.
- Keep any production release in draft until all Console declarations and automated review checks are complete.
