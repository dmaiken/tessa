package io.image

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ImagePropertiesTest {

    @ParameterizedTest
    @ValueSource(ints = [-1, 0])
    fun `PreProcessingProperties maxHeight cannot be less than 0`(maxHeight: Int) {
        val exception = shouldThrow<IllegalArgumentException> {
            PreProcessingProperties.create(
                maxHeight = maxHeight,
                maxWidth = 100
            )
        }

        exception.message shouldBe "'maxHeight' must be greater than 0"
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, 0])
    fun `PreProcessingProperties maxWidth cannot be less than 0`(maxWidth: Int) {
        val exception = shouldThrow<IllegalArgumentException> {
            PreProcessingProperties.create(
                maxWidth = maxWidth,
                maxHeight = 100
            )
        }

        exception.message shouldBe "'maxWidth' must be greater than 0"
    }

    @Test
    fun `PreProcessingProperties maxHeight can be null`() {
        shouldNotThrowAny {
            PreProcessingProperties.create(
                maxWidth = 100,
                maxHeight = null
            )
        }
    }

    @Test
    fun `PreProcessingProperties maxWidth can be null`() {
        shouldNotThrowAny {
            PreProcessingProperties.create(
                maxWidth = null,
                maxHeight = 100
            )
        }
    }
}