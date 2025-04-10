import os
from ai_model import model, tokenizer
from langdetect import detect
import torch
import threading
from datetime import date
from keys import UPLOAD_DIR

MAX_RESPONSE_TOKENS = 1024

CURRENT_DATE = date.today().isoformat()
model_lock = threading.Lock()

PROMPT_SUMMARIZE = (
    f"Current date: {CURRENT_DATE}\n"
    f"Language of the transcript: {{lang}}\n"
    "Audience: Internal team.\n"
    "Format: Paragraph style with clear, natural language.\n"
    "Role: You are a highly skilled professional in summarizing and interpreting team discussions with precision and clarity.\n"
    "Instruction (in English): If the following transcript contains a structured discussion such as a meeting or collaborative work session, summarize the key points in a concise paragraph. "
    "Do not add any information that is not explicitly mentioned. If the content is not a meeting or lacks meaningful discussion (e.g., random text, unrelated talk), return the message: 'no meaningful content found'.\n"
    "Meeting transcript starts below:\n"
)
PROMPT_DECISIONS = (
    f"Current date: {CURRENT_DATE}\n"
    f"Language of the transcript: {{lang}}\n"
    "Audience: Internal team.\n"
    "Format: Paragraph style with clear, natural language.\n"
    "Role: You are a highly skilled professional in summarizing and interpreting team discussions with precision and clarity. "
    "You are an expert in organizational communication, team alignment, and extracting key insights from collaborative conversations.\n"
    "Instruction (in English): If the following transcript contains a structured discussion such as a meeting or collaborative work session, identify and list all decisions made, if any. "
    "Use the same language as the transcript. Avoid repeating phrases and ensure clarity. "
    "If the content is not a meeting or lacks meaningful discussion, return the message: 'no meaningful content found'.\n"
    "Meeting transcript starts below:\n"
)
PROMPT_TASKS = (
    f"Current date: {CURRENT_DATE}\n"
    f"Language of the transcript: {{lang}}\n"
    "Audience: Internal team.\n"
    "Format: Paragraph style with clear, natural language.\n"
    "Role: You are a highly skilled professional in summarizing and interpreting team discussions with precision and clarity. "
    "You are an expert in organizational communication, team alignment, and extracting key insights from collaborative conversations.\n"
    "Instruction (in English): If the following transcript contains a structured discussion such as a meeting or collaborative work session, identify and list all action items and tasks discussed, if any. "
    "Use the same language as the transcript. Avoid redundancy and use clear, natural language. "
    "If the content is not a meeting or lacks meaningful discussion, return the message: 'no meaningful content found'.\n"
    "Meeting transcript starts below:\n"
)


def generate_text_chunks(prompt: str, text: str) -> str:
    max_len = tokenizer.model_max_length
    inputs = tokenizer(prompt, return_tensors="pt").input_ids.to(model.device)
    prompt_len = inputs.shape[1]


    transcript_tokens = tokenizer(text, return_tensors="pt").input_ids[0]
    chunks = []
    current_chunk = []
    current_len = prompt_len

    for token in transcript_tokens:
        if current_len + 1 > max_len - MAX_RESPONSE_TOKENS:
            chunks.append(current_chunk)
            current_chunk = [token]
            current_len = prompt_len + 1
        else:
            current_chunk.append(token)
            current_len += 1
    if current_chunk:
        chunks.append(current_chunk)
    torch.cuda.empty_cache()
    full_output = ""
    for chunk in chunks:
        print(f"Prompt tokens: {prompt_len}, Chunk tokens: {len(current_chunk)}, Total: {prompt_len + len(current_chunk)}")
        chunk_tensor = torch.tensor([chunk], dtype=torch.long, device=model.device)
        input_ids = torch.cat([inputs, chunk_tensor], dim=1)
        with model_lock:
            max_output_tokens = min(MAX_RESPONSE_TOKENS, max_len - input_ids.shape[1])
            outputs = model.generate(input_ids, max_new_tokens=max_output_tokens,
                                     do_sample=True,
                                     temperature=0.6,
                                     top_p=0.95)
            torch.cuda.empty_cache()
        output_text = tokenizer.decode(outputs[0], skip_special_tokens=True)
        trimmed_output = output_text.replace(prompt, "", 1).replace(text, "", 1).lstrip()
        full_output += trimmed_output + "\n"

    return full_output.strip()


def summarize_transcript(file_id: str, transcript_path: str) -> str:
    """Generates a summary from a meeting transcript file."""
    with open(transcript_path, "r", encoding="utf-8") as f:
        whisper_text = f.read()
    lang = detect(whisper_text)
    prompt = PROMPT_SUMMARIZE.format(lang=lang.upper())

    summary = generate_text_chunks(prompt, whisper_text)

    output_path = os.path.join(UPLOAD_DIR, f"{file_id}_summary.txt")
    with open(output_path, "w", encoding="utf-8") as out:
        out.write(summary)
    return summary


def extract_decisions_from_transcript(file_id: str, transcript_path: str) -> list:
    """Extracts decisions made during a meeting from a transcript file."""
    with open(transcript_path, "r", encoding="utf-8") as f:
        whisper_text = f.read()
    lang = detect(whisper_text)
    prompt = PROMPT_DECISIONS.format(lang=lang.upper())

    decoded_text = generate_text_chunks(prompt, whisper_text)

    decisions = [line.strip("-• ") for line in decoded_text.split("\n") if line.strip()]
    output_path = os.path.join(UPLOAD_DIR, f"{file_id}_decisions.txt")
    with open(output_path, "w", encoding="utf-8") as out:
        out.write("\n".join(decisions))
    return decisions


def extract_tasks_from_transcript(file_id: str, transcript_path: str) -> list:
    """Extracts action items or tasks discussed in the meeting."""
    with open(transcript_path, "r", encoding="utf-8") as f:
        whisper_text = f.read()
    lang = detect(whisper_text)
    prompt = PROMPT_TASKS.format(lang=lang.upper())

    decoded_text = generate_text_chunks(prompt, whisper_text)

    tasks = [line.strip("-• ") for line in decoded_text.split("\n") if line.strip()]
    output_path = os.path.join(UPLOAD_DIR, f"{file_id}_tasks.txt")
    with open(output_path, "w", encoding="utf-8") as out:
        out.write("\n".join(tasks))
    return tasks
