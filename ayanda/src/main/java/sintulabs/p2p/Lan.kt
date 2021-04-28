package sintulabs.p2p

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.*
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.MimeTypeMap
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Created by sabzo on 12/26/17.
 */
class Lan(private val mContext: Context, private val iLan: ILan) : P2P() {
    // For discovery
    private var mDiscoveryListener: DiscoveryListener? = null

    // For announcing service
    private var localPort = 0
    private var mServiceName: String? = null
    private var mRegistrationListener: RegistrationListener? = null

    // for connecting
    private var clientID = "" // This device's WiFi ID
    private var mNsdManager: NsdManager
    private var serviceAnnounced: Boolean
    private var isDiscovering = false
    private val deviceList: MutableList<Ayanda.Device>
    private val servicesDiscovered: MutableSet<String>
    fun setLocalPort(port: Int) {
        localPort = port
    }

    override fun isSupported(): Boolean? {
        return null
    }

    override fun isEnabled(): Boolean? {
        return null
    }

    override fun announce() {
        val msg: String
        // Create the NsdServiceInfo object, and populate it.
        val serviceInfo = NsdServiceInfo()
        if (Server.server == null) {
            msg = "No Server implementation found"
            Log.d(TAG_DEBUG, msg)
        } else {
            // The name is   subject to change based on conflicts
            // with other services advertised on the same network.
            serviceInfo.serviceName = SERVICE_NAME_DEFAULT
            serviceInfo.serviceType = SERVICE_TYPE
            serviceInfo.port = localPort
            mNsdManager = mContext.getSystemService(Context.NSD_SERVICE) as NsdManager
            if (mRegistrationListener == null) initializeRegistrationListener()
            msg = if (!serviceAnnounced) {
                mNsdManager.registerService(
                        serviceInfo, PROTOCOL_DNS_SD, mRegistrationListener)
                "Announcing on LAN: " + SERVICE_NAME_DEFAULT + " : " + SERVICE_TYPE + "on port: " + localPort.toString()
            } else {
                "Service already announced"
            }
        }
        Log.d(TAG_DEBUG, msg)
    }

