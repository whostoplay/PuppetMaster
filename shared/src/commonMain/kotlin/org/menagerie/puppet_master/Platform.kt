package org.menagerie.puppet_master

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform