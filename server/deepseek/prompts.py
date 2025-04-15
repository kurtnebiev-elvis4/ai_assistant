from datetime import date

CURRENT_DATE = date.today().isoformat()

PREFIX = "<think>\n"

BASE_HEADER = (
    f"Current date: {CURRENT_DATE}\n"
    f"Language of the transcript: {{lang}}\n"
)

BASE_PROMPT_HEADER = (
        BASE_HEADER +
        "Audience: Internal team.\n"
        "Format: Paragraph style with clear, natural language.\n"
        "Role: You are a highly skilled professional in summarizing and interpreting team discussions with precision and clarity.\n"
        "You are an expert in organizational communication, team alignment, and extracting key insights from collaborative conversations.\n"
)

PROMPT_SUMMARIZE = (
        BASE_PROMPT_HEADER +
        "Instruction (in English): If the following transcript contains a structured discussion such as a meeting or collaborative work session, summarize the key points in a concise paragraph. "
        "Do not add any information that is not explicitly mentioned.\n"
        "Transcript starts below:\n"
)
PROMPT_DECISIONS = (
        BASE_PROMPT_HEADER +
        "Instruction (in English): If the following transcript contains a structured discussion such as a meeting or collaborative work session, identify and list all decisions made, if any. "
        "Use the same language as the transcript. Avoid repeating phrases and ensure clarity. "
        "If the content is not a meeting or lacks meaningful discussion, return the message: 'no meaningful content found'.\n"
        "Meeting transcript starts below:\n"
)
PROMPT_TASKS = (
        BASE_PROMPT_HEADER +
        "Instruction (in English): If the following transcript contains a structured discussion such as a meeting or collaborative work session, identify and list all action items and tasks discussed, if any. "
        "Use the same language as the transcript. Avoid redundancy and use clear, natural language. "
        "If the content is not a meeting or lacks meaningful discussion, return the message: 'no meaningful content found'.\n"
        "Meeting transcript starts below:\n"
)
PROMPT_READY = (
        BASE_PROMPT_HEADER +
        "Instruction (in English): If the following transcript contains a structured discussion such as a meeting or collaborative work session, identify and list all items that were marked as complete or ready. "
        "This may include completed tasks, finished components, finalized decisions, or any work that was reported as done. "
        "Use the same language as the transcript. Be clear and avoid repeating unnecessary details. "
        "If nothing is clearly marked as complete, return the message: 'no ready items found'.\n"
        "Transcript starts below:\n"
)
