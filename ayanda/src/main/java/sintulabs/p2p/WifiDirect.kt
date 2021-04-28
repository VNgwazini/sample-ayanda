package sintulabs.p2p

import android.app.AlertDialog
import android.content.*
import android.location.LocationManager
import android.net.NetworkInfo
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.*
import android.os.Parcelable
import android.provider.Settings
import android.util.Log
import java.io.IOException
import java.net.InetAddress
import java.util.*

/**
 * WiFi Direct P2P Class for detecting and connecting to nearby devices
 */
class WifiDirect(private val context: Context, private val iWifiDirect: IWifiDirect) : P2P() {
    private var receiver: BroadcastReceiver? = null
    private var intentFilter: IntentFilter? = null
    private var wiFiP2pEnabled = false
    private var isGroupOwner = false
    private var groupOwnerAddress: InetAddress? = null

    /**
     * Return devices discovered. Method should be called when WIFI_P2P_PEERS_CHANGED_ACTION
     * is complete
     * @return Arraylist <WifiP2pDevice>
    </WifiP2pDevice> */
    val devicesDiscovered: ArrayList<WifiP2pDevice?> = ArrayList()
    private var fileToShare: NearbyMedia? = null
    private var serverPort = 8080

    /**
     * Determines if device is connected and acting as a client
     * @return
     */
    var isClient = false
        private set

    /**
     * Determines if device is connected and acting as a server (GroupOwner)
     * @return
     */
    var isServer = false
        private set

    fun setServerport(port: Int) {
        serverPort = port
    }

