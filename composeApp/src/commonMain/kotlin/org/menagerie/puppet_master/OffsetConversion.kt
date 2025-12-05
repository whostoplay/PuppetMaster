package org.menagerie.puppet_master

import androidx.compose.ui.geometry.Offset

fun SerializableOffset.toOffset() = Offset(x, y)
fun Offset.toSerializableOffset() = SerializableOffset(x, y)
