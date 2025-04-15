from datetime import date

CURRENT_DATE = date.today().isoformat()

PREFIX = ""

BASE_HEADER = (
    f"Current date: {CURRENT_DATE}\n"
)

BASE_PROMPT_HEADER = (
        BASE_HEADER +
        "Audience: Internal team.\n"
        "Output format: natural paragraph-style text.\n"
)

PROMPT_SUMMARIZE = (
        BASE_PROMPT_HEADER +
        "Task: Summarize the key points from the transcript below. Only use what's in the transcript. No extra information.\n"
        "Transcript:\n"
)

PROMPT_DECISIONS = (
        BASE_PROMPT_HEADER +
        "Task: Extract a list of decisions made during the conversation, if any. Use the same language as the transcript.\n"
        "If there are no clear decisions, return: 'no meaningful content found'.\n"
        "Transcript:\n"
)

PROMPT_TASKS = (
        BASE_PROMPT_HEADER +
        "Task: Extract a list of action items or tasks discussed in the transcript, if any. Use natural, clear language from the transcript.\n"
        "If nothing relevant, return: 'no meaningful content found'.\n"
        "Transcript:\n"
)

PROMPT_READY = (
        BASE_PROMPT_HEADER +
        "Task: List all items that were reported as completed, ready, or finalized in the transcript. Use the same language as the transcript.\n"
        "If nothing is complete, return: 'no ready items found'.\n"
        "Transcript:\n"
)
