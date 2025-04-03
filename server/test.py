from fastapi import FastAPI, File, UploadFile, BackgroundTasks, HTTPException
from fastapi.responses import FileResponse

app = FastAPI()

@app.get("/health")
async def health():
    return {"status": "ok"}