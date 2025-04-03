#!/bin/bash

set -e  # Остановить при ошибке

echo "📁 Создаём рабочую директорию"
mkdir -p /app
cd /app

echo "📦 Обновляем систему и устанавливаем зависимости"
sudo apt-get update && sudo apt-get upgrade -y
sudo apt-get install -y ffmpeg curl python3 python3-pip

echo "🐍 Устанавливаем зависимости Python"
# Если у тебя есть requirements.txt — скопируй его в /app перед этим скриптом
pip3 install --upgrade pip
pip3 install -r requirements.txt

echo "📦 Устанавливаем FastAPI и Uvicorn (на всякий случай)"
pip3 install fastapi uvicorn

echo "✅ Установка завершена"
echo "📡 Запустить сервер можно командой:"
echo "uvicorn assistant:app --host 0.0.0.0 --port 8000 --reload"