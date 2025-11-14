package com.ntgjvmagent.orchestrator.utils

object Constant {
    const val SUMMARY_PROMPT = "Summarize the following user question into a short title (max 7 words), no punctuation:"
    const val QUESTION_TYPE = 1
    const val ANSWER_TYPE = 2
    const val TOP_K = 5
    const val PNG_CONTENT_TYPE = "image/png"
    const val MAXIMUM_UPLOAD_FILE_SIZE = 5 * 1024 * 1024 // 5MB
    const val SUMMARY_UPDATE_PROMPT = """
    You create a new running conversation summary.

    Goal:
    - Produce a short, factual summary based on:
      (1) The previous summary
      (2) The newest user/assistant message

    Given the recent messages update the summary in a structured way.

    Rules:
    - Keep only information relevant for continuing the conversation.
    - Track multiple topics separately.
    - Keep a list of unresolved questions.
    - Preserve the user’s preferences.
    - Remove obsolete or irrelevant threads.

    Output format MUST follow this structure:

    ACTIVE_TOPICS:
      - <topic1>:
          * fact or decision
          * fact or detail
      - <topic2>:
          * ...

    OPEN_QUESTIONS:
      - <list or empty>

    USER_PREFERENCES:
      - <list or empty>

    Recent message:
    {{latest_message}}

    Return the updated summary.
    """
}
