from fastapi import FastAPI, File, UploadFile, BackgroundTasks, HTTPException
from fastapi.responses import FileResponse
import os
import uuid
import asyncio
import soundfile as sf


app = FastAPI()

import torch
from transformers import AutoModelForSpeechSeq2Seq, AutoProcessor, pipeline

device = "cuda:0" if torch.cuda.is_available() else "cpu"
torch_dtype = torch.float16 if torch.cuda.is_available() else torch.float32

model_id = "openai/whisper-large-v3-turbo"

model = AutoModelForSpeechSeq2Seq.from_pretrained(
    model_id, torch_dtype=torch_dtype, low_cpu_mem_usage=True, use_safetensors=True
)
model.to(device)

processor = AutoProcessor.from_pretrained(model_id)

pipe = pipeline(
    "automatic-speech-recognition",
    model=model,
    tokenizer=processor.tokenizer,
    feature_extractor=processor.feature_extractor,
    torch_dtype=torch_dtype,
    device=device,
)

# Временная папка для хранения файлов
UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

def transcribe_audio(input_path: str, output_path: str):
    try:
        result = pipe(input_path)
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(result["text"])
    except Exception as e:
        print(f"Transcription failed: {e}")

@app.post("/upload")
async def upload_audio(file: UploadFile = File(...), background_tasks: BackgroundTasks = None):
    # Генерируем уникальное имя для файла, чтобы избежать конфликтов
    file_id = str(uuid.uuid4())
    file_extension = os.path.splitext(file.filename)[1]

    allowed_extensions = {".wav", ".mp3", ".flac", ".m4a"}
    if file_extension.lower() not in allowed_extensions:
        raise HTTPException(status_code=400, detail="Unsupported file format. Allowed formats: wav, mp3, flac, m4a")

    input_filename = f"{file_id}{file_extension}"
    input_filepath = os.path.join(UPLOAD_DIR, input_filename)

    # Сохраняем полученный аудиофайл
    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="Uploaded file is empty or corrupted")

    with open(input_filepath, "wb") as f:
        f.write(content)

    try:
        sf.info(input_filepath)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Invalid audio file: {e}")

    # Задаём имя для файла с транскрипцией
    output_filename = f"{file_id}.txt"
    output_filepath = os.path.join(UPLOAD_DIR, output_filename)

    # Запускаем транскрипцию в фоне, чтобы не блокировать основной поток
    background_tasks.add_task(transcribe_audio, input_filepath, output_filepath)

    # Возвращаем идентификатор файла для последующего скачивания результата
    return {"message": "Файл получен и обрабатывается", "file_id": file_id}

@app.get("/download/{file_id}")
async def download_transcription(file_id: str):
    output_filepath = os.path.join(UPLOAD_DIR, f"{file_id}.txt")
    # Проверяем, что файл транскрипции существует
    if not os.path.exists(output_filepath):
        raise HTTPException(status_code=404, detail="Файл транскрипции ещё не готов. Попробуйте позже.")

    # Отправляем файл для скачивания
    return FileResponse(path=output_filepath, media_type="text/plain", filename="transcription.txt")

# Дополнительно можно добавить эндпоинт для проверки статуса обработки,
# если вы захотите реализовать более гибкую логику.
@app.get("/health")
async def health():
    return {"status": "ok"}