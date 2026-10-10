package org.vik.torrente

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform