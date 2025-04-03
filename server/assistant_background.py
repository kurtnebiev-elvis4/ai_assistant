from task_0 import transcribe_audio
from task_1 import summarize_transcript, extract_decisions_from_transcript, extract_tasks_from_transcript
import os

UPLOAD_DIR = "uploads"

allowed_extensions = {".wav", ".mp3", ".flac"}

RESULT_TYPES = {
    "transcript": "",
    "summary": "_summary",
    "tasks": "_tasks",
    "decisions": "_decisions"
}


def run_full_analysis_pipeline(file_id: str):
    for ext in allowed_extensions:
        audio_path_candidate = os.path.join(UPLOAD_DIR, f"{file_id}{ext}")
        if os.path.exists(audio_path_candidate):
            audio_path = audio_path_candidate
            break
    else:
        raise FileNotFoundError(f"No audio file found for {file_id} with supported extensions.")
    transcript_path = os.path.join(UPLOAD_DIR, f"{file_id}.txt")
    print("Step 0 complete: Start analysis")
    # 1. Transcribe audio to text
    transcribe_audio(audio_path, transcript_path)
    print("Step 1 complete: Audio transcribed")

    # 2. Generate meeting summary
    summarize_transcript(file_id, transcript_path)
    print("Step 2 complete: Transcript summarized")
    # 3. Extract key decisions
    extract_decisions_from_transcript(file_id, transcript_path)
    print("Step 3 complete: Decisions extracted")
    # 4. Identify action items / tasks
    extract_tasks_from_transcript(file_id, transcript_path)
    print("Step 4 complete: Tasks extracted")
