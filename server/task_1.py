from transformers import T5Tokenizer, T5ForConditionalGeneration
from langdetect import detect
import torch

tokenizer = T5Tokenizer.from_pretrained("google/flan-t5-small")
model = T5ForConditionalGeneration.from_pretrained("google/flan-t5-small")
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model = model.to(device)

def summarize_transcript(transcript_path="transcripts/latest_transcript.txt") -> str:
    """Generates a summary from a meeting transcript file."""
    with open(transcript_path, "r", encoding="utf-8") as f:
        whisper_text = f.read()
    lang = detect(whisper_text)
    if lang == "ru":
        prompt = "Сделай краткое резюме: "
    else:
        prompt = "Summarize this meeting: "
    input_text = prompt + whisper_text
    input_ids = tokenizer(input_text, return_tensors="pt").input_ids.to(device)
    outputs = model.generate(input_ids)
    summary = tokenizer.decode(outputs[0], skip_special_tokens=True)
    with open(transcript_path + "_summary.txt", "w", encoding="utf-8") as out:
        out.write(summary)
    return summary

def extract_decisions_from_transcript(transcript_path="transcripts/latest_transcript.txt") -> list:
    """Extracts decisions made during a meeting from a transcript file."""
    with open(transcript_path, "r", encoding="utf-8") as f:
        whisper_text = f.read()
    lang = detect(whisper_text)
    if lang == "ru":
        prompt = "Выдели все принятые решения на этом совещании:\n"
    else:
        prompt = "List all decisions made in the following meeting:\n"
    input_text = prompt + whisper_text
    input_ids = tokenizer(input_text, return_tensors="pt").input_ids.to(device)
    outputs = model.generate(input_ids, max_new_tokens=200)
    decoded_text = tokenizer.decode(outputs[0], skip_special_tokens=True)
    decisions = [line.strip("-• ") for line in decoded_text.split("\n") if line.strip()]
    with open(transcript_path + "_decisions.txt", "w", encoding="utf-8") as out:
        out.write("\n".join(decisions))
    return decisions

def extract_tasks_from_transcript(transcript_path="transcripts/latest_transcript.txt") -> list:
    """Extracts action items or tasks discussed in the meeting."""
    with open(transcript_path, "r", encoding="utf-8") as f:
        whisper_text = f.read()
    lang = detect(whisper_text)
    if lang == "ru":
        prompt = "Перечисли задачи и действия, которые нужно выполнить после этого совещания:\n"
    else:
        prompt = "List all action items and tasks discussed in this meeting:\n"
    input_text = prompt + whisper_text
    input_ids = tokenizer(input_text, return_tensors="pt").input_ids.to(device)
    outputs = model.generate(input_ids, max_new_tokens=200)
    decoded_text = tokenizer.decode(outputs[0], skip_special_tokens=True)
    tasks = [line.strip("-• ") for line in decoded_text.split("\n") if line.strip()]
    with open(transcript_path + "_tasks.txt", "w", encoding="utf-8") as out:
        out.write("\n".join(tasks))
    return tasks