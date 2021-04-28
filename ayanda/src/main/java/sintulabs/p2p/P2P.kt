package sintulabs.p2p

/**
 * WiFi Direct P2P Class for detecting and connecting to nearby devices.
 * WifiDirect, Bluetooth inherit from this class and must implement a common interface
 * Created by sabzo on 12/20/17
 */
abstract class P2P {
    protected val WIFIDIRECT: Short = 0
    protected val BLUETOOTH: Short = 1
    protected val TAG_DEBUG = "ayanda_bug"

    /* announce the p2p service */
    abstract fun announce()

    /* Discover a nearby Peer*/
    abstract fun discover()

    /* If connection method is supported */
    abstract fun isSupported(): Boolean?

    /* If connection method is not only supported, but is available*/
    abstract fun isEnabled(): Boolean?
}