package hk.whatoeat.feature.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hk.whatoeat.R
import hk.whatoeat.feature.home.*
import java.text.DateFormat
import java.util.Date

@Composable fun FavoritesScreen(s: HomeState,vm: HomeViewModel,onOpen: (String)->Unit) {
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text(stringResource(R.string.favorites),style=MaterialTheme.typography.headlineMedium);Text(stringResource(R.string.offline_saved)) }
        if(s.favorites.isEmpty()) item { Text(stringResource(R.string.empty_favorites)) }
        items(s.favorites,key={it.key}) { f ->
            OutlinedCard(onClick={onOpen(f.key)},modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.saved_placeholder),style=MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.saved_at,DateFormat.getDateTimeInstance().format(Date(f.createdAt))))
                    if(f.key.startsWith("google:")) Text("舊版收藏：資料來源已轉換，無法再讀取此編號。",style=MaterialTheme.typography.bodySmall)
                    TextButton(onClick={vm.toggleFavorite(f.key)}) {Text(stringResource(R.string.favorite_remove))}
                }
            }
        }
    }
}
