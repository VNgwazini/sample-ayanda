package sintulabs.p2p

/**
 * Created by sabzo on 3/22/18.
 */
interface IServer {
    val port: Int

    fun setFileToShare(media: NearbyMedia?)
}