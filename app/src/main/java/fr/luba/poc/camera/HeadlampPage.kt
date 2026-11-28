package fr.luba.poc.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import fr.luba.poc.ble.RobotBle
import fr.luba.poc.ble.RobotUi
import fr.luba.poc.i18n.tr
import fr.luba.poc.ui.ServicePage
import fr.luba.poc.ui.components.*
import fr.luba.poc.ui.theme.Dimens
import fr.luba.poc.ui.theme.MammotionColors

/**
 * Service → Headlamp: manual on/off through the robot link (Bluetooth or gateway). No Mammotion account needed, so
 * the page stays available in local mode, unlike the Camera page.
 */
@Composable
fun HeadlampPage(robot: RobotBle, state: RobotUi, back: () -> Unit, modifier: Modifier = Modifier) {
    val ready = state.ready
    // State read on opening; the switch answer (set_lamp_rsp) and the read (get_lamp_rsp) update it.
    LaunchedEffect(ready) { if (ready) robot.headlampRead() }
    PushedPage(ServicePage.HEADLAMP.title, back, modifier) {
        if (!ready) SafetyNotice(tr("Robot not connected: connect it (Bluetooth or gateway) to switch the headlamp.",
            "Robot non connecté : connectez-le (Bluetooth ou passerelle) pour commander le phare."))
        SettingsGroup(title = tr("Headlamp", "Phare")) {
            KeyValueRow(tr("State", "État"), state.camera.headlampLabel)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Sm)) {
                SecondaryPillButton(tr("Light on", "Allumer"), { robot.headlamp(true) }, Modifier.weight(1f), enabled = ready)
                SecondaryPillButton(tr("Light off", "Éteindre"), { robot.headlamp(false) }, Modifier.weight(1f), enabled = ready)
            }
            Text(tr("Models with a headlamp (Yuka Mini, LUBA 2…). Untested: check the light and the state above.",
                "Modèles avec phare (Yuka Mini, LUBA 2…). Non testé : vérifier le phare et l'état ci-dessus."),
                style = MaterialTheme.typography.bodySmall, color = MammotionColors.TextSecondary)
        }
    }
}
