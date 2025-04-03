from transcription import transcribe_audio
from nlp_tasks import summarize_transcript, extract_decisions_from_transcript, extract_tasks_from_transcript
import os

UPLOAD_DIR = "uploads"

RESULT_TYPES = {
    "transcript": "",
    "summary": "_summary",
    "tasks": "_tasks",
    "decisions": "_decisions"
}

def run_full_analysis_pipeline(file_id: str):
    audio_path = os.path.join(UPLOAD_DIR, f"{file_id}.wav")
    transcript_path = os.path.join(UPLOAD_DIR, f"{file_id}.txt")

    # 1. Transcribe audio to text
    transcribe_audio(audio_path, transcript_path)

    # 2. Generate meeting summary
    summarize_transcript(transcript_path)
    # 3. Extract key decisions
    extract_decisions_from_transcript(transcript_path)
    # 4. Identify action items / tasks
    extract_tasks_from_transcript(transcript_path)
