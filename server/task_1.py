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

BASE_PROMPT_HEADER = (
    f"Current date: {CURRENT_DATE}\n"
    f"Language of the transcript: {{lang}}\n"
    "Audience: Internal team.\n"
    "Format: Paragraph style with clear, natural language.\n"
    "Role: You are a highly skilled professional in summarizing and interpreting team discussions with precision and clarity.\n"
    "You are an expert in organizational communication, team alignment, and extracting key insights from collaborative conversations.\n"
)

PROMPT_SUMMARIZE = (
        BASE_PROMPT_HEADER +
        "Instruction (in English): If the following transcript contains a structured discussion such as a meeting or collaborative work session, summarize the key points in a concise paragraph. "
        "Do not add any information that is not explicitly mentioned.\n"
        "Transcript starts below:\n"
)
PROMPT_DECISIONS = (
        BASE_PROMPT_HEADER +
        "Instruction (in English): If the following transcript contains a structured discussion such as a meeting or collaborative work session, identify and list all decisions made, if any. "
        "Use the same language as the transcript. Avoid repeating phrases and ensure clarity. "
        "If the content is not a meeting or lacks meaningful discussion, return the message: 'no meaningful content found'.\n"
        "Meeting transcript starts below:\n"
)
PROMPT_TASKS = (
        BASE_PROMPT_HEADER +
        "Instruction (in English): If the following transcript contains a structured discussion such as a meeting or collaborative work session, identify and list all action items and tasks discussed, if any. "
        "Use the same language as the transcript. Avoid redundancy and use clear, natural language. "
        "If the content is not a meeting or lacks meaningful discussion, return the message: 'no meaningful content found'.\n"
        "Meeting transcript starts below:\n"
)


def generate_text_chunks(prompt: str, text: str) -> str:
    text = text + "<think>\n"

    max_len = tokenizer.model_max_length
    print(f"Max length: {max_len}")
    encoded_prompt = tokenizer(prompt, return_tensors="pt", padding=True, return_attention_mask=True)
    inputs = encoded_prompt["input_ids"].to(model.device)
    prompt_attention_mask = encoded_prompt["attention_mask"].to(model.device)
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
        print(
            f"Prompt tokens: {prompt_len}, Chunk tokens: {len(current_chunk)}, Total: {prompt_len + len(current_chunk)}")
        chunk_input = torch.tensor(chunk, dtype=torch.long, device=model.device).unsqueeze(0)  # shape: [1, len(chunk)]
        input_ids = torch.cat([inputs, chunk_input], dim=1)
        chunk_attention_mask = torch.ones_like(chunk_input)
        attention_mask = torch.cat([prompt_attention_mask, chunk_attention_mask], dim=1)
        with model_lock:
            max_output_tokens = min(MAX_RESPONSE_TOKENS, max_len - input_ids.shape[1])
            outputs = model.generate(
                input_ids=input_ids,
                attention_mask=attention_mask,
                max_new_tokens=max_output_tokens,
                do_sample=True,
                temperature=0.6,
                top_p=0.95,
                pad_token_id=tokenizer.pad_token_id or tokenizer.eos_token_id,
            )
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
