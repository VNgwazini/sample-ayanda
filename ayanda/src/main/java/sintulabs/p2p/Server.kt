package sintulabs.p2p

import java.io.IOException

class Server private constructor(mServer: IServer) {
    private val mServer: IServer?
    val port: Int
        get() {
            var port = 0
            if (mServer != null) {
                port = mServer.port
            }
            return port
        }

    fun setFileToShare(media: NearbyMedia?) {
        mServer!!.setFileToShare(media)
    }

    companion object {
        var server: Server? = null
        fun createInstance(mServer: IServer): Server? {
            server = if (server == null) Server(mServer).also { server = it } else server
            return server
        }

        @get:Throws(IOException::class)
        val instance: Server?
            get() {
                if (server == null) {
                    throw IOException("Server not defined")
                }
                return server
            }
    }

    init {
        this.mServer = mServer
    }
}