    /**
     * Create intents for default WiFi direct actions
     */
    private fun createIntent() {
        intentFilter = IntentFilter()
        intentFilter!!.addAction(WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter!!.addAction(WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter!!.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter!!.addAction(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    /**
     * Create WifiP2pManager and Channel
     */
    private fun initializeWifiDirect() {
        wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        wifiDirectChannel = wifiP2pManager!!.initialize(context, context.mainLooper) { // On Disconnect reconnect again
            initializeWifiDirect()
        }
    }

    /**
     * receiver for WiFi direct hardware events
     */
    private fun createReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                when (action) {
                    WIFI_P2P_STATE_CHANGED_ACTION -> wifiP2pStateChangedAction(intent)
                    WIFI_P2P_PEERS_CHANGED_ACTION -> wifiP2pPeersChangedAction()
                    WIFI_P2P_CONNECTION_CHANGED_ACTION -> wifiP2pConnectionChangedAction(intent)
                    WIFI_P2P_THIS_DEVICE_CHANGED_ACTION ->                         // Respond to this device's wifi state changing
                        wifiP2pThisDeviceChangedAction(intent)
                }
            }
        }
    }

    /**
     * When Wifi Direct is enabled/disabled. Propagates event to WiFi Direct interface
     * @param intent
     */
    fun wifiP2pStateChangedAction(intent: Intent) {
        val state = intent.getIntExtra(EXTRA_WIFI_STATE, -1)
        when (state) {
            WIFI_P2P_STATE_DISABLED -> wiFiP2pEnabled = false
            WIFI_P2P_STATE_ENABLED -> wiFiP2pEnabled = true
        }
        iWifiDirect.wifiP2pStateChangedAction(intent)
    }

    /**
     * When new peers are discovered
     */
    fun wifiP2pPeersChangedAction() {
        if (wifiP2pManager != null) {
            wifiP2pManager!!.requestPeers(wifiDirectChannel) { peerList ->
                devicesDiscovered.clear()
                devicesDiscovered.addAll(peerList.deviceList)
            }
        }
        iWifiDirect.wifiP2pPeersChangedAction()
    }

    /**
     * When connection is made/lost
     * @param intent
     */
    fun wifiP2pConnectionChangedAction(intent: Intent) {
        // Respond to new connection or disconnections
        if (wifiP2pManager == null) {
            return
        }
        val networkInfo = intent
                .getParcelableExtra<Parcelable>(EXTRA_NETWORK_INFO) as NetworkInfo
        if (networkInfo.isConnected) {
            // We are connected with the other device, request connection
            // info to find group owner IP
            // TODO Find group owner port
            wifiP2pManager!!.requestConnectionInfo(wifiDirectChannel) { wifiP2pInfo ->
                if (wifiP2pInfo.groupFormed) {
                    isGroupOwner = wifiP2pInfo.isGroupOwner
                    groupOwnerAddress = wifiP2pInfo.groupOwnerAddress
                    if (isGroupOwner) {
                        isServer = true
                        onConnectedAsServer()
                    } else {
                        isClient = true
                        onConnectedAsClient()
                    }
                }
            }
        }
        iWifiDirect.wifiP2pConnectionChangedAction(intent)
    }

    /**
     * This device connected as a group owner (server).
     */
    private fun onConnectedAsServer() {
        iWifiDirect.onConnectedAsServer(Server.server)
    }

    /**
     * This device connected as a client
     */
    private fun onConnectedAsClient() {
        iWifiDirect.onConnectedAsClient(groupOwnerAddress)
    }

    fun wifiP2pThisDeviceChangedAction(intent: Intent?) {
        iWifiDirect.wifiP2pThisDeviceChangedAction(intent)
    }

    fun registerReceivers() {
        context.registerReceiver(receiver, intentFilter)
    }

    fun unregisterReceivers() {
        context.unregisterReceiver(receiver)
    }

    /**
     * look for nearby peers
     */
    private fun discoverPeers() {
        if (!isLocationOn) {
            enableLocation(context)
        }
        wifiP2pManager!!.discoverPeers(wifiDirectChannel, object : ActionListener {
            override fun onSuccess() {}
            override fun onFailure(reasonCode: Int) {
                Log.d("Debug", "failed to look for pears: $reasonCode")
            }
        })
    }

    /**
     * Connect to a nearby device
     * @param device
     */
    fun connect(device: WifiP2pDevice) {
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress
        config.wps.setup = WpsInfo.PBC
        wifiP2pManager!!.connect(wifiDirectChannel, config, object : ActionListener {
            override fun onSuccess() {
                // WiFiDirectBroadcastReceiver notifies us. Ignore for now.
            }

            override fun onFailure(reason: Int) {
                // todo if failure == 2
            }
        })
    }

    /**f
     * Should be called when a connection has already been made to WifiP2pDevice
     * @param device
     * @param bytes
     */
    fun sendData(device: WifiP2pDevice?, bytes: ByteArray?) {}

    /**
     * Set the file to share
     * @param fileToShare
     */
    fun setFileToShare(fileToShare: NearbyMedia?) {
        this.fileToShare = fileToShare
    }

    fun shareFile(file: NearbyMedia?) {
        try {
            Server.instance?.setFileToShare(file)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        discover()
    }

    /**
     * Android 8.0+ requires location to be turned on when discovering
     * nearby devices.
     * @return boolean
     */
    val isLocationOn: Boolean
        get() {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }

    /**
     * Enable location
     * @param context
     */
    private fun enableLocation(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id -> context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                .setNegativeButton("No") { dialog, id -> dialog.cancel() }
        val alert = builder.create()
        alert.show()
    }

    override fun announce() {}
    override fun discover() {
        discoverPeers()
    }

    /**
     * is Wifi Direct supported
     * @return
     */
    override fun isSupported(): Boolean? {
        return null
    }

    /**
     * is Wifi Direct enabled
     * @return
     */
    override fun isEnabled(): Boolean? {
        return null
    }

    companion object {
        private var wifiP2pManager: WifiP2pManager? = null
        private var wifiDirectChannel: Channel? = null
    }

    /**
     * Creates a WifiDirect instance
     * @param context activity/application contex
     * @param iWifiDirect an inteface to provide callbacks to WiFi Direct events
     */
    init {
        initializeWifiDirect()
        // IntentFilter for receiver
        createIntent()
        createReceiver()
    }
}