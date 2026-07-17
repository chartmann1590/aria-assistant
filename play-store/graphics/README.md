# Google Play graphics

Production assets:

- Play icon: `icon/aria-play-icon-512.png` (512 × 512).
- Feature graphic: `feature-graphic/aria-feature-graphic-1024x500.png` (1024 × 500).
- Raw phone captures: `phone-screenshots/raw/` (real Pixel 8 Pro UI).
- Captioned phone art: `phone-screenshots/framed/` (Play-ready portrait PNGs).
- 7-inch tablet art: `seven-inch-screenshots/framed/` (1200 × 1920 Play-ready PNGs derived from the real Pixel captures).
- 10-inch tablet art: `ten-inch-screenshots/framed/` (1600 × 2560 Play-ready PNGs derived from the real Pixel captures).
- Promo video: `promo-video/aria-google-play-promo-1080p.mp4` (1920 × 1080 H.264/AAC, natural `en-US-AriaNeural` narration).
- Published YouTube promo: https://youtu.be/_wgi6OSL80g

The icon and feature-graphic masters were generated with OpenAI's built-in image generation tool using the prompts recorded in `generation-prompts.md`. Text and screenshot frames are composed deterministically so actual UI content is not altered.

Google Play accepts between 2 and 8 phone screenshots. The raw captures are retained to document that the marketed UI came from the physical test device.
The listing automation attaches the published YouTube promo URL to the English Google Play listing.
Run `scripts/prepare-tablet-screenshots.ps1` after updating the framed phone captures to regenerate both tablet sets without altering the captured UI.
