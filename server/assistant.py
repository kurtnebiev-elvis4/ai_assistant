import asyncio
import os
import soundfile as sf
import uuid
from fastapi import FastAPI, File, UploadFile, BackgroundTasks, HTTPException, WebSocket, WebSocketDisconnect, WebSocketException
from fastapi.responses import FileResponse, JSONResponse
from chat_bot import chat_with_deepseek

from assistant_background import (run_full_analysis_pipeline, chunk_file,
                                  run_transcript_chunk_pipeline)
from keys import UPLOAD_DIR, allowed_extensions, RESULT_TYPES

app = FastAPI()


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.get("/types")
async def get_available_result_types():
    return list(RESULT_TYPES.keys())


@app.post("/upload")
async def upload_audio(file: UploadFile = File(...), background_tasks: BackgroundTasks = None):
    # Генерируем уникальное имя для файла, чтобы избежать конфликтов
    file_id = str(uuid.uuid4())
    file_extension = os.path.splitext(file.filename)[1]

    if file_extension.lower() not in allowed_extensions:
        raise HTTPException(status_code=400, detail=f"Unsupported file format. Allowed formats: {allowed_extensions}")

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


@app.post("/{session_id}/upload-chunk")
async def upload_chunk(
        session_id: str,
        chunk_index: int,
        is_last_chunk: bool,
        chunk: UploadFile = File(...),
        background_tasks: BackgroundTasks = None
):
    file_extension = os.path.splitext(chunk.filename)[1]
    if file_extension.lower() not in allowed_extensions:
        raise HTTPException(status_code=400, detail=f"Unsupported file format. Allowed formats: {allowed_extensions}")

    chunk_filepath = chunk_file(session_id, chunk_index, file_extension)

    content = await chunk.read()
    if not content:
        raise HTTPException(status_code=400, detail="Uploaded chunk is empty or corrupted")

    with open(chunk_filepath, "wb") as f:
        f.write(content)

    background_tasks.add_task(run_transcript_chunk_pipeline, session_id, chunk_index)

    if is_last_chunk:
        background_tasks.add_task(run_full_analysis_pipeline, session_id)

    return {"message": "Chunk received", "session_id": session_id, "chunk_index": chunk_index,
            "is_last_chunk": is_last_chunk}


@app.post("/{session_id}/analyse")
async def start_analysis(session_id: str, background_tasks: BackgroundTasks = None):
    background_tasks.add_task(run_full_analysis_pipeline, session_id)
    return {"message": "session finished", "session_id": session_id}


@app.get("/{session_id}/download")
async def download_result(session_id: str, type: str = "transcript"):
    if type not in RESULT_TYPES:
        raise HTTPException(status_code=400, detail="Unknown result type.")

    suffix = RESULT_TYPES[type]
    file_path = os.path.join(UPLOAD_DIR, f"{session_id}{suffix}.txt")

    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="Requested result not found yet. Try again later.")

    filename = f"{type}.txt" if type != "transcript" else "transcription.txt"
    return FileResponse(path=file_path, media_type="text/plain", filename=filename)


@app.get("/{session_id}/status")
async def get_status(session_id: str):
    result_types = list(RESULT_TYPES.keys())
    status = {}
    for result_type in result_types:
        suffix = "" if result_type == "transcript" else f"_{result_type}"
        file_path = os.path.join(UPLOAD_DIR, f"{session_id}{suffix}.txt")
        status[result_type] = os.path.exists(file_path)

    status["ready"] = all(status[result_type] for result_type in result_types)

    return status


@app.websocket("/ws/{session_id}")
async def websocket_endpoint(websocket: WebSocket, session_id: str):
    await websocket.accept()
    try:
        while True:
            data = await websocket.receive_text()
            response = chat_with_deepseek(data, user_id=session_id)
            await websocket.send_text(response)
    except WebSocketDisconnect:
        print(f"WebSocket disconnected: {session_id}")
    except WebSocketException as e:
        await websocket.close(code=1003)
        print(f"WebSocket exception: {e}")
