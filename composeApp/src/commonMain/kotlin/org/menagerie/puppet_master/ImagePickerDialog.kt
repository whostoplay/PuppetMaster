package org.menagerie.puppet_master

import androidx.compose.runtime.Composable

@Composable
expect fun ImagePickerDialog(
  show: Boolean,
  title: String,
  multiSelect: Boolean,
  onCancel: () -> Unit,
  onResult: (List<Pair<ByteArray, String>>) -> Unit
)
