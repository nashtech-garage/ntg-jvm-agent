package com.ntgjvmagent.orchestrator.utils

object Constant {
    const val SUMMARY_PROMPT = "Summarize the following user question into a short title (max 7 words), no punctuation:"
    const val QUESTION_TYPE = 1
    const val ANSWER_TYPE = 2
    const val TOP_K = 5
    const val PNG_CONTENT_TYPE = "image/png"
    const val MAXIMUM_UPLOAD_FILE_SIZE = 5 * 1024 * 1024 // 5MB

    const val SEARCH_TOOL_INSTRUCTION = """
        When the tool response contains search results (titles, snippets, links):
        1. Treat the snippets as factual content that you must base your answer on.
        2. ALWAYS synthesize a direct, natural-language answer for the user based on the snippets.
        3. DO NOT EVER reply “I don’t know”, “I cannot provide”, or ask the user to read the links.
        4. When creating your answer, embed citations IN-LINE, directly next to the information they support.
        5. Inline citation format MUST be: [title](URL). Example:
            "It is expected to rain lightly [title](https://baomoi.com/abc)"

        Rules:
        - Do NOT list citations at the end.
        - Each important factual claim MUST reference one of the search result links inline.
        - Prefer the most relevant sources first (highest snippet relevance).
        - You MUST generate a clean, readable, conversational answer.
    """
}
