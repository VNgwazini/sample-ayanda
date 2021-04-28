package sintulabs.p2p

import android.bluetooth.BluetoothDevice
import android.content.Intent

/**
 * Created by sabzo on 1/21/18.
 */
interface IBluetooth {
    // ACTION_DISCOVERY_STARTED
    fun actionDiscoveryStarted(intent: Intent?)

    // ACTION_DISCOVERY_FINISHED
    fun actionDiscoveryFinished(intent: Intent?)

    // ACTION_SCAN_MODE_CHANGED
    fun stateChanged(intent: Intent?)

    // ACTION_STATE_CHANGED
    fun scanModeChange(intent: Intent?)

    // Bluethooth.Device ACTION_FOUND
    fun actionFound(intent: Intent?)

    // Event after reading from connected device
    fun dataRead(bytes: ByteArray?, numRead: Int)

    // connected to a device
    fun connected(device: BluetoothDevice?)
}