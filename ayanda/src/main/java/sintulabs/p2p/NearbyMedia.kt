package sintulabs.p2p

import java.io.File

/**
 * Created by sabzo on 2/6/18.
 */
class NearbyMedia {
    var title: String? = null
    var mMimeType: String? = null
    var metadataJson: String? = null
    var mFileMedia: File? = null
    lateinit var digest: ByteArray
    var length: Long = 0

    fun getmMimeType(): String? {
        return mMimeType
    }

    var fileMedia: File?
        get() = mFileMedia
        set(fileMedia) {
            mFileMedia = fileMedia
            digest = Utils.getDigest(mFileMedia)!!
        }

    fun setMimeType(mimeType: String?) {
        mMimeType = mimeType
    }
}