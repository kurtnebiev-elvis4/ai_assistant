from deepseek.deepseek_r1_32b import model, tokenizer
import torch

# Истории диалогов по пользователям
user_histories = {}


def chat_with_deepseek(prompt, user_id, max_new_tokens=512):
    if user_id not in user_histories:
        user_histories[user_id] = [
            {"role": "system",
             "content": "Ты — дружелюбный и полезный ассистент. Отвечай понятно, вежливо и по существу. Если не знаешь ответа — честно скажи об этом."}
        ]

    history = user_histories[user_id]
    history.append({"role": "user", "content": prompt})

    input_text = ""
    for turn in history:
        input_text += f"{turn['role']}: {turn['content']}\n"
    input_text += "assistant:"

    inputs = tokenizer(input_text, return_tensors="pt").to(model.device)
    outputs = model.generate(
        **inputs,
        max_new_tokens=max_new_tokens,
        do_sample=True,
        temperature=0.7,
        top_p=0.9,
        pad_token_id=tokenizer.eos_token_id
    )

    response = tokenizer.decode(outputs[0], skip_special_tokens=True)
    assistant_reply = response.split("assistant:")[-1].strip()

    history.append({"role": "assistant", "content": assistant_reply})
    return assistant_reply
