package sintulabs.p2p

/**
 * Client class
 */
class Client
/**
 * Create a Client object
 */ private constructor(private val mClient: IClient) {
    companion object {
        var instance: Client? = null

        fun createInstance(iclient: IClient): Client? {
            return if (instance != null) instance else Client(iclient).also { instance = it }
        }
    }
}