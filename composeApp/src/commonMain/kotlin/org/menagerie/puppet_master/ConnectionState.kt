package org.menagerie.puppet_master

enum class ConnectionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    FAILED
}