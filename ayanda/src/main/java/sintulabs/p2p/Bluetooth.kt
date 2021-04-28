package sintulabs.p2p

import android.app.Activity
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import sintulabs.p2p.Bluetooth.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


/**
 * Created by sabzo on 1/14/18.
 */
class Bluetooth(private val context: Context, /* Bluetooth Events Interface */
                private val iBluetooth: IBluetooth): P2P() {
    var mBluetoothAdapter: BluetoothAdapter?
    private var receiver: BroadcastReceiver? = null
    private var intentFilter: IntentFilter? = null
    private var discoveryInitiated = false
    private val deviceNamesDiscovered: MutableSet<String>
    val deviceList: HashMap<String?, BluetoothDevice>
    private var pairedDevices: Set<BluetoothDevice>? = null
    private val dataTransferThreads: HashMap<String, DataTransferThread?>
    var REQUEST_ENABLE_BT = 1
    var BT_PERMISSION_REQUEST_LOCATION = 4444
    var UUID = "00001101-0000-1000-8000-00805F9B34AC" // arbitrary
    var NAME = "AyandaSecure"
    var NAME_INSECURE = "AyandaInsecure"

    // Is there an active connection
    var isConnected = false
    override fun isSupported(): Boolean? {
        return if (mBluetoothAdapter == null) false else true
    }

    override fun isEnabled(): Boolean? {
        return mBluetoothAdapter?.isEnabled
    }

    private fun enable() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        (context as Activity).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    /**
     * Announce Bluetooth service to Nearby Devices
     */
    override fun announce() {
        if (isSupported() == true) {
            enable()
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            context.startActivity(discoverableIntent)
            ServerThread(true).start()
        }
    }

    /**
     * Create Intent Filters for Bluetooth events
     */
    private fun createIntentFilter() {
        intentFilter = IntentFilter()
        intentFilter!!.addAction(BluetoothDevice.ACTION_FOUND)
        intentFilter!!.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        intentFilter!!.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        intentFilter!!.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter!!.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
    }

    /**
     * Broadcast receiver to handle Bluetooth events
     */
    private fun createReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                when (action) {
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> actionDiscoveryStarted(intent)
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> actionDiscoveryFinished(intent)
                    BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> scanModeChange(intent)
                    BluetoothAdapter.ACTION_STATE_CHANGED -> stateChanged(intent)
                    BluetoothDevice.ACTION_FOUND -> deviceFound(intent)
                }
            }

            private fun scanModeChange(intent: Intent) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,
                        BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> {
                        Toast.makeText(context, "Device is connectable and discoverable", Toast.LENGTH_SHORT).show()
                        Log.d(TAG_DEBUG, "Device is connectable and discoverable")
                    }
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE -> {
                    }
                }
            }

            // Discovery is quick and limited (about 12 seconds)
            private fun actionDiscoveryStarted(intent: Intent) {
                Log.d(TAG_DEBUG, "Discovery started")
                iBluetooth.actionDiscoveryStarted(intent)
            }

            // Calls after BT finishes scanning (12 seconds)
            private fun actionDiscoveryFinished(intent: Intent) {
                discoveryInitiated = false
                Log.d(TAG_DEBUG, "Discovery finished")
                iBluetooth.actionDiscoveryFinished(intent)
            }

            /* Bluetooth enabled/disabled */
            private fun stateChanged(intent: Intent) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        if (discoveryInitiated && !mBluetoothAdapter!!.startDiscovery()) {
                            Log.d(TAG_DEBUG, "unable to start bluetooth discovery")
                        }
                    }
                }
                iBluetooth.stateChanged(intent)
            }
        }
    }

    /**
     * Get nearby devices already paired with using Bluetooth.
     * Notifies iBluetooth interface when a device is found
     */
    private fun findPairedDevices() {
        pairedDevices = mBluetoothAdapter!!.bondedDevices
        if ((pairedDevices as MutableSet<BluetoothDevice>?)?.isNotEmpty() == true) {
            // There are paired devices. Get the name and address of each paired device.
            for (device in (pairedDevices as MutableSet<BluetoothDevice>?)!!) {
                if (device.name != null &&
                        (device.bluetoothClass.deviceClass == BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA || device.bluetoothClass.deviceClass == BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA || device.bluetoothClass.deviceClass == BluetoothClass.Device.PHONE_SMART)) {
                    val intent = Intent()
                    intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
                    deviceFound(intent)
                }
            }
        }
    }

    /**
     * Calls a helper method to retrieve paired devices.
     * @return returns list of paired devices
     */
    fun getPairedDevices(): Set<BluetoothDevice>? {
        findPairedDevices()
        return pairedDevices
    }

    /**
     * Event handler for when device is found. It performs some book-keeping and propagates event
     * to the IBluetooth interface.
     * @param intent
     */
    private fun deviceFound(intent: Intent) {
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        val d = Device(device)
        val deviceName = if (d.getDeviceName() == null) d.getDeviceAddress() else d.getDeviceName()!!
        deviceList[deviceName] = device
        if (deviceName != null) {
            deviceNamesDiscovered.add(deviceName)
        }
        iBluetooth.actionFound(intent)
    }

    /* Register/unregister Receiver */
    fun registerReceivers() {
        context.registerReceiver(receiver, intentFilter)
    }

    fun unregisterReceivers() {
        context.unregisterReceiver(receiver)
    }

    override fun discover() {
        if (isSupported() == true) {
            enable()
            discoveryInitiated = true
            if (!isEnabled()!!) {
                enable()
            } else {
                getPairedDevices()
                if (!mBluetoothAdapter!!.startDiscovery()) {
                    Log.d(TAG_DEBUG, "unable to start bluetooth discovery")
                }
            }
        }
    }

    fun getDeviceNamesDiscovered(): Set<String> {
        return deviceNamesDiscovered
    }

    /**
     * Send data to bluetooth device.
     * There must be an existing connection in place
     * @param device Nearyby bluetooth device
     * @param bytes Array of bytes to send
     */
    @Throws(IOException::class)
    fun sendData(device: BluetoothDevice, bytes: ByteArray?) {
        val dataTransferThread: DataTransferThread?
        // There's no connection to device yet, so return
        if (!dataTransferThreads.containsKey(device.address)) {
            return
        }
        // Use existing connection to write data
        dataTransferThread = dataTransferThreads[device.address]
        dataTransferThread!!.write(bytes)
    }

    /**
     * Represents a Bluetooth Device
     */
    class Device(device: BluetoothDevice){
        private var device : BluetoothDevice? = null;
        private var deviceName : String? = null;
        private var deviceAddress : String? = null;
        init{
            this.device = device;
            deviceName = device.name;
            deviceAddress = device.address;
        }
        fun getDeviceName(): String? {
            return deviceName;
        }
        fun getDeviceAddress(): String? {
            return deviceAddress;
        }
        fun getDevice(): BluetoothDevice? {
            return device;
        }
    }


    /**
     * Connects, as a client,  to a Bluetooth Device
     * @param device Bluetooth devices discovered or already paired
     */
    fun connect(device: BluetoothDevice) {
        if (connectionExists(device)) {
            onDeviceConnected(device)
        } else {
            ClientThread(device).start()
        }
    }

    /**
     * Determines if a connection to a device exists
     * @param device
     * @return Boolean: if connection to device exists
     */
    private fun connectionExists(device: BluetoothDevice): Boolean {
        return dataTransferThreads.containsKey(device.address)
    }

    /**
     * Removes connection from collection of connections.
     * Occurs when a connection to a bluetooth device is lost.
     * @param device Bluetooth device
     */
    private fun connectionLost(device: BluetoothDevice) {
        dataTransferThreads.remove(device.address)
    }

    /**
     * Occurs when Bluetooth device is connected
     * @param device
     */
    private fun onDeviceConnected(device: BluetoothDevice) {
        Handler(Looper.getMainLooper()).post { iBluetooth.connected(device) }
    }

    /**
     * Creates Sever to receive connections from other bluetooth devices
     */
    private inner class ServerThread(secure: Boolean) : Thread() {
        // Server
        private val btServerSocket: BluetoothServerSocket?
        private var btSocket: BluetoothSocket? = null
        private val socketType: String
        override fun run() {
            try {
                btSocket = btServerSocket!!.accept()
                //bluetooth server accepts 1 connection at a time so close after new connection
                btServerSocket.close()
                // begin writing data
                if (btSocket != null) {
                    val dt = DataTransferThread(btSocket!!)
                    val device = btSocket!!.remoteDevice
                    dataTransferThreads[device.address] = dt
                    dt.start()
                }
                // client has connected
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // close open thread
        fun close() {
            try {
                btServerSocket!!.close()
                btSocket!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        init {
            socketType = if (secure) "Secure" else "Insecure"
            var tmp: BluetoothServerSocket? = null
            // Create a new listening server socket
            try {
                tmp = if (secure) {
                    mBluetoothAdapter!!.listenUsingRfcommWithServiceRecord(NAME,
                            java.util.UUID.fromString(UUID))
                } else {
                    mBluetoothAdapter!!.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE,
                            java.util.UUID.fromString(UUID))
                }
            } catch (e: IOException) {
                Log.e(TAG_DEBUG, "Socket Type: " + socketType + "listen() failed", e)
            }
            btServerSocket = tmp
        }
    }

    /**
     * Connect as a Client to a nearby Bluetooth Device acting as a server
     */
    private inner class ClientThread(private val device: BluetoothDevice) : Thread() {
        private var socket: BluetoothSocket? = null
        private val dataTansfer: DataTransferThread? = null
        override fun run() {
            // Discovery while trying to connect slows connection down
            mBluetoothAdapter!!.cancelDiscovery()
            if (socket != null) {
                try {
                    socket!!.connect() // blocking
                    val address = device.address
                    val dt = DataTransferThread(socket!!)
                    // Check to see is there an existing connection to device
                    if (!connectionExists(device)) {
                        dataTransferThreads[device.address] = dt
                    }
                    dt.start()
                    // Notify main thread about the successful connection
                    onDeviceConnected(device)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        /**
         *
         * @param socket Open Socket to a device acting as a server
         */
        fun close(socket: BluetoothSocket) {
            try {
                socket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        init {
            try {
                socket = device.createRfcommSocketToServiceRecord(
                        java.util.UUID.fromString(UUID))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Represents an ative connection between this device and another device.
     */
    private inner class DataTransferThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream?
        private val outputStream: OutputStream?
        private val buffer: ByteArray

        /**
         * write bytes to connected device
         * @param bytes
         * @throws IOException if error occurs, such as connection being lost
         */
        @Throws(IOException::class)
        fun write(bytes: ByteArray?) {
            try {
                outputStream!!.write(bytes)
            } catch (e: IOException) {
                connectionLost(socket.remoteDevice)
                socket.close()
                e.printStackTrace()
                throw IOException(e)
            }
        }

        override fun run() {
            while (connectionExists(socket.remoteDevice)) {
                try {
                    read(buffer)
                } catch (e: IOException) {
                    e.printStackTrace()
                    connectionLost(socket.remoteDevice)
                    try {
                        socket.close()
                    } catch (e1: IOException) {
                        e1.printStackTrace()
                    }
                    break
                }
            }
        }

        /**
         * Read data from connected device
         * @param buffer A buffer to store data read
         * @return number of bytes read as an int
         * @throws IOException
         */
        @Throws(IOException::class)
        fun read(buffer: ByteArray?) {
            val numRead = inputStream!!.read(buffer)
            // Runnable for main thread
            Handler(Looper.getMainLooper()).post { iBluetooth.dataRead(buffer, numRead) }
        }

        /**
         * Creates InputStream & Output Stream from Bluetooth Scoket
         * @param socket Bluetooth socket representing active connection
         */
        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            buffer = ByteArray(1024)
            try {
                tmpIn = socket.inputStream
            } catch (e: IOException) {
                Log.e(TAG_DEBUG, "Error occurred when creating input stream", e)
            }
            try {
                tmpOut = socket.outputStream
            } catch (e: IOException) {
                Log.e(TAG_DEBUG, "Error occurred when creating output stream", e)
            }
            inputStream = tmpIn
            outputStream = tmpOut
            isConnected = true
        }
    }

    companion object {
        var REQUEST_ENABLE_BT = 1
        var BT_PERMISSION_REQUEST_LOCATION = 4444
        var UUID = "00001101-0000-1000-8000-00805F9B34AC" // arbitrary
        var NAME = "AyandaSecure"
        var NAME_INSECURE = "AyandaInsecure"
    }

    init {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        deviceNamesDiscovered = HashSet()
        deviceList = HashMap()
        dataTransferThreads = HashMap()
        createIntentFilter()
        createReceiver()
        // ensure to register and unregister receivers
    }
}