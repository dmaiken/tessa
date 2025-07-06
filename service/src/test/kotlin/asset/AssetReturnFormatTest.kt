package asset

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class AssetReturnFormatTest {
    companion object {
        @JvmStatic
        fun fromQueryParamSource(): Stream<Arguments> =
            Stream.of(
                Arguments.of("redirect", AssetReturnFormat.REDIRECT),
                Arguments.of("metadata", AssetReturnFormat.METADATA),
                Arguments.of("content", AssetReturnFormat.CONTENT),
                Arguments.of("REDIRECT", AssetReturnFormat.REDIRECT),
                Arguments.of("METADATA", AssetReturnFormat.METADATA),
                Arguments.of("CONTENT", AssetReturnFormat.CONTENT),
            )
    }

    @ParameterizedTest
    @MethodSource("fromQueryParamSource")
    fun `fromQueryParam returns correct asset format`(
        string: String,
        expected: AssetReturnFormat,
    ) {
        AssetReturnFormat.fromQueryParam(string) shouldBe expected
    }

    @Test
    fun `fromQueryParam sets default type`() {
        AssetReturnFormat.fromQueryParam(null) shouldBe AssetReturnFormat.REDIRECT
    }

    @Test
    fun `fromQueryParam throws exception on invalid value`() {
        shouldThrow<IllegalArgumentException> {
            AssetReturnFormat.fromQueryParam("invalid")
        }
    }
}
