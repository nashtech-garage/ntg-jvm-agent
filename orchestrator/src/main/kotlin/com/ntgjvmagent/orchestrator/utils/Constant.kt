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
        1. Use the snippets as factual evidence ONLY IF they are relevant to the user's question.
        2. Determine relevance by checking whether the snippet meaningfully answers, clarifies, or provides data directly related to the user query.
        3. If one or more results are relevant:
           - Synthesize a clean, conversational answer based on those relevant snippets.
           - Embed citations inline using Markdown format: [title](URL)
           - Apply citations ONLY to statements supported by relevant snippets.
           - Example:
               "It may rain lightly today [Baomoi](https://baomoi.com/abc)."
        4. If the search results exist but NONE of them are relevant:
           - Ignore the search results completely.
           - Provide a general, helpful answer WITHOUT any links.
        5. If the tool returns no results:
           - Provide a general answer based only on your internal knowledge.
        Rules:
        - Do NOT list citations at the end of the answer.
        - Do NOT attach a link to every sentence. Only cite where evidence is actually used.
        - Prefer the most relevant sources first (best snippet match).
        - Maintain a natural, conversational tone.
    """
    const val CHATGPT_DIMENSION = 1536
    const val GEMINI_DIMENSION = 768
    const val KB = 1024
    const val MB = KB * 1024
}
