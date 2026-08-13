package com.zeus.eqpro18

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

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
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Zeus EQ Pro18",
                color = Color(0xFFE8E8F0),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1A1A24))
                    .border(1.dp, Color(0xFF333344), RoundedCornerShape(20.dp))
                    .padding(horizontal = 2.dp, vertical = 1.dp)
            ) {
                IconButton(onClick = { viewModel.previousSection() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color(0xFFAAAAAA))
                }
                Text(
                    text = viewModel.sectionTitle(),
                    color = Color(0xFFFFF3B0),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(onClick = { viewModel.nextSection() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFFAAAAAA))
                }
            }

            IconButton(
                onClick = onToggleEngine,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (viewModel.isEngineRunning) Color(0xFF2ECC71) else Color(0xFF333344))
            ) {
                Icon(Icons.Default.PowerSettingsNew, null, tint = Color.White, modifier = Modifier.size(20.dp))
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
                MultibandCompressorSection(viewModel, Modifier.weight(1f).fillMaxWidth())
            }
            EqSection.LIMITER -> {
                LimiterSection(viewModel, Modifier.weight(1f).fillMaxWidth())
            }
        }

        if (viewModel.currentSection == EqSection.EQUALIZER && band != null) {
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF16161E))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("PREAMP", color = Color(0xFF888899), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                EditableParam(
                    label = "",
                    value = viewModel.preamp,
                    unit = "dB",
                    min = -30f,
                    max = 12f,
                    format = { v -> String.format("%+.1f", v) },
                    onValueChange = { viewModel.preamp = it },
                    accent = Color(0xFFFFF3B0)
                )
                Text("Headroom auto", color = Color(0xFF555566), fontSize = 10.sp)
            }

            Spacer(Modifier.height(6.dp))

            BandControls(
                band = band,
                bandCount = viewModel.bands.size,
                onFrequencyChange = { viewModel.updateSelectedBand(frequency = it) },
                onGainChange = { viewModel.updateSelectedBand(gain = it) },
                onQChange = { viewModel.updateSelectedBand(q = it) },
                onEnabledChange = { viewModel.updateSelectedBand(enabled = it) },
                onFilterTypeChange = { viewModel.updateSelectedBand(filterType = it) },
                onAddBand = { viewModel.addBand() },
                onRemoveBand = { viewModel.removeSelectedBand() }
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                viewModel.bands.forEachIndexed { idx, b ->
                    val selected = idx == viewModel.selectedBandIndex
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) b.color else Color(0xFF1A1A24))
                            .border(
                                1.dp,
                                if (selected) b.color else Color(0xFF333344),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.selectBand(idx) }
                    ) {
                        Text(
                            text = "${idx + 1}",
                            color = if (selected) Color.Black else Color(0xFFCCCCCC),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BandControls(
    band: EqBand,
    bandCount: Int,
    onFrequencyChange: (Float) -> Unit,
    onGainChange: (Float) -> Unit,
    onQChange: (Float) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onFilterTypeChange: (EqBand.FilterType) -> Unit,
    onAddBand: () -> Unit,
    onRemoveBand: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF16161E))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val types = listOf(
                EqBand.FilterType.PEAK to "PEAK",
                EqBand.FilterType.LOW_SHELF to "LO-SHELF",
                EqBand.FilterType.HIGH_SHELF to "HI-SHELF",
                EqBand.FilterType.LOW_PASS to "LP",
                EqBand.FilterType.HIGH_PASS to "HP",
                EqBand.FilterType.NOTCH to "NOTCH",
                EqBand.FilterType.BAND_PASS to "BP",
                EqBand.FilterType.BYPASS to "BYPASS"
            )
            types.forEach { (type, label) ->
                val selected = band.filterType == type
                Text(
                    text = label,
                    color = if (selected) Color.White else Color(0xFFAAAAAA),
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) band.color.copy(alpha = 0.9f) else Color(0xFF0D0D12))
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
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = band.color,
                        checkedTrackColor = band.color.copy(alpha = 0.4f)
                    )
                )
                Text("ON", color = Color(0xFFCCCCCC), fontSize = 10.sp)
            }

            EditableParam(
                label = "FREQ", value = band.frequency, unit = "Hz",
                min = 1f, max = 30000f,
                format = { v -> if (v >= 1000f) String.format("%.1fk", v / 1000f) else String.format("%.0f", v) },
                onValueChange = onFrequencyChange, accent = band.color
            )
            EditableParam(
                label = "GAIN", value = band.gain, unit = "dB",
                min = -30f, max = 30f,
                format = { v -> String.format("%+.1f", v) },
                onValueChange = onGainChange, accent = band.color
            )
            EditableParam(
                label = "Q", value = band.q, unit = "",
                min = 0.1f, max = 40f,
                format = { v -> String.format("%.2f", v) },
                onValueChange = onQChange, accent = band.color
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onRemoveBand,
                    enabled = bandCount > 1,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A2A35))
                ) {
                    Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onAddBand,
                    enabled = bandCount < EqViewModel.MAX_BANDS,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A2A35))
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun EditableParam(
    label: String,
    value: Float,
    unit: String,
    min: Float,
    max: Float,
    format: (Float) -> String,
    onValueChange: (Float) -> Unit,
    accent: Color
) {
    var editing by remember { mutableStateOf(false) }
    var text by remember(value) { mutableStateOf(format(value)) }
    val focusManager = LocalFocusManager.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (label.isNotEmpty()) {
            Text(text = label, color = Color(0xFF888899), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
        }
        if (editing) {
            BasicTextField(
                value = text,
                onValueChange = { new ->
                    // Permite números, punto y signo menos
                    if (new.isEmpty() || new == "-" || new.matches(Regex("-?\\d*\\.?\\d*"))) {
                        text = new
                    }
                },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        text.toFloatOrNull()?.let { onValueChange(it.coerceIn(min, max)) }
                        editing = false
                        focusManager.clearFocus()
                    }
                ),
                singleLine = true,
                cursorBrush = SolidColor(accent),
                modifier = Modifier
                    .width(72.dp)
                    .background(Color(0xFF0D0D12), RoundedCornerShape(6.dp))
                    .border(1.dp, accent, RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 3.dp)
            )
        } else {
            Text(
                text = format(value) + if (unit.isNotEmpty()) " $unit" else "",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        text = value.toString()
                        editing = true
                    }
                    .background(Color(0xFF0D0D12))
                    .border(1.dp, Color(0xFF333344), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
fun MultibandCompressorSection(viewModel: EqViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF12121A))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Compresor Multibanda", color = Color(0xFFFFF3B0), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Switch(
                checked = viewModel.mbcEnabled,
                onCheckedChange = { viewModel.mbcEnabled = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF74B9FF))
            )
        }

        Spacer(Modifier.height(8.dp))

        // Gráfica simple de compresión (curvas de ratio)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0D0D12))
                .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(8.dp))
        ) {
            val w = size.width
            val h = size.height
            // Grid
            for (i in 0..4) {
                val y = h * i / 4f
                drawLine(Color(0xFF2A2A35), Offset(0f, y), Offset(w, y), 1f)
            }
            // Curvas de compresión por banda (Low / Mid / High)
            fun drawCurve(ratio: Float, color: Color) {
                val path = Path()
                for (i in 0..100) {
                    val x = w * i / 100f
                    val input = -40f + 40f * i / 100f // -40 a 0 dB
                    val thresh = -18f
                    val output = if (input < thresh) input
                    else thresh + (input - thresh) / ratio
                    val y = h - ((output + 40f) / 40f) * h
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(2f, cap = StrokeCap.Round))
            }
            drawCurve(viewModel.mbcLowRatio, Color(0xFFFF6B6B))
            drawCurve(viewModel.mbcMidRatio, Color(0xFF55EFC4))
            drawCurve(viewModel.mbcHighRatio, Color(0xFF74B9FF))
        }

        Spacer(Modifier.height(10.dp))

        // Frecuencias de cruce
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            EditableParam(
                label = "LOW|MID", value = viewModel.mbcLowFreq, unit = "Hz",
                min = 20f, max = 2000f,
                format = { v -> String.format("%.0f", v) },
                onValueChange = { viewModel.mbcLowFreq = it }, accent = Color(0xFFFF6B6B)
            )
            EditableParam(
                label = "MID|HIGH", value = viewModel.mbcHighFreq, unit = "Hz",
                min = 500f, max = 12000f,
                format = { v -> if (v >= 1000f) String.format("%.1fk", v / 1000f) else String.format("%.0f", v) },
                onValueChange = { viewModel.mbcHighFreq = it }, accent = Color(0xFF74B9FF)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Controles por banda
        listOf(
            Triple("LOW", Color(0xFFFF6B6B), listOf(
                { viewModel.mbcLowThreshold } to { v: Float -> viewModel.mbcLowThreshold = v },
                { viewModel.mbcLowRatio } to { v: Float -> viewModel.mbcLowRatio = v },
                { viewModel.mbcLowAttack } to { v: Float -> viewModel.mbcLowAttack = v },
                { viewModel.mbcLowRelease } to { v: Float -> viewModel.mbcLowRelease = v }
            )),
            Triple("MID", Color(0xFF55EFC4), listOf(
                { viewModel.mbcMidThreshold } to { v: Float -> viewModel.mbcMidThreshold = v },
                { viewModel.mbcMidRatio } to { v: Float -> viewModel.mbcMidRatio = v },
                { viewModel.mbcMidAttack } to { v: Float -> viewModel.mbcMidAttack = v },
                { viewModel.mbcMidRelease } to { v: Float -> viewModel.mbcMidRelease = v }
            )),
            Triple("HIGH", Color(0xFF74B9FF), listOf(
                { viewModel.mbcHighThreshold } to { v: Float -> viewModel.mbcHighThreshold = v },
                { viewModel.mbcHighRatio } to { v: Float -> viewModel.mbcHighRatio = v },
                { viewModel.mbcHighAttack } to { v: Float -> viewModel.mbcHighAttack = v },
                { viewModel.mbcHighRelease } to { v: Float -> viewModel.mbcHighRelease = v }
            ))
        ).forEach { (name, color, params) ->
            Text(name, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                EditableParam(
                    label = "THRESH", value = params[0].first(), unit = "dB",
                    min = -40f, max = 0f, format = { v -> String.format("%.1f", v) },
                    onValueChange = params[0].second, accent = color
                )
                EditableParam(
                    label = "RATIO", value = params[1].first(), unit = ":1",
                    min = 1f, max = 50f, format = { v -> String.format("%.1f", v) },
                    onValueChange = params[1].second, accent = color
                )
                EditableParam(
                    label = "ATK", value = params[2].first(), unit = "ms",
                    min = 0.01f, max = 100f, format = { v -> String.format("%.2f", v) },
                    onValueChange = params[2].second, accent = color
                )
                EditableParam(
                    label = "REL", value = params[3].first(), unit = "ms",
                    min = 1f, max = 500f, format = { v -> String.format("%.0f", v) },
                    onValueChange = params[3].second, accent = color
                )
            }
        }
    }
}

