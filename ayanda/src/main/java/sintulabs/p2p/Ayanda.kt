package sintulabs.p2p

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.nsd.NsdServiceInfo
import android.net.wifi.p2p.WifiP2pDevice
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.util.*

/**
 * Created by sabzo on 1/19/18.
 */
class Ayanda(private val context: Context, iBluetooth: IBluetooth?, iLan: ILan?, iWifiDirect: IWifiDirect?) {
    private var bt: Bluetooth? = null
    private var lan: Lan? = null
    private var wd: WifiDirect? = null

    /**
     * Discover nearby devices that have made themselves detectable via blue Bluetooth.
     * Discovered devices are stored in a collection of devices found.
     */
    fun btDiscover() {
        bt!!.discover()
    }

    /**
     * Connects to a discovered bluetooth device. Role: Client
     * @param device Bluetooth Device
     */
    fun btConnect(device: BluetoothDevice?) {
        if (device != null) {
            bt!!.connect(device)
        }
    }

    /**
     * Register a Bluetooth Broadcast Receiver.
     * This method must be called to detect Bluetooth events. The iBluetooth interface
     * exposes Bluetooth events.
     */
    fun btRegisterReceivers() {
        bt!!.registerReceivers()
    }

    /**
     * Unregisters Bluetooth Broadcast Receiver.
     * Must be called when Activity/App stops/closes
     */
    fun btUnregisterReceivers() {
        bt!!.unregisterReceivers()
    }

    /**
     * Announce device's presence via Bluetooth
     */
    fun btAnnounce() {
        bt!!.announce()
    }

    /**
     * Get the names of the Bluetooth devices discovered
     * @return
     */
    fun btGetDeviceNamesDiscovered(): Set<String> {
        return bt!!.getDeviceNamesDiscovered()
    }

    fun btGetDevices(): HashMap<String, BluetoothDevice> {
        return bt!!.deviceList
    }

    /**
     * Send data from this device to a connected bluetooth device
     * @param device
     * @param bytes
     */
    @Throws(IOException::class)
    fun btSendData(device: BluetoothDevice?, bytes: ByteArray?) {
        if (device != null) {
            bt!!.sendData(device, bytes)
        }
    }

    @Throws(IOException::class)
    fun lanShare(media: NearbyMedia?) {
        lan!!.shareFile(media)
    }

    fun lanAnnounce() {
        lan!!.announce()
    }

    /*
        Discover nearby devices using LAN:
        A device can register a service on the network and other devices connected on the network
        will be able to detect it.
     */
    fun lanDiscover() {
        lan!!.discover()
    }

    fun lanStopAnnouncement() {
        lan!!.stopAnnouncement()
    }

    fun lanStopDiscovery() {
        lan!!.stopDiscovery()
    }

    fun lanGetDeviceList(): List<Device>? {
        return lan?.getDeviceList()
    }
    /* Wifi Direct Methods */
    /**
     *
     * @param device to send data to
     * @param bytes array of data to send
     */
    fun wdSendData(device: WifiP2pDevice?, bytes: ByteArray?) {
        wd!!.sendData(device, bytes)
    }

    @Throws(IOException::class)
    fun wdShareFile(media: NearbyMedia?) {
        wd!!.shareFile(media)
    }

    /**
     * Connect to a WifiDirect device
     * @param device
     */
    fun wdConnect(device: WifiP2pDevice?) {
        if (device != null) {
            wd!!.connect(device)
        }
    }

    /**
     * Discover nearby WiFi Direct enabled devices
     */
    fun wdDiscover() {
        wd!!.discover()
    }

    fun wdRegisterReceivers() {
        wd!!.registerReceivers()
    }

    fun wdUnregisterReceivers() {
        wd!!.unregisterReceivers()
    }

    fun wdGetDevicesDiscovered(): ArrayList<WifiP2pDevice?> {
        return wd!!.devicesDiscovered
    }

    /**
     * Add a user defined Server class to respond to client calls
     * @param server A descendant of the server class
     */
    fun setServer(server: IServer) {
        Server.createInstance(server)
        lan?.setLocalPort(server.port)
    }

    /**
     * Add a user defined Client class. This is used to make calls to the server
     * @param client
     */
    fun setClient(client: IClient?) {
        if (client != null) {
            Client.createInstance(client)
        }
    }

    class Device {
        var host: InetAddress? = null
            private set
        var port: Int? = null
            private set
        var serviceInfo: NsdServiceInfo? = null

        constructor() {}
        constructor(serviceInfo: NsdServiceInfo) {
            port = serviceInfo.port
            host = serviceInfo.host
            this.serviceInfo = serviceInfo
        }

        val name: String
            get() = serviceInfo!!.serviceName
    }

    companion object {
        @Throws(IOException::class)
        fun findOpenSocket(): Int {
            // Initialize a server socket on the next available port.
            val serverSocket = ServerSocket(0)
            // Store the chosen port.
            val port = serverSocket.localPort
            serverSocket.close()
            return port
        }
    }

    /**
     * Ayanda is a class that discovers and interacts with nearby devices that support
     * Network Service Discovery (NSD), WiFi Direct, and  Bluetooth
     * @param context The Activity/Application Context
     * @param iBluetooth An interface to handle Bluetooth events
     * @param iLan An interface to handle LAN (NSD/Bonjour/ZeroConfig/etc.,) events
     * @param iWifiDirect An interface to handle Wifi Direct events
     */
    init {
        if (iBluetooth != null) {
            bt = Bluetooth(context, iBluetooth)
        }
        if (iLan != null) {
            lan = Lan(context, iLan)
        }
        if (iWifiDirect != null) {
            wd = WifiDirect(context, iWifiDirect)
        }
    }
}