import torch
import gc
from transformers import AutoModelForSpeechSeq2Seq, AutoProcessor, pipeline
import threading

device = "cuda:0" if torch.cuda.is_available() else "cpu"
torch_dtype = torch.float16 if torch.cuda.is_available() else torch.float32

model_id = "openai/whisper-large-v3-turbo"

model = AutoModelForSpeechSeq2Seq.from_pretrained(
    model_id, torch_dtype=torch_dtype,
    low_cpu_mem_usage=True,
    use_safetensors=True,
    # attn_implementation="flash_attention_2"
)
model.to(device)

processor = AutoProcessor.from_pretrained(model_id)

pipe = pipeline(
    "automatic-speech-recognition",
    model=model,
    tokenizer=processor.tokenizer,
    feature_extractor=processor.feature_extractor,
    chunk_length_s=30,
    batch_size=4,
    torch_dtype=torch_dtype,
    device=device,
    return_timestamps=True
)

model_lock = threading.Lock()


def transcribe_audio(input_path: str, output_path: str, output_path_t: str):
    try:
        with model_lock:
            result = pipe(input_path, return_timestamps=True)

        with open(output_path, "w", encoding="utf-8") as f:
            f.write(result["text"])

        # Save transcription with timestamps in a separate file
        with open(output_path_t, "w", encoding="utf-8") as ts_f:
            chunks = result["chunks"]
            if chunks:
                for chunk in chunks:
                    start, end = chunk["timestamp"]
                    text = chunk["text"]
                    ts_f.write(f"[{start:.2f} - {end:.2f}]: {text}\n")
            print(f"Transcription finished")
    except Exception as e:
        print(f"Transcription failed: {e}")
    finally:
        torch.cuda.empty_cache()
        gc.collect()
