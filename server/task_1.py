import gc
import os
import torch
from langdetect import detect
from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig

from keys import UPLOAD_DIR

UPLOAD_DIR = "uploads"

gc.collect()
torch.cuda.empty_cache()

# Загрузка Qwen 14B (Distill)
model_id = "deepseek-ai/DeepSeek-R1-Distill-Qwen-14B"
bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_use_double_quant=True,
    bnb_4bit_quant_type="nf4",
    bnb_4bit_compute_dtype=torch.float16
)
tokenizer = AutoTokenizer.from_pretrained(model_id, trust_remote_code=True)
model = AutoModelForCausalLM.from_pretrained(
    model_id,
    device_map="auto",
    quantization_config=bnb_config,
    trust_remote_code=True
)
model.eval()


def generate_text_chunks(prompt: str, text: str) -> str:
    max_len = tokenizer.model_max_length
    inputs = tokenizer(prompt, return_tensors="pt").input_ids.to(model.device)
    prompt_len = inputs.shape[1]

    transcript_tokens = tokenizer(text, return_tensors="pt").input_ids[0]
    chunks = []
    current_chunk = []
    current_len = prompt_len

    for token in transcript_tokens:
        if current_len + 1 > max_len:
            chunks.append(current_chunk)
            current_chunk = [token]
            current_len = prompt_len + 1
        else:
            current_chunk.append(token)
            current_len += 1
    if current_chunk:
        chunks.append(current_chunk)

    full_output = ""
    for chunk in chunks:
        input_ids = torch.cat([inputs, torch.tensor([chunk], device=model.device)], dim=1)
        outputs = model.generate(input_ids, max_new_tokens=200, do_sample=True, temperature=0.7, top_p=0.95)
        output_text = tokenizer.decode(outputs[0], skip_special_tokens=True)
        trimmed_output = output_text.replace(prompt, "", 1).replace(text, "", 1).lstrip()
        full_output += trimmed_output + "\n"

    return full_output.strip()


def summarize_transcript(file_id: str, transcript_path: str) -> str:
    """Generates a summary from a meeting transcript file."""
    with open(transcript_path, "r", encoding="utf-8") as f:
        whisper_text = f.read()
    lang = detect(whisper_text)
    prompt = f"Summarize this meeting and respond in language {lang.upper()}:"

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
    prompt = f"List all decisions made in the following meeting and respond in language {lang.upper()}:\n"

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
    prompt = f"List all action items and tasks discussed in this meeting and respond in language {lang.upper()}:\n"

    decoded_text = generate_text_chunks(prompt, whisper_text)

    tasks = [line.strip("-• ") for line in decoded_text.split("\n") if line.strip()]
    output_path = os.path.join(UPLOAD_DIR, f"{file_id}_tasks.txt")
    with open(output_path, "w", encoding="utf-8") as out:
        out.write("\n".join(tasks))
    return tasks
