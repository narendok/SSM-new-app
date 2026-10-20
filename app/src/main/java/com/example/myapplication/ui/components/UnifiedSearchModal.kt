package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.repository.ScmsRepository
import com.example.myapplication.ui.screens.UnifiedSearchScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedSearchModal(
    scmsRepository: ScmsRepository,
    onDismiss: () -> Unit,
    onRecordSelected: (Any) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            UnifiedSearchScreen(
                scmsRepository = scmsRepository,
                onRecordSelected = { record ->
                    onRecordSelected(record)
                    onDismiss()
                }
            )
        }
    }
}
