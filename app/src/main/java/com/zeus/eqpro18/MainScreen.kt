package com.zeus.eqpro18

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScreen(
    viewModel: EqViewModel,
    onToggleEngine: () -> Unit,
    modifier: Modifier = Modifier
) {
    val band = viewModel.selectedBand()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D12))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Zeus EQ Pro18",
                color = Color(0xFFE8E8F0),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1A1A24))
                    .border(1.dp, Color(0xFF333344), RoundedCornerShape(20.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                IconButton(onClick = { viewModel.previousSection() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Anterior", tint = Color(0xFFAAAAAA))
                }
                Text(
                    text = viewModel.sectionTitle(),
                    color = Color(0xFFFFF3B0),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = { viewModel.nextSection() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Siguiente", tint = Color(0xFFAAAAAA))
                }
            }

            IconButton(
                onClick = onToggleEngine,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (viewModel.isEngineRunning) Color(0xFF2ECC71) else Color(0xFF333344))
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = "On/Off", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        when (viewModel.currentSection) {
            EqSection.EQUALIZER -> {
                EqGraph(
                    bands = viewModel.bands,
                    selectedIndex = viewModel.selectedBandIndex,
                    spectrum = viewModel.spectrum,
                    onBandSelected = { viewModel.selectBand(it) },
                    onBandMoved = { idx, freq, gain ->
                        viewModel.selectBand(idx)
                        viewModel.updateSelectedBand(frequency = freq, gain = gain)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(8.dp))
                )
            }
            EqSection.CROSSOVER -> {
                CrossoverSection(viewModel, Modifier.weight(1f).fillMaxWidth())
            }
            EqSection.LIMITER -> {
                LimiterSection(viewModel, Modifier.weight(1f).fillMaxWidth())
            }
        }

        if (viewModel.currentSection == EqSection.EQUALIZER && band != null) {
            Spacer(Modifier.height(8.dp))
            BandControls(
                band = band,
                onFrequencyChange = { viewModel.updateSelectedBand(frequency = it) },
                onGainChange = { viewModel.updateSelectedBand(gain = it) },
                onQChange = { viewModel.updateSelectedBand(q = it) },
                onEnabledChange = { viewModel.updateSelectedBand(enabled = it) },
                onFilterTypeChange = { viewModel.updateSelectedBand(filterType = it) }
            )
        }
    }
}

@Composable
private fun BandControls(
    band: EqBand,
    onFrequencyChange: (Float) -> Unit,
    onGainChange: (Float) -> Unit,
    onQChange: (Float) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onFilterTypeChange: (EqBand.FilterType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF16161E))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TYPE", color = Color(0xFF888899), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(4.dp))
            val types = listOf(
                EqBand.FilterType.PEAK to "Peak",
                EqBand.FilterType.LOW_SHELF to "L.Shelf",
                EqBand.FilterType.HIGH_SHELF to "H.Shelf",
                EqBand.FilterType.LOW_PASS to "LP",
                EqBand.FilterType.HIGH_PASS to "HP",
                EqBand.FilterType.NOTCH to "Notch",
                EqBand.FilterType.BAND_PASS to "BP"
            )
            types.forEach { (type, label) ->
                val selected = band.filterType == type
                Text(
                    text = label,
                    color = if (selected) Color.White else Color(0xFFAAAAAA),
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) band.color.copy(alpha = 0.85f) else Color(0xFF0D0D12))
                        .border(1.dp, if (selected) band.color else Color(0xFF333344), RoundedCornerShape(6.dp))
                        .clickable { onFilterTypeChange(type) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = band.enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = band.color, checkedTrackColor = band.color.copy(alpha = 0.4f))
                )
                Spacer(Modifier.width(4.dp))
                Text("ON", color = Color(0xFFCCCCCC), fontSize = 11.sp)
            }
            EditableParam(label = "FREQ", value = band.frequency, unit = "Hz", min = 1f, max = 30000f,
                format = { if (it >= 1000) "%.1fk".format(it / 1000) else "%.1f".format(it) },
                onValueChange = onFrequencyChange, accent = band.color)
            EditableParam(label = "GAIN", value = band.gain, unit = "dB", min = -30f, max = 30f,
                format = { "%+.1f".format(it) }, onValueChange = onGainChange, accent = band.color)
            EditableParam(label = "Q", value = band.q, unit = "", min = 0.1f, max = 40f,
                format = { "%.2f".format(it) }, onValueChange = onQChange, accent = band.color)
        }
    }
}

@Composable
fun EditableParam(
    label: String, value: Float, unit: String, min: Float, max: Float,
    format: (Float) -> String, onValueChange: (Float) -> Unit, accent: Color
) {
    var editing by remember { mutableStateOf(false) }
    var text by remember(value) { mutableStateOf(format(value)) }
    val focusManager = LocalFocusManager.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color(0xFF888899), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        if (editing) {
            BasicTextField(
                value = text, onValueChange = { text = it },
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    text.toFloatOrNull()?.let { onValueChange(it.coerceIn(min, max)) }
                    editing = false
                    focusManager.clearFocus()
                }),
                singleLine = true, cursorBrush = SolidColor(accent),
                modifier = Modifier.width(80.dp).background(Color(0xFF0D0D12), RoundedCornerShape(6.dp))
                    .border(1.dp, accent, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 4.dp)
            )
        } else {
            Text(
                text = "\( {format(value)} \){if (unit.isNotEmpty()) " $unit" else ""}",
                color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable {
                    text = value.toString()
                    editing = true
                }.background(Color(0xFF0D0D12)).border(1.dp, Color(0xFF333344), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun CrossoverSection(viewModel: EqViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF12121A)).padding(16.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Crossover Multiband", color = Color(0xFFFFF3B0), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Text("Frecuencias de corte configurables\n(Low / Mid / High / Air)", color = Color(0xFFAAAAAA), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        viewModel.crossoverFrequencies.forEachIndexed { idx, freq ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                Text("Band ${idx + 1} →", color = Color(0xFFCCCCCC), modifier = Modifier.width(80.dp))
                EditableParam(label = "FREQ", value = freq, unit = "Hz", min = 20f, max = 20000f,
                    format = { if (it >= 1000) "%.1fk".
