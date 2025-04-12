import os

UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

allowed_extensions = {".wav", ".mp3", ".flac"}

RESULT_TYPES = {
    "transcript": "",
    "summary": "_summary",
    "tasks": "_tasks",
    "decisions": "_decisions",
    "ready": "_ready"
}
