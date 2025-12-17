package org.menagerie.puppet_master

import androidx.compose.runtime.Composable

@Composable
expect fun ImagePickerDialog(
  show: Boolean,
  title: String,
  multiSelect: Boolean,
  initialDirectory: String?,
  onCancel: () -> Unit,
  onResult: (List<Pair<ByteArray, String>>) -> Unit,
  onFolderSelected: (String) -> Unit
)