@Composable
fun LimiterSection(viewModel: EqViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF12121A))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Limiter", color = Color(0xFFFFF3B0), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Switch(
                checked = viewModel.limiterEnabled,
                onCheckedChange = { viewModel.limiterEnabled = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFE17055))
            )
        }

        Spacer(Modifier.height(8.dp))

        // Gráfica del limitador
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0D0D12))
                .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(8.dp))
        ) {
            val w = size.width
            val h = size.height
            val thresh = viewModel.limiterThreshold
            val ratio = viewModel.limiterRatio.coerceAtLeast(1f)

            for (i in 0..4) {
                val y = h * i / 4f
                drawLine(Color(0xFF2A2A35), Offset(0f, y), Offset(w, y), 1f)
            }

            val path = Path()
            for (i in 0..100) {
                val x = w * i / 100f
                val input = -30f + 30f * i / 100f
                val output = if (input < thresh) input
                else thresh + (input - thresh) / ratio
                val y = h - ((output + 30f) / 30f) * h
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Color(0xFFE17055), style = Stroke(2.5f, cap = StrokeCap.Round))

            // Línea de threshold
            val ty = h - ((thresh + 30f) / 30f) * h
            drawLine(Color(0xFFFFF3B0).copy(alpha = 0.5f), Offset(0f, ty), Offset(w, ty), 1.5f)
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            EditableParam(
                label = "THRESH", value = viewModel.limiterThreshold, unit = "dB",
                min = -30f, max = 0f, format = { v -> String.format("%.1f", v) },
                onValueChange = { viewModel.limiterThreshold = it }, accent = Color(0xFFE17055)
            )
            EditableParam(
                label = "ATTACK", value = viewModel.limiterAttack, unit = "ms",
                min = 0.01f, max = 100f, format = { v -> String.format("%.2f", v) },
                onValueChange = { viewModel.limiterAttack = it }, accent = Color(0xFFE17055)
            )
            EditableParam(
                label = "RELEASE", value = viewModel.limiterRelease, unit = "ms",
                min = 1f, max = 500f, format = { v -> String.format("%.0f", v) },
                onValueChange = { viewModel.limiterRelease = it }, accent = Color(0xFFE17055)
            )
            EditableParam(
                label = "RATIO", value = viewModel.limiterRatio, unit = ":1",
                min = 1f, max = 50f, format = { v -> String.format("%.1f", v) },
                onValueChange = { viewModel.limiterRatio = it }, accent = Color(0xFFE17055)
            )
            EditableParam(
                label = "POST", value = viewModel.limiterPostGain, unit = "dB",
                min = -12f, max = 12f, format = { v -> String.format("%+.1f", v) },
                onValueChange = { viewModel.limiterPostGain = it }, accent = Color(0xFFE17055)
            )
        }

        Text(
            "Limiter nativo DynamicsProcessing",
            color = Color(0xFF666677),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
        )
    }
}
