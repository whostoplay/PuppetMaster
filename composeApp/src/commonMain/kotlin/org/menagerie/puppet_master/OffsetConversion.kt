package org.menagerie.puppet_master

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

fun SerializableOffset.toOffset() = Offset(x, y)
fun Offset.toSerializableOffset() = SerializableOffset(x, y)

fun SerializableSize.toSize() = Size(width, height)
fun Size.toSerializableSize() = SerializableSize(width, height)
