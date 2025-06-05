package io

class WildcardRegexAdapter {
    fun toRegex(pathPattern: String): Regex {
        val sb = StringBuilder("^")
        val parts =
            pathPattern.lowercase().split("/")
                .filterNot { it.isEmpty() }

        parts.forEach { part ->
            sb.append("\\/")
            sb.append(
                when (part) {
                    "**" -> ".*"
                    "*" -> "[^/]+"
                    else -> part
                },
            )
        }

        sb.append("\\/?")
        sb.append("$")
        return Regex(sb.toString())
    }
}
