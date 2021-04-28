package sintulabs.p2p

import android.content.Intent
import java.net.InetAddress

/**
 * Created by sabzo on 1/21/18.
 */
interface IWifiDirect {
    fun wifiP2pStateChangedAction(intent: Intent?)
    fun wifiP2pPeersChangedAction()
    fun wifiP2pConnectionChangedAction(intent: Intent?)
    fun wifiP2pThisDeviceChangedAction(intent: Intent?)
    fun onConnectedAsServer(server: Server?)
    fun onConnectedAsClient(groupOwnerAddress: InetAddress?)
}