package io.image

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class ImageFormatTest {

    companion object {

        @JvmStatic
        fun supportedImageFormatSource(): Stream<Arguments> = Stream.of(
            arguments("jpg", ImageFormat.JPEG),
            arguments("jpeg", ImageFormat.JPEG),
            arguments("png", ImageFormat.PNG),
            arguments("webp", ImageFormat.WEBP),
            arguments("avif", ImageFormat.AVIF)
        )

        @JvmStatic
        fun supportedImageMimeTypeSource(): Stream<Arguments> = Stream.of(
            arguments("image/jpeg", ImageFormat.JPEG),
            arguments("image/png", ImageFormat.PNG),
            arguments("image/webp", ImageFormat.WEBP),
            arguments("image/avif", ImageFormat.AVIF)
        )
    }

    @ParameterizedTest
    @MethodSource("supportedImageFormatSource")
    fun `test fromFormat with supported image type`(string: String, expected: ImageFormat) {
        ImageFormat.fromFormat(string) shouldBe expected
    }

    @Test
    fun `test fromFormat with unsupported image type`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ImageFormat.fromFormat("unsupported")
        }
        exception.message shouldBe "Unsupported image format: unsupported"
    }

    @ParameterizedTest
    @MethodSource("supportedImageMimeTypeSource")
    fun `test from mimeType with supported mimeType`(mimeType: String, expected: ImageFormat) {
        ImageFormat.fromMimeType(mimeType) shouldBe expected
    }

    @Test
    fun `test fromMimeType with unsupported image type`() {
        val exception = shouldThrow<IllegalArgumentException> {
            ImageFormat.fromMimeType("unsupported")
        }
        exception.message shouldBe "Unsupported image mime type: unsupported"
    }
}