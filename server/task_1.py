from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig
from langdetect import detect
import torch
import os
import gc

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


def summarize_transcript(file_id: str, transcript_path: str) -> str:
    """Generates a summary from a meeting transcript file."""
    with open(transcript_path, "r", encoding="utf-8") as f:
        whisper_text = f.read()
    lang = detect(whisper_text)
    prompt = "Сделай краткое резюме: " if lang == "ru" else "Summarize this meeting: "
    input_text = prompt + whisper_text
    input_ids = tokenizer(input_text, return_tensors="pt").input_ids.to(model.device)
    outputs = model.generate(input_ids, max_new_tokens=200, do_sample=True, temperature=0.7, top_p=0.95)
    summary = tokenizer.decode(outputs[0], skip_special_tokens=True)
    output_path = os.path.join(UPLOAD_DIR, f"{file_id}_summary.txt")
    with open(output_path, "w", encoding="utf-8") as out:
        out.write(summary)
    return summary


def extract_decisions_from_transcript(file_id: str, transcript_path: str) -> list:
    """Extracts decisions made during a meeting from a transcript file."""
    with open(transcript_path, "r", encoding="utf-8") as f:
        whisper_text = f.read()
    lang = detect(whisper_text)
    prompt = "Выдели все принятые решения на этом совещании:\n" if lang == "ru" else "List all decisions made in the following meeting:\n"
    input_text = prompt + whisper_text
    input_ids = tokenizer(input_text, return_tensors="pt").input_ids.to(model.device)
    outputs = model.generate(input_ids, max_new_tokens=200, do_sample=True, temperature=0.7, top_p=0.95)
    decoded_text = tokenizer.decode(outputs[0], skip_special_tokens=True)
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
    prompt = "Перечисли задачи и действия, которые нужно выполнить после этого совещания:\n" if lang == "ru" else "List all action items and tasks discussed in this meeting:\n"
    input_text = prompt + whisper_text
    input_ids = tokenizer(input_text, return_tensors="pt").input_ids.to(model.device)
    outputs = model.generate(input_ids, max_new_tokens=200, do_sample=True, temperature=0.7, top_p=0.95)
    decoded_text = tokenizer.decode(outputs[0], skip_special_tokens=True)
    tasks = [line.strip("-• ") for line in decoded_text.split("\n") if line.strip()]
    output_path = os.path.join(UPLOAD_DIR, f"{file_id}_tasks.txt")
    with open(output_path, "w", encoding="utf-8") as out:
        out.write("\n".join(tasks))
    return tasks