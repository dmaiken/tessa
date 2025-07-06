package image

import org.koin.core.module.Module
import org.koin.dsl.module

fun imageModule(): Module =
    module {
        single<ImageProcessor> {
            VipsImageProcessor()
        }
    }
