package com.ntgjvmagent.orchestrator.unit.support

import org.springframework.ai.document.Document
import org.springframework.ai.embedding.Embedding
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse
import kotlin.math.sqrt

/**
 * Deterministic bag-of-words embedding, so cosine similarity tracks word overlap.
 *
 * Lets a test drive a real SimpleVectorStore -- filter expressions and ranking included --
 * without a model call or a fixed vector that makes every document equally similar.
 */
object BagOfWordsEmbeddingModel : EmbeddingModel {
    private const val DIMS = 64

    override fun embed(text: String): FloatArray {
        val vector = FloatArray(DIMS)
        text
            .lowercase()
            .split(Regex("\\W+"))
            .filter { it.isNotBlank() }
            .forEach { word -> vector[Math.floorMod(word.hashCode(), DIMS)] += 1f }
        val norm = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
        return if (norm == 0f) vector else FloatArray(DIMS) { vector[it] / norm }
    }

    override fun dimensions(): Int = DIMS

    override fun embed(document: Document): FloatArray = embed(document.text.orEmpty())

    override fun call(request: EmbeddingRequest): EmbeddingResponse =
        EmbeddingResponse(request.instructions.mapIndexed { index, text -> Embedding(embed(text), index) })
}
