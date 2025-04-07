#!/bin/bash

set -e  # Остановить при ошибке

echo "📁 Создаём рабочую директорию"
#mkdir -p /app
#cd /app

echo "📦 Обновляем систему и устанавливаем зависимости"
apt-get update && apt-get upgrade -y
apt-get install -y ffmpeg curl python3 python3-pip nano

echo "🐍 Устанавливаем зависимости Python"
# Если у тебя есть requirements.txt — скопируй его в /app перед этим скриптом
pip3 install --upgrade pip
pip3 install -r requirements.txt
pip3 install flash-attn --no-build-isolation

echo "📦 Устанавливаем FastAPI и Uvicorn (на всякий случай)"
pip3 install fastapi uvicorn

echo "✅ Установка завершена"
echo "🌍 Внешний IP сервера:"
curl ifconfig.me
nohup uvicorn assistant:app --host 0.0.0.0 --port 8000 --reload > server.log 2>&1 &
echo "📡 Сервер запушен"