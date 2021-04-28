package sintulabs.p2p

class Neighbor(var mId: String, var mName: String, var mType: Int) {
    companion object {
        const val TYPE_BLUETOOTH = 1
        const val TYPE_WIFI_NSD = 2
        const val TYPE_WIFI_P2P = 3
    }
}