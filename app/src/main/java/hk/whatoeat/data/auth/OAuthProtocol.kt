package hk.whatoeat.data.auth
import hk.whatoeat.core.model.*
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
object OAuthProtocol {
    fun random()=ByteArray(32).also {SecureRandom().nextBytes(it)}.let {Base64.getUrlEncoder().withoutPadding().encodeToString(it)}
    fun challenge(verifier: String)=Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
    fun authorize(base: String,scheme: String,provider: Provider,verifier: String,nonce: String): String {
        val redirect="$scheme://auth/callback?flow="+URLEncoder.encode(nonce,"UTF-8")
        return base.trimEnd('/')+"/auth/v1/authorize?provider=${provider.wire}&redirect_to="+URLEncoder.encode(redirect,"UTF-8")+"&code_challenge="+challenge(verifier)+"&code_challenge_method=s256"
    }
    fun callback(raw: String,scheme: String,nonce: String): String {
        val uri=runCatching {URI(raw)}.getOrElse {throw AppFailure(Failure.Login)}
        if(uri.scheme!=scheme || uri.host!="auth" || uri.path!="/callback") throw AppFailure(Failure.Login)
        val fields=uri.rawQuery.orEmpty().split('&').filter {it.contains('=')}.associate {part ->
            val p=part.split('=',limit=2);URLDecoder.decode(p[0],"UTF-8") to URLDecoder.decode(p[1],"UTF-8")
        }
        if(fields["flow"]!=nonce) throw AppFailure(Failure.Login)
        if(fields["error"]=="access_denied") throw AppFailure(Failure.Cancelled)
        if(fields.containsKey("error")) throw AppFailure(Failure.Login)
        return fields["code"]?.takeIf {it.isNotBlank()} ?: throw AppFailure(Failure.Login)
    }
}
