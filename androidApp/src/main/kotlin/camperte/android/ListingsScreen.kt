package camperte.android

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import camperte.SearchResult
import java.util.Date

@Composable
fun ListingsScreen(
    state: ListingsState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onOpenListing: (String) -> Unit,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
        when (val result = state.result) {
            null -> Loading()
            SearchResult.NoConnection -> NoConnection(onRetry)
            is SearchResult.Found -> Found(result, state.isRefreshing, state.filtersSummary, onRefresh, onOpenListing, onOpenFilters)
        }
    }
}

@Composable
private fun Loading() {
    CenteredState {
        CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            strokeWidth = 5.dp,
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.secondaryContainer,
        )
        Text(stringResource(R.string.loading), fontSize = 20.sp, lineHeight = 28.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun NoConnection(onRetry: () -> Unit) {
    CenteredState {
        RoundIcon(R.drawable.ic_wifi_off, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            stringResource(R.string.no_connection),
            fontSize = 22.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 32.dp),
            modifier = Modifier.padding(top = 8.dp).heightIn(min = 56.dp),
        ) {
            Icon(painterResource(R.drawable.ic_refresh), contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.retry), fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CenteredState(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Title(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp))
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp).padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

@Composable
private fun Found(
    result: SearchResult.Found,
    isRefreshing: Boolean,
    filtersSummary: String,
    onRefresh: () -> Unit,
    onOpenListing: (String) -> Unit,
    onOpenFilters: () -> Unit,
) {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(
                top = insets.calculateTopPadding() + 24.dp,
                bottom = insets.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "header") { Header(result, filtersSummary, onOpenFilters) }
            if (result.listings.isEmpty()) {
                item(key = "empty") { Empty(Modifier.fillParentMaxHeight(0.7f)) }
            }
            items(result.listings, key = { it.listing.id }) { shown ->
                ListingCard(
                    shown = shown,
                    onOpen = { onOpenListing(shown.listing.url) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
        )
    }
}

@Composable
private fun Header(result: SearchResult.Found, filtersSummary: String, onOpenFilters: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Title(Modifier.weight(1f))
            FiltersButton(onOpenFilters)
        }
        Text(
            filtersSummary,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClickLabel = stringResource(R.string.filters), onClick = onOpenFilters)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(pluralStringResource(R.plurals.listings_count, result.listings.size, result.listings.size))
            if (result.newCount > 0) {
                Pill(
                    pluralStringResource(R.plurals.new_count, result.newCount, result.newCount),
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Pill(stringResource(R.string.updated_at, timeText(result.updatedAt.toEpochMilliseconds())))
        }
        if (result.unavailableSources.isNotEmpty()) {
            Warnings(result.unavailableSources)
        }
    }
}

@Composable
private fun FiltersButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Icon(painterResource(R.drawable.ic_tune), contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.filters), fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Title(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.listings_title),
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
    )
}

@Composable
private fun Pill(
    text: String,
    background: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text,
        color = content,
        fontSize = 17.sp,
        modifier = Modifier.background(background, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun Warnings(sources: List<String>) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        sources.forEach { source ->
            Row(
                modifier = Modifier.padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_info), contentDescription = null, tint = LocalWarningColor.current)
                Text(stringResource(R.string.source_unavailable, source), color = LocalWarningColor.current, fontSize = 17.sp)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun Empty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RoundIcon(R.drawable.ic_camper, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.secondary)
        Text(stringResource(R.string.no_results), fontSize = 22.sp, lineHeight = 31.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RoundIcon(icon: Int, background: Color, tint: Color) {
    Box(Modifier.size(96.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun timeText(epochMillis: Long): String =
    DateFormat.getTimeFormat(LocalContext.current).format(Date(epochMillis))
