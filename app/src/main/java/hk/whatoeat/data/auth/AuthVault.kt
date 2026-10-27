package hk.whatoeat.data.auth
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface AuthVault {fun read(): JSONObject;fun write(value: JSONObject)}
/** Tokens, PKCE verifier and pending action are encrypted and excluded from backup. */
class EncryptedAuthVault(context: Context): AuthVault {
    private val file=AtomicFile(File(context.noBackupFilesDir,"community-auth.bin"))
    private val alias=context.packageName+".auth.v1"
    private fun key(): SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply {load(null)}
        (store.getKey(alias,null) as? SecretKey)?.let {return it}
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    @Synchronized override fun read(): JSONObject = try {
        val bytes=file.openRead().use {it.readBytes()}
        require(bytes.size>13 && bytes[0].toInt()==12)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply {init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(1,13)))}
        JSONObject(String(cipher.doFinal(bytes.copyOfRange(13,bytes.size)),Charsets.UTF_8))
    } catch(_: Exception) {JSONObject()}
    @Synchronized override fun write(value: JSONObject) {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply {init(Cipher.ENCRYPT_MODE,key())}
        val bytes=byteArrayOf(cipher.iv.size.toByte())+cipher.iv+cipher.doFinal(value.toString().toByteArray(Charsets.UTF_8))
        val output=file.startWrite()
        try {output.write(bytes);file.finishWrite(output)} catch(e: Exception) {file.failWrite(output);throw e}
    }
}
