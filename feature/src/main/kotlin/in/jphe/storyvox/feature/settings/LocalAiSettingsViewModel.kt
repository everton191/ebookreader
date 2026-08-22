package `in`.jphe.storyvox.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.jphe.storyvox.llm.local.LocalGemmaModelInstaller
import `in`.jphe.storyvox.llm.local.LocalModelState
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LocalAiSettingsState(val model: LocalModelState = LocalModelState.NotInstalled, val enabled: Boolean = false, val error: String? = null)

@HiltViewModel
class LocalAiSettingsViewModel @Inject constructor(private val installer: LocalGemmaModelInstaller) : ViewModel() {
    val state: StateFlow<LocalAiSettingsState> = combine(installer.state, installer.enabled) { model, enabled ->
        LocalAiSettingsState(model, enabled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalAiSettingsState())

    fun download() = viewModelScope.launch { runCatching { installer.install() } }
    fun setEnabled(enabled: Boolean) = installer.setEnabled(enabled)
    fun delete() = viewModelScope.launch { installer.delete() }
}
