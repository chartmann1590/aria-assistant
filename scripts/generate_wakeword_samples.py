"""Generate synthetic TTS samples for wake word training."""
from gtts import gTTS
import os
import random

PHRASE = "hey aria"
OUTPUT_DIR = "./samples/hey_aria"
os.makedirs(OUTPUT_DIR, exist_ok=True)

for i in range(200):
    lang = random.choice(["en"])
    tts = gTTS(PHRASE, lang=lang, slow=random.random() < 0.2)
    tts.save(f"{OUTPUT_DIR}/sample_{i:03d}.mp3")
