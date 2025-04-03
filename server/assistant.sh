sudo apt-get update && sudo apt-get upgrade
pip install fastapi uvicorn openai-whisper

uvicorn assistant:app --host 0.0.0.0 --port 8000 --reload