
chmod +x assistant.sh

sudo apt-get update && sudo apt-get upgrade
pip install fastapi uvicorn openai-whisper

upload file assistant.py
uvicorn assistant:app --host 0.0.0.0 --port 8000 --reload

http://3.83.49.52:8000/health



=============================================


apt update
apt install nano

pip install git+https://github.com/huggingface/transformers@f742a644ca32e65758c3adb36225aef1731bd2a8
pip install accelerate

pip install qwen-omni-utils


pip uninstall torch torchvision torchaudio -y
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu118

nvidia-smi // check attention