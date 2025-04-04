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


def chunk_file(session_id: str, chunk_index: int, file_extension: str):
    session_dir = os.path.join(UPLOAD_DIR, session_id)
    os.makedirs(session_dir, exist_ok=True)
    chunk_filename = f"chunk_{chunk_index:06d}.{file_extension}"
    return os.path.join(session_dir, chunk_filename)


def run_transcript_chunk_pipeline(session_id: str, chunk_index: int):
    for ext in allowed_extensions:
        audio_path_candidate = chunk_file(session_id, chunk_index, ext)
        if os.path.exists(audio_path_candidate):
            audio_path = audio_path_candidate
            break
    transcript_path = chunk_file(session_id, chunk_index)
    print("Step 0 complete: Start analysis")
    # 1. Transcribe audio to text
    transcribe_audio(audio_path, transcript_path)


def run_full_analysis_pipeline(session_id: str):
    print("Step 1 complete: Full analysis started")
    # Wait until all transcript chunks are ready
    session_dir = os.path.join(UPLOAD_DIR, session_id)
    if not os.path.exists(session_dir):
        print("No chunks found for this session.")
        return

    chunk_files = sorted(f for f in os.listdir(session_dir) if f.endswith(".txt"))
    if not chunk_files:
        print("No transcribed chunks available.")
        return

    # Merge all chunk transcripts into a single transcript file
    transcript_path = os.path.join(UPLOAD_DIR, f"{session_id}.txt")
    with open(transcript_path, "w", encoding="utf-8") as outfile:
        for chunk_file in chunk_files:
            with open(os.path.join(session_dir, chunk_file), "r", encoding="utf-8") as infile:
                outfile.write(infile.read() + "\n")

    # 2. Generate meeting summary
    summarize_transcript(session_id, transcript_path)
    print("Step 2 complete: Transcript summarized")
    # 3. Extract key decisions
    extract_decisions_from_transcript(session_id, transcript_path)
    print("Step 3 complete: Decisions extracted")
    # 4. Identify action items / tasks
    extract_tasks_from_transcript(session_id, transcript_path)
    print("Step 4 complete: Tasks extracted")
