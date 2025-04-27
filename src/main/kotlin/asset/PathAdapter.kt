package io.asset

class PathAdapter {

    companion object {
        private val validPathRegex = Regex("^(?!/)[a-zA-Z0-9_~!$'()*+=@/-]*$")
        private const val TREE_PATH_DELIMITER = "."
        private const val URI_PATH_DELIMITER = "/"
        const val TREE_ROOT = "root"
        const val URI_PREFIX = "/assets"
    }

    fun toTreePathFromUriPath(uriPath: String): String {
        val trimmedPath = uriPath.removePrefix(URI_PREFIX)
            .removePrefix(URI_PATH_DELIMITER)
            .removeSuffix(URI_PATH_DELIMITER)
        if (!trimmedPath.matches(validPathRegex)) {
            throw IllegalArgumentException("Invalid path: $trimmedPath")
        }
        return trimmedPath.replace(URI_PATH_DELIMITER, TREE_PATH_DELIMITER).let {
            return if (it.isEmpty()) {
                TREE_ROOT
            } else {
                TREE_ROOT + TREE_PATH_DELIMITER + it
            }
        }
    }

    fun toUriPathFromTreePath(treePath: String): String {
        return URI_PREFIX + URI_PATH_DELIMITER + treePath.replace(TREE_PATH_DELIMITER, URI_PATH_DELIMITER)
    }
}