package `in`.jphe.storyvox.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.jphe.storyvox.llm.local.LocalModelState
import `in`.jphe.storyvox.ui.theme.LocalSpacing

/** Local-only AI settings. It deliberately does not expose chat/API providers. */
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
    viewModel: LocalAiSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current
    SettingsSubscreenScaffold(title = "Inteligência Artificial", onBack = onBack) { padding ->
        SettingsSubscreenBody(padding) {
            SettingsGroupCard {
                Column(Modifier.fillMaxWidth().padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text("Gemma 4 E2B")
                    Text("Offline • 2,59 GB • análise e dublagem inteligente")
                    when (val model = state.model) {
                        LocalModelState.NotInstalled -> Button(onClick = viewModel::download) { Text("Baixar") }
                        is LocalModelState.Downloading -> Text("Baixando: ${(model.downloadedBytes / (1024 * 1024))} MB")
                        LocalModelState.Verifying -> Text("Verificando integridade…")
                        is LocalModelState.Failed -> Text("Falha: ${model.reason}")
                        is LocalModelState.Installed -> {
                            Text("✓ Instalado")
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                                Text("Dublagem inteligente")
                                Switch(checked = state.enabled, onCheckedChange = viewModel::setEnabled)
                            }
                            OutlinedButton(onClick = viewModel::delete) { Text("Excluir modelo") }
                        }
                    }
                    Text("A leitura não espera a IA: sem modelo ou sem análise, o narrador segue normalmente.")
                }
            }
        }
    }
}
