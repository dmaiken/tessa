package io.path

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PathAdapterTest {
    private val pathAdapter = PathAdapter()

    @ParameterizedTest
    @ValueSource(strings = ["", "/", "/assets", "/assets/"])
    fun `root path is used when uri path is the root`(uriPath: String) {
        val treePath = pathAdapter.toTreePathFromUriPath(uriPath)

        treePath shouldBe PathAdapter.Companion.TREE_ROOT
    }

    @ParameterizedTest
    @ValueSource(strings = ["/;", "/:", "/.", "/profile-picture/1.2.3"])
    fun `path is rejected if not valid`(uriPath: String) {
        shouldThrow<IllegalArgumentException> {
            pathAdapter.toTreePathFromUriPath(uriPath)
        }
    }

    @Test
    fun `generates the tree path correctly`() {
        val uriPath = "/assets/user1/profile-picture/"

        val treePath = pathAdapter.toTreePathFromUriPath(uriPath)

        treePath shouldBe "${PathAdapter.Companion.TREE_ROOT}.user1.profile-picture"
    }
}
