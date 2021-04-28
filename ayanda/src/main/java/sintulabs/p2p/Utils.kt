package sintulabs.p2p

import android.util.Log
import java.io.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.experimental.and

object Utils {
    private const val TAG = "android-btxfr/Utils"
    private const val DIGEST_ALGO = "SHA1"
    fun intToByteArray(a: Int): ByteArray {
        val ret = ByteArray(4)
        ret[3] = (a and 0xFF).toByte()
        ret[2] = (a shr 8 and 0xFF).toByte()
        ret[1] = (a shr 16 and 0xFF).toByte()
        ret[0] = (a shr 24 and 0xFF).toByte()
        return ret
    }

    fun digestMatch(imageData: ByteArray?, digestData: ByteArray?): Boolean {
        return Arrays.equals(imageData, digestData)
    }

    fun getDigest(imageData: ByteArray?): ByteArray {
        return try {
            val messageDigest = MessageDigest.getInstance(DIGEST_ALGO)
            messageDigest.digest(imageData)
        } catch (ex: Exception) {
            Log.e(TAG, ex.toString())
            throw UnsupportedOperationException(DIGEST_ALGO + " algorithm not available on this device.")
        }
    }

    fun checkDigest(digestBytes: ByteArray?, updateFile: File?): Boolean {
        val calculatedDigest = getDigest(updateFile)
        if (calculatedDigest == null) {
            Log.e(TAG, "calculatedDigest null")
            return false
        }
        return Arrays.equals(calculatedDigest, digestBytes)
    }

    fun getDigest(updateFile: File?): ByteArray? {
        val digest: MessageDigest
        digest = try {
            MessageDigest.getInstance(DIGEST_ALGO)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Exception while getting digest", e)
            return null
        }
        val `is`: InputStream
        `is` = try {
            FileInputStream(updateFile)
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Exception while getting FileInputStream", e)
            return null
        }
        val buffer = ByteArray(8192)
        var read: Int
        return try {
            while (`is`.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
            digest.digest()
        } catch (e: IOException) {
            throw RuntimeException("Unable to process file for " + DIGEST_ALGO, e)
        } finally {
            try {
                `is`.close()
            } catch (e: IOException) {
                Log.e(TAG, "Exception on closing input stream", e)
            }
        }
    }
}