package com.ntgjvmagent.orchestrator.ingestion.utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object ContentExtractor {
    fun extractMainContent(html: String): String {
        val doc = Jsoup.parse(html)

        doc.removeBoilerplate()

        val content: Element =
            doc.selectFirst("article, main")
                ?: doc.selectFirst(
                    """
                    div#content, div#main, div#primary, div#page,
                    div.post, div.article, div.entry, div.entry-content,
                    div.post-content, div#main-content,
                    section.content, section#content
                    """.trimIndent(),
                )
                ?: doc.body()

        content.cleanInnerContent()

        return content.text().trim()
    }

    // ---------- Extensions ---------- //

    private fun Document.removeBoilerplate() {
        // Remove non-content / structural noise
        this.select("header, nav, footer, aside, noscript, script, style, link, meta, svg").remove()

        // Remove ads, promos, banners
        this.select("[class*=ad], [id*=ad], [class*=promo], [id*=promo], [class*=banner], [id*=banner]").remove()

        // Sidebar removal
        this.select("[class*=sidebar], [id*=sidebar]").remove()
    }

    private fun Element.cleanInnerContent() {
        // Social buttons, comment boxes
        this.select("[class*=share], [class*=social], [id*=comments], [class*=comments]").remove()

        // Scripts and styles inside content
        this.select("script, style, svg, noscript").remove()
    }
}
