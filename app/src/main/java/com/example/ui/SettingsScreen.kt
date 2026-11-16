package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ai.AiProviderKind
import com.example.ai.CustomProviderConfig
import com.example.motion.ExportFormat
import com.example.motion.VideoResolution
import com.example.motion.VideoSettings
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NoticeAmberBg
import com.example.ui.theme.NoticeAmberBorder
import com.example.ui.theme.NoticeAmberInk
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceStudioGray

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    videoSettings: VideoSettings,
    providerConfig: CustomProviderConfig,
    onUpdateVideoSettings: (VideoSettings) -> Unit,
    onSelectProvider: (String) -> Unit,
    onSaveCustomProvider: (endpoint: String, model: String, apiKey: String?) -> Unit,
    onClearCustomKey: () -> Unit
) {
    var endpointInput by remember(providerConfig.customEndpointUrl) {
        mutableStateOf(providerConfig.customEndpointUrl)
    }
    var modelInput by remember(providerConfig.customModelName) {
        mutableStateOf(providerConfig.customModelName)
    }
    var apiKeyInput by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Video & Render Settings
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Tune, null, tint = ElectricBlue)
                            Text(
                                text = "تنظیمات پیشرفته ویدیو و رندر",
                                style = MaterialTheme.typography.titleLarge,
                                color = CharcoalBlack
                            )
                        }

                        // Resolution
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("رزولوشن خروجی", style = MaterialTheme.typography.titleMedium)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                VideoResolution.entries.forEach { res ->
                                    FilterChip(
                                        selected = videoSettings.resolution == res,
                                        onClick = { onUpdateVideoSettings(videoSettings.copy(resolution = res)) },
                                        label = { Text(res.label) }
                                    )
                                }
                            }
                        }

                        // FPS
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("نرخ فریم (FPS)", style = MaterialTheme.typography.titleMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(24, 30, 60).forEach { fps ->
                                    FilterChip(
                                        selected = videoSettings.fps == fps,
                                        onClick = { onUpdateVideoSettings(videoSettings.copy(fps = fps)) },
                                        label = { Text("$fps FPS") }
                                    )
                                }
                            }
                        }

                        // Animation Speed
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("سرعت انیمیشن", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "%.2fx".format(videoSettings.animationSpeed),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricBlue
                                )
                            }
                            Slider(
                                value = videoSettings.animationSpeed,
                                onValueChange = {
                                    onUpdateVideoSettings(videoSettings.copy(animationSpeed = it))
                                },
                                valueRange = 0.5f..2.0f,
                                steps = 5
                            )
                        }

                        // Default Export Format
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("فرمت پیش‌فرض خروجی", style = MaterialTheme.typography.titleMedium)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ExportFormat.entries.forEach { fmt ->
                                    FilterChip(
                                        selected = videoSettings.exportFormat == fmt,
                                        onClick = { onUpdateVideoSettings(videoSettings.copy(exportFormat = fmt)) },
                                        label = { Text(fmt.label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: AI Engine & Extensible Provider Management
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Key, null, tint = ElectricBlue)
                            Text(
                                text = "مدیریت موتور هوش مصنوعی و پروفایل API",
                                style = MaterialTheme.typography.titleLarge,
                                color = CharcoalBlack
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NoticeAmberBg,
                            border = BorderStroke(1.dp, NoticeAmberBorder)
                        ) {
                            Text(
                                text = "اتصال زنده به Gemini در اپ‌های اندروید AI Studio از طریق Firebase AI Logic انجام می‌شود. به دلیل فعال‌نشدن Firebase، اکنون موتور تحلیل مفهوم آفلاین (Sample Mode) فعال است.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NoticeAmberInk,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AiProviderKind.entries.forEach { kind ->
                                FilterChip(
                                    selected = providerConfig.selectedProviderId == kind.id,
                                    onClick = { onSelectProvider(kind.id) },
                                    label = { Text(kind.displayName) }
                                )
                            }
                        }

                        // Custom provider & key storage for future extensibility
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceStudioGray,
                            border = BorderStroke(1.dp, SurfaceCardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "پیکربندی ارائه‌دهنده توسعه‌پذیر (ذخیره امن محلی)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CharcoalBlack
                                )
                                OutlinedTextField(
                                    value = modelInput,
                                    onValueChange = { modelInput = it },
                                    label = { Text("نام مدل (مثلاً gemini-3.8-flash)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("settings_model_input")
                                )
                                OutlinedTextField(
                                    value = endpointInput,
                                    onValueChange = { endpointInput = it },
                                    label = { Text("آدرس Endpoint سفارشی (اختیاری)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("settings_endpoint_input")
                                )
                                OutlinedTextField(
                                    value = apiKeyInput,
                                    onValueChange = { apiKeyInput = it },
                                    label = {
                                        Text(
                                            if (providerConfig.hasStoredCustomApiKey)
                                                "کلید API ذخیره شده (برای جایگزینی وارد کنید)"
                                            else "کلید API سفارشی (مخفی‌سازی‌شده)"
                                        )
                                    },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("settings_api_key_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onSaveCustomProvider(endpointInput, modelInput, apiKeyInput)
                                            apiKeyInput = ""
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("settings_save_provider_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                                    ) {
                                        Text("ذخیره پیکربندی")
                                    }
                                    if (providerConfig.hasStoredCustomApiKey) {
                                        OutlinedButton(
                                            onClick = onClearCustomKey,
                                            modifier = Modifier.height(48.dp).testTag("settings_clear_key_button")
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("حذف کلید")
                                        }
                                    }
                                }
                            }
                        }

                        // Future Roadmap architecture note
                        Text(
                            text = "معماری آماده توسعه برای: Voice-Over به موشن، تشخیص خودکار پارت‌های فیلمنامه، زیرنویس متحرک، رندر سه‌بعدی و کتابخانه آبجکت‌ها.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateMuted
                        )
                    }
                }
            }
        }
    }
}
