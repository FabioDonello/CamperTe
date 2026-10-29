package camperte.android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import camperte.SearchArea
import camperte.SearchFilters

@Composable
fun FiltersScreen(
    state: FiltersState,
    onChange: ((SearchFilters) -> SearchFilters) -> Unit,
    onChooseArea: (SearchArea) -> Unit,
    onAllowLocation: () -> Unit,
    onDismissLocation: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    val filters = state.filters
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState())) {
            TopBar(onClose)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Section(stringResource(R.string.filters_price)) {
                    Stepper(stringResource(R.string.filters_price_from), filters.minPriceText) { up -> onChange { it.stepMinPrice(up) } }
                    Stepper(stringResource(R.string.filters_price_to), filters.maxPriceText) { up -> onChange { it.stepMaxPrice(up) } }
                }
                DistanceSection(filters.area, state.locationDenied, onChooseArea, onOpenLocationSettings)
                Section(stringResource(R.string.filters_vehicle)) {
                    Stepper(stringResource(R.string.filters_min_year), filters.minYear.toString()) { up -> onChange { it.stepMinYear(up) } }
                    Stepper(stringResource(R.string.filters_max_mileage), filters.maxMileageText) { up -> onChange { it.stepMaxMileage(up) } }
                }
                TypesSection(filters.camperTypes) { type -> onChange { it.toggleCamperType(type) } }
                Button(
                    onClick = onClose,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.filters_show), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.filters_reset), fontSize = 18.sp)
                }
            }
        }
        if (state.pendingDistanceKm != null) {
            LocationRequestSheet(onAllowLocation, onDismissLocation)
        }
    }
}

@Composable
private fun TopBar(onClose: () -> Unit) {
    Row(Modifier.padding(start = 4.dp, top = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.filters_back), modifier = Modifier.size(28.dp))
        }
        Text(stringResource(R.string.filters), fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun Stepper(label: String, value: String, onStep: (up: Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StepButton("−", stringResource(R.string.filters_decrease, label)) { onStep(false) }
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            StepButton("+", stringResource(R.string.filters_increase, label)) { onStep(true) }
        }
    }
}

@Composable
private fun StepButton(symbol: String, description: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(52.dp).semantics { contentDescription = description },
    ) {
        Text(symbol, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun DistanceSection(
    area: SearchArea,
    locationDenied: Boolean,
    onChooseArea: (SearchArea) -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    Section(stringResource(R.string.filters_distance)) {
        if (locationDenied) {
            Row(
                modifier = Modifier.background(LocalWarningColor.current.copy(alpha = 0.12f), RoundedCornerShape(14.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(painterResource(R.drawable.ic_info), contentDescription = null, tint = LocalWarningColor.current)
                Text(stringResource(R.string.filters_location_off), fontSize = 17.sp, lineHeight = 24.sp, color = LocalWarningColor.current)
            }
            OutlinedButton(
                onClick = onOpenLocationSettings,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(stringResource(R.string.filters_enable_location), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(stringResource(R.string.filters_distance_hint), fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SearchFilters.distanceOptionsKm.forEach { km ->
                AreaChip(stringResource(R.string.filters_distance_km, km), area == SearchArea.Nearby(km), !locationDenied) {
                    onChooseArea(SearchArea.Nearby(km))
                }
            }
            AreaChip(stringResource(R.string.filters_whole_italy), area == SearchArea.WholeItaly, enabled = true) {
                onChooseArea(SearchArea.WholeItaly)
            }
        }
    }
}

@Composable
private fun AreaChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label, fontSize = 18.sp, fontWeight = FontWeight.Medium) },
        shape = RoundedCornerShape(24.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.heightIn(min = 48.dp),
    )
}

@Composable
private fun TypesSection(selected: List<String>, onToggle: (String) -> Unit) {
    Section(stringResource(R.string.filters_types)) {
        Column {
            SearchFilters.camperTypeOptions.forEachIndexed { index, type ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .toggleable(value = type in selected, role = Role.Checkbox) { onToggle(type) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Checkbox(checked = type in selected, onCheckedChange = null)
                    Text(type.replaceFirstChar { it.uppercase() }, fontSize = 19.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationRequestSheet(onAllow: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(88.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_location), contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(44.dp))
            }
            Text(stringResource(R.string.location_title), fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.location_text),
                fontSize = 18.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAllow, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(R.string.location_allow), fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.location_not_now), fontSize = 18.sp)
            }
        }
    }
}
