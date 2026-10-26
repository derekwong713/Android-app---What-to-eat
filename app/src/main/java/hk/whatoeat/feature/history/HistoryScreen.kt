package hk.whatoeat.feature.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hk.whatoeat.R
import hk.whatoeat.core.model.Presentation
import hk.whatoeat.feature.home.*
import java.text.DateFormat
import java.util.Date

@Composable fun HistoryScreen(s: HomeState,vm: HomeViewModel,onOpen: (String)->Unit) {
    var confirm by remember { mutableStateOf(false) }
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text(stringResource(R.string.history),style=MaterialTheme.typography.headlineMedium);Text(stringResource(R.string.offline_saved))
            TextButton(onClick={confirm=true},enabled=s.history.isNotEmpty()){Text(stringResource(R.string.clear_history))} }
        if(s.history.isEmpty()) item {Text(stringResource(R.string.empty_history))}
        items(s.history,key={it.drawId}) { record ->
            OutlinedCard(onClick={onOpen(record.key)},modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.saved_placeholder),style=MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.record_at,DateFormat.getDateTimeInstance().format(Date(record.revealedAt)),stringResource(if(record.mode==Presentation.Direct) R.string.direct else R.string.wheel)))
                    Text(record.summary)
                    TextButton(onClick={vm.deleteRecord(record.drawId)}){Text(stringResource(R.string.delete_record))}
                }
            }
        }
    }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},text={Text(stringResource(R.string.confirm_clear_history))},
        confirmButton={TextButton(onClick={vm.clearHistory();confirm=false}){Text(stringResource(R.string.confirm))}},
        dismissButton={TextButton(onClick={confirm=false}){Text(stringResource(R.string.cancel))}})
}
