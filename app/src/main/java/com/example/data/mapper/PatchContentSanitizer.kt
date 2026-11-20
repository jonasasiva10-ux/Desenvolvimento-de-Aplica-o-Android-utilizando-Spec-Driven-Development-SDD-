package com.example.data.mapper

object PatchContentSanitizer {
    private val steamImageTokenRegex = Regex("""\{STEAM_CLAN_IMAGE\}[^\s\[\]<"]+""")
    private val bbCodeImgRegex = Regex("""\[img].*?\[/img]""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val bbCodeUrlWithTextRegex = Regex("""\[url=[^]]+](.*?)\[/url]""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val bbCodeHeadingRegex = Regex("""\[h[1-6]](.*?)\[/h[1-6]]""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val bbCodeBulletRegex = Regex("""\[\*]""")
    private val bbCodeGenericTagRegex = Regex("""\[/?[a-z0-9=*#\-_/"' ]+]""", RegexOption.IGNORE_CASE)
    private val htmlBreakRegex = Regex("""<br\s*/?>|</p>|</li>""", RegexOption.IGNORE_CASE)
    private val htmlTagRegex = Regex("""<[^>]+>""")
    private val multiBlankLinesRegex = Regex("""\n{3,}""")
    private val multiSpaceRegex = Regex("""[ \t]{2,}""")

    private val patchKeywords = listOf(
        "patch",
        "update",
        "hotfix",
        "changelog",
        "release notes",
        "balance",
        "bug fix",
        "bugfix",
        "atualização",
        "correção",
        "season",
        "versão",
        "v1.",
        "v2.",
        "7.3",
    )

    fun sanitizeFullText(raw: String?): String {
        if (raw.isNullOrBlank()) return "Sem descrição detalhada disponível. Abra a nota oficial para ler o registro completo."
        var text = raw
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(steamImageTokenRegex, "")
            .replace(bbCodeImgRegex, "")
            .replace(bbCodeUrlWithTextRegex, "$1")
            .replace(bbCodeHeadingRegex, "\n$1\n")
            .replace(bbCodeBulletRegex, "\n• ")
            .replace(htmlBreakRegex, "\n")
            .replace(htmlTagRegex, " ")
            .replace(bbCodeGenericTagRegex, "")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")

        text = text.lines()
            .map { it.trim().replace(multiSpaceRegex, " ") }
            .joinToString("\n")
            .replace(multiBlankLinesRegex, "\n\n")
            .trim()

        return text.ifBlank {
            "Consulte a publicação oficial para visualizar todas as modificações deste patch."
        }
    }

    fun buildSummary(sanitizedText: String, maxLength: Int = 180): String {
        val compact = sanitizedText.replace("\n", " ").replace(multiSpaceRegex, " ").trim()
        return if (compact.length <= maxLength) {
            compact
        } else {
            compact.take(maxLength).trimEnd() + "…"
        }
    }

    fun isOfficialPatch(
        title: String,
        feedName: String?,
        tags: List<String>?,
    ): Boolean {
        val hasPatchTag = tags?.any { it.equals("patchnotes", ignoreCase = true) } == true
        if (hasPatchTag) return true
        val normalizedFeed = feedName?.lowercase().orEmpty()
        val isOfficialFeed = normalizedFeed.contains("steam_community_announcements") ||
            normalizedFeed.contains("steam_updates")
        val normalizedTitle = title.lowercase()
        val matchesKeyword = patchKeywords.any { normalizedTitle.contains(it) }
        return matchesKeyword || isOfficialFeed
    }
}
