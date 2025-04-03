from fastapi import FastAPI, File, UploadFile, BackgroundTasks, HTTPException
from fastapi.responses import FileResponse, JSONResponse
import os
import uuid
import asyncio
import soundfile as sf
from nlp_tasks import summarize_transcript, extract_decisions_from_transcript, extract_tasks_from_transcript
from transcription import transcribe_audio

app = FastAPI()

@app.get("/health")
async def health():
    return {"status": "ok"}

# Временная папка для хранения файлов
UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)


@app.post("/upload")
async def upload_audio(file: UploadFile = File(...), background_tasks: BackgroundTasks = None):
    # Генерируем уникальное имя для файла, чтобы избежать конфликтов
    file_id = str(uuid.uuid4())
    file_extension = os.path.splitext(file.filename)[1]

    allowed_extensions = {".wav", ".mp3", ".flac"}
    if file_extension.lower() not in allowed_extensions:
        raise HTTPException(status_code=400, detail="Unsupported file format. Allowed formats: wav, mp3, flac")

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

    # Запускаем транскрипцию в фоне, чтобы не блокировать основной поток
    background_tasks.add_task(run_full_analysis_pipeline, file_id)

    # Возвращаем идентификатор файла для последующего скачивания результата
    return {"message": "Файл получен и обрабатывается", "file_id": file_id}

@app.get("/download/{file_id}")
async def download_result(file_id: str, type: str = "transcript"):
    if type not in RESULT_TYPES:
        raise HTTPException(status_code=400, detail="Unknown result type.")

    suffix = RESULT_TYPES[type]
    file_path = os.path.join(UPLOAD_DIR, f"{file_id}{suffix}.txt")

    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="Requested result not found yet. Try again later.")

    filename = f"{type}.txt" if type != "transcript" else "transcription.txt"
    return FileResponse(path=file_path, media_type="text/plain", filename=filename)

@app.get("/status/{file_id}")
async def get_status(file_id: str):
    result_types = list(RESULT_TYPES.keys())
    status = {}
    for result_type in result_types:
        suffix = "" if result_type == "transcript" else f"_{result_type}"
        file_path = os.path.join(UPLOAD_DIR, f"{file_id}{suffix}.txt")
        status[result_type] = os.path.exists(file_path)

    status["ready"] = all(status[result_type] for result_type in result_types)

    return status

@app.get("/types")
async def get_available_result_types():
    return list(RESULT_TYPES.keys())