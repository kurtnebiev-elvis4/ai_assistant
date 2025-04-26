import gc
import torch
from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig

print("start loading model deepseek-ai/DeepSeek-R1-Distill-Qwen-32B")
gc.collect()
torch.cuda.empty_cache()

model_id = "deepseek-ai/DeepSeek-R1-Distill-Qwen-32B"
bnb_config = BitsAndBytesConfig(
    # load_in_4bit=True,
    # bnb_4bit_use_double_quant=True,
    # bnb_4bit_quant_type="nf4",
    # bnb_4bit_compute_dtype=torch.float16
    load_in_8bit=True
)
tokenizer = AutoTokenizer.from_pretrained(model_id, trust_remote_code=True)
model = AutoModelForCausalLM.from_pretrained(
    model_id,
    device_map="auto",
    quantization_config=bnb_config,
    torch_dtype=torch.float16,
    trust_remote_code=True
)
model = torch.compile(model)
model.eval()
print("end loading model deepseek-ai/DeepSeek-R1-Distill-Qwen-32B")
