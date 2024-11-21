package org.totschnig.myexpenses.fragment.preferences

import android.os.Bundle
import androidx.annotation.Keep
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.io.isConnectedWifi
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.WebUiViewModel

@Keep
class PreferencesWebUiFragment : BasePreferenceFragment() {

    override val preferencesResId = R.xml.preferences_web_ui

    private val webUiViewModel: WebUiViewModel by viewModels()

    private var serverIsRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webUiViewModel.getServiceState().observe(this) { result ->
            with(requirePreference<Preference>(PrefKey.UI_WEB)) {
                result.onSuccess { serverAddress ->
                    summary = serverAddress
                    title = getString(if(serverAddress == null) R.string.start else R.string.stop)
                    serverIsRunning = serverAddress != null
                }.onFailure {
                    serverIsRunning = false
                    title = getString(R.string.start)
                    preferenceActivity.showSnackBar(it.safeMessage)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (featureManager.isFeatureInstalled(Feature.WEBUI, requireContext())) {
            bindToWebUiService()
        }
    }

    override fun onStop() {
        super.onStop()
        if (featureManager.isFeatureInstalled(Feature.WEBUI, requireContext())) {
            webUiViewModel.unbind(requireContext())
        }
    }

    override fun onPreferenceTreeClick(preference: Preference)= when {
        super.onPreferenceTreeClick(preference) -> true
        matches(preference, PrefKey.UI_WEB) -> {
            if (serverIsRunning) {
                prefHandler.putBoolean(PrefKey.UI_WEB, false)
                preferenceActivity.resultCode = BaseActivity.RESULT_INVALIDATE_OPTIONS_MENU
            } else {
                if (!isConnectedWifi(requireContext())) {
                    ConfirmationDialogFragment.newInstance(Bundle().apply {
                        putInt(
                            ConfirmationDialogFragment.KEY_TITLE,
                            R.string.title_webui
                        )
                        putString(
                            ConfirmationDialogFragment.KEY_MESSAGE,
                            TextUtils.concatResStrings(requireContext(), " ", R.string.wifi_not_connected, R.string.continue_confirmation)
                        )
                        putInt(
                            ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                            R.id.WEB_UI_COMMAND
                        )
                    }).show(parentFragmentManager, "NO_WIFI")
                } else {
                    preferenceActivity.onStartWebUi()
                }
            }
            true
        }
        else -> false
    }

    fun bindToWebUiService() {
        webUiViewModel.bind(requireContext())
    }
}