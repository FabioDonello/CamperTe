package camperte.android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import camperte.ShownListing
import coil3.compose.AsyncImage

@Composable
fun ListingCard(shown: ShownListing, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        PhotoArea(shown)
        Column(
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(shown.listing.title, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium)
            if (shown.detailsText.isNotBlank()) {
                Text(
                    shown.detailsText,
                    fontSize = 18.sp,
                    lineHeight = 25.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (shown.bonusWords.isNotEmpty()) {
                BonusWords(shown.bonusWords)
            }
            Button(
                onClick = onOpen,
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier.padding(top = 4.dp).fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.open_listing), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(painterResource(R.drawable.ic_arrow_forward), contentDescription = null)
            }
        }
    }
}

@Composable
private fun PhotoArea(shown: ShownListing) {
    var photoFailed by remember(shown.listing.photoUrl) { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxWidth().height(240.dp).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val photoUrl = shown.listing.photoUrl
        if (photoUrl == null || photoFailed) {
            Text(
                stringResource(R.string.photo_unavailable),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 40.dp),
            )
        } else {
            AsyncImage(
                model = photoUrl,
                contentDescription = shown.listing.title,
                contentScale = ContentScale.Crop,
                onError = { photoFailed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (shown.isNew) {
            NewBadge(Modifier.align(Alignment.TopStart).padding(14.dp))
        }
        shown.priceText?.let { PriceOverlay(it, Modifier.align(Alignment.BottomStart)) }
    }
}

@Composable
private fun NewBadge(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.new_badge),
        color = MaterialTheme.colorScheme.onTertiary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.9.sp,
        modifier = modifier
            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun PriceOverlay(price: String, modifier: Modifier = Modifier) {
    Text(
        price,
        color = Color.White,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
            .padding(start = 18.dp, end = 18.dp, top = 28.dp, bottom = 12.dp),
    )
}

@Composable
private fun BonusWords(words: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        words.forEach { BonusChip(it) }
    }
}

@Composable
private fun BonusChip(word: String) {
    val color = MaterialTheme.colorScheme.onSecondaryContainer
    Row(
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 11.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(word, color = color, fontSize = 16.sp)
    }
}
