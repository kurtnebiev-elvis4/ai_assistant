# Use official Python image
FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get upgrade -y && apt-get install -y \
    ffmpeg \
    curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --upgrade pip && pip install -r requirements.txt


# Copy requirements directly
RUN pip install fastapi uvicorn

# Copy your FastAPI app files into the container
COPY . .

# Expose the port uvicorn will run on
EXPOSE 8000

# Start the server
CMD ["uvicorn", "assistant:app", "--host", "0.0.0.0", "--port", "8000", "--reload"]