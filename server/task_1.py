import os
from deepseek.deepseek_r1_32b import model, tokenizer
from qwen.prompts import PREFIX, PROMPT_SUMMARIZE, PROMPT_DECISIONS, PROMPT_TASKS, PROMPT_READY, BASE_HEADER
from langdetect import detect
import torch
import threading

from keys import UPLOAD_DIR

MAX_RESPONSE_TOKENS = 1024

model_lock = threading.Lock()


def generate_text_chunks(prompt: str, text: str) -> str:
    text = text + PREFIX

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


def extract_ready_items_from_transcript(file_id: str, transcript_path: str) -> list:
    """Extracts items marked as done or ready during the meeting."""
    with open(transcript_path, "r", encoding="utf-8") as f:
        whisper_text = f.read()
    lang = detect(whisper_text)
    prompt = PROMPT_READY.format(lang=lang.upper())

    decoded_text = generate_text_chunks(prompt, whisper_text)

    ready_items = [line.strip("-• ") for line in decoded_text.split("\n") if line.strip()]
    output_path = os.path.join(UPLOAD_DIR, f"{file_id}_ready.txt")
    with open(output_path, "w", encoding="utf-8") as out:
        out.write("\n".join(ready_items))
    return ready_items


def analyze_with_custom_prompt(file_id: str, transcript_path: str, label: str, prompt: str) -> str:
    """Runs a custom prompt on the transcript and saves the output."""
    print(f"[Prompt Label]: {label} [Prompt Used]:\n{prompt}")

    with open(transcript_path, "r", encoding="utf-8") as f:
        whisper_text = f.read()
    lang = detect(whisper_text)
    final_prompt = BASE_HEADER.format(lang=lang.upper()) + prompt + "\nTranscript starts below:\n"

    result = generate_text_chunks(final_prompt, whisper_text)

    output_path = os.path.join(UPLOAD_DIR, f"{file_id}_{label}.txt")
    with open(output_path, "w", encoding="utf-8") as out:
        out.write(result)
    return result
