package sintulabs.p2p

import android.net.nsd.NsdServiceInfo
import java.io.File

/**
 * Created by sabzo on 1/21/18.
 */
interface ILan {
    // Runs on UI thread
    fun deviceListChanged()
    fun transferComplete(neighbor: Neighbor?, media: NearbyMedia?)
    fun transferProgress(neighbor: Neighbor?, fileMedia: File?, title: String?, mimeType: String?,
                         transferred: Long, total: Long)

    fun serviceRegistered(serviceName: String?)
    fun serviceResolved(serviceInfo: NsdServiceInfo?)
}