    private fun initializeRegistrationListener() {
        mRegistrationListener = object : RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                mServiceName = NsdServiceInfo.serviceName
                serviceAnnounced = true
                Log.d(TAG_DEBUG, "successfully registered service $mServiceName")
                iLan.serviceRegistered(mServiceName)
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // Registration failed!  Put debugging code here to determine why.
                Log.e(TAG_DEBUG, "Error registering service " + Integer.toString(errorCode))
                mRegistrationListener = null // Allow service to be reinitialized
                serviceAnnounced = false // Allow service to be re-announced
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
                serviceAnnounced = false // Allow service to be re-announced
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // Unregistration failed.  Put debugging code here to determine why.
            }
        }
    }

    /* Discover service */
    override fun discover() {
        /* TWO Steps:
            1. Create DiscoverListener
            2. Start Discovery and pass in DiscoverListener
         */

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = object : DiscoveryListener {
            //  Called as soon as service discovery begins.
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG_DEBUG, "LAN Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                // A service was found!  Do something with it.
                Log.d(TAG_DEBUG, "Service discovery success$service")
                val hash = service.serviceName
                if (servicesDiscovered.contains(hash)) {
                    Log.d(TAG_DEBUG, "Service already discovered")
                    updateDeviceList()
                    // Service already discovered -- ignore it!
                } else if (service.serviceType == SERVICE_TYPE &&
                        service.serviceName.contains(SERVICE_NAME_DEFAULT)) {
                    mNsdManager.resolveService(service, object : ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            // Called when the resolve fails.  Use the error code to debug.
                            Log.e(TAG_DEBUG, "Resolve failed$errorCode")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            Log.e(TAG_DEBUG, "Resolve Succeeded. $serviceInfo")
                            val d = Ayanda.Device(serviceInfo)
                            addDeviceToList(d)
                            updateDeviceList()
                            iLan.serviceResolved(serviceInfo)
                            Log.d(TAG_DEBUG, "Discovered Service: $serviceInfo")
                            /* FYI; ServiceType within listener doesn't have a period at the end.
                         outside the listener it does */servicesDiscovered.add(serviceInfo.serviceName + serviceInfo.serviceType)
                        }
                    })
                }
                servicesDiscovered.add(hash)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                // When the network service is no longer available.
                // remove service from list
                Log.e(TAG_DEBUG, "service lost$service")
                removeDeviceFromList(Ayanda.Device(service))
                servicesDiscovered.remove(service.serviceName)
                updateDeviceList()
            }

            override fun onDiscoveryStopped(serviceType: String) {
                isDiscovering = false
                Log.i(TAG_DEBUG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                isDiscovering = false
                Log.e(TAG_DEBUG, "Discovery failed: Error code:$errorCode")
                mNsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                isDiscovering = false
                Log.e(TAG_DEBUG, "Discovery failed: Error code:$errorCode")
                mNsdManager.stopServiceDiscovery(this)
            }
        }

        //start discovery
        startDiscovery()
    }

    /*  Update UI thread that device list has been changed */
    private fun updateDeviceList() {
        // Runnable for main thread
        Handler(Looper.getMainLooper()).post { iLan.deviceListChanged() }
    }

    private fun addDeviceToList(device: Ayanda.Device) {
        deviceList.add(device)
    }

    private fun removeDeviceFromList(device: Ayanda.Device) {
        var pos = -1 // pos of device to remove
        val deviceName: String = device.name
        var match: String?
        for (i in deviceList.indices) {
            match = deviceList[i].name
            if (deviceName.contains(match)) {
                pos = i
                break
            }
        }
        if (pos != -1) {
            deviceList.removeAt(pos)
        }
        updateDeviceList()
    }

    /**
     * Helper method to start discovery
     */
    private fun startDiscovery() {
        mNsdManager.discoverServices(
                SERVICE_TYPE, PROTOCOL_DNS_SD, mDiscoveryListener)
        isDiscovering = true
    }

    fun stopDiscovery() {
        if (mDiscoveryListener != null) {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener)
            mDiscoveryListener = null
            servicesDiscovered.clear()
            isDiscovering = false
        }
    }

    fun stopAnnouncement() {
        if (mRegistrationListener != null) {
            mNsdManager.unregisterService(mRegistrationListener)
            mRegistrationListener = null
        }
    }

    /* Create a String representing the host and port of a device on LAN */
    private fun buildURLFromDevice(device: Ayanda.Device): StringBuilder {
        val sbUrl = StringBuilder()
        sbUrl.append("http://")
        sbUrl.append(device.host?.getHostName())
        sbUrl.append(":").append(device.port)
        return sbUrl
    }

    /* Create a Request Object */
    private fun buildRequest(url: StringBuilder): Request {
        return Request.Builder().url(url.toString())
                .addHeader("NearbyClientId", clientID).build()
    }

    private fun createFile(mTitle: String): File {
        val dirDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(dirDownloads, Date().time.toString() + "." + mTitle)
    }

    private fun setFileExtension(media: NearbyMedia) {
        var fileExt = MimeTypeMap.getSingleton().getExtensionFromMimeType(media.mMimeType)
        if (fileExt == null) {
            if (media.mMimeType?.startsWith("image") == true) fileExt = "jpg"
            else if (media.mMimeType?.startsWith("video") == true) fileExt = "mp4"
            else if (media.mMimeType?.startsWith("audio") == true) fileExt = "m4a"
        }
        media.title += ".$fileExt"
    }

    /* Use WiFi Address as a unique device id */
    private fun getWifiAddress(context: Context): String {
        val applicationContext = context.applicationContext
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress = wifiManager.connectionInfo.ipAddress
        // handle IPv6!
        return String.format(Locale.ENGLISH,
                "%d.%d.%d.%d", ipAddress and 0xff, ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff, ipAddress shr 24 and 0xff)
    }

    fun getDeviceList(): List<Ayanda.Device> {
        return deviceList
    }

    /* Share file with nearby devices */
    @Throws(IOException::class)
    fun shareFile(media: NearbyMedia?) {
        //this.fileToShare = media;
        Server.instance?.setFileToShare(media)
        announce()
    }

    companion object {
        // constants for identifying service and service type
        const val SERVICE_NAME_DEFAULT = "NSDaya"
        const val SERVICE_TYPE = "_http._tcp."
        const val SERVICE_DOWNLOAD_FILE_PATH = "/nearby/file"
        const val SERVICE_DOWNLOAD_METADATA_PATH = "/nearby/meta"
    }

    init {
        mNsdManager = mContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        deviceList = ArrayList()
        serviceAnnounced = false
        servicesDiscovered = HashSet()
        clientID = getWifiAddress(mContext)
    }
}