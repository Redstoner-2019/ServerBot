from gtts import gTTS
import sys

def generate_tts(text, output_file="tts.mp3"):
    tts = gTTS(text, lang="en")  # You can change the language if needed
    tts.save(output_file)

if __name__ == "__main__":
    if len(sys.argv) > 1:
        text = " ".join(sys.argv[1:])  # Get text from Java command-line arguments
        generate_tts(text)
