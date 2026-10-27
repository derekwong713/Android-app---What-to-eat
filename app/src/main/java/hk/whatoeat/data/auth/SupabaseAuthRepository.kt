package hk.whatoeat.data.auth
import hk.whatoeat.core.model.*
import hk.whatoeat.data.backend.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

class SupabaseAuthRepository(private val http: JsonTransport,private val base: String,private val scheme: String,private val vault: AuthVault,private val time: TimeSource): AuthRepository {
    private val mutex=Mutex()
    private val mutable=MutableStateFlow<User?>(null)
    override val user=mutable.asStateFlow()
    private fun pending(j: JSONObject): PendingAction?=j.optJSONObject("pending")?.let {PendingAction(it.getString("kind"),it.getString("restaurant_id"))}
    private fun publish(j: JSONObject) {mutable.value=j.optJSONObject("session")?.let {User(it.getString("user_id"),it.optString("nickname","食客-"+it.getString("user_id").take(6)))}}
    override suspend fun restore()=mutex.withLock {publish(vault.read())}
    override suspend fun begin(provider: Provider,pending: PendingAction?): String=mutex.withLock {
        if(base.isBlank()) throw AppFailure(Failure.Configuration)
        val j=vault.read();val verifier=OAuthProtocol.random();val nonce=OAuthProtocol.random()
        j.put("flow",JSONObject().put("verifier",verifier).put("nonce",nonce).put("created_at",time.now()))
        j.put("pending",pending?.let {JSONObject().put("kind",it.kind).put("restaurant_id",it.restaurantId)} ?: JSONObject.NULL)
        vault.write(j)
        OAuthProtocol.authorize(base,scheme,provider,verifier,nonce)
    }
    private fun saveSession(j: JSONObject,response: JSONObject) {
        val id=response.getJSONObject("user").getString("id")
        val nickname=j.optJSONObject("session")?.takeIf {it.optString("user_id")==id}?.optString("nickname") ?: ("食客-"+id.take(6))
        j.put("session",JSONObject().put("access_token",response.getString("access_token")).put("refresh_token",response.getString("refresh_token")).put("expires_at",time.now()+response.getLong("expires_in")*1000).put("user_id",id).put("nickname",nickname))
        vault.write(j);publish(j)
    }
    override suspend fun callback(uri: String): PendingAction?=mutex.withLock {
        val j=vault.read();val flow=j.optJSONObject("flow") ?: throw AppFailure(Failure.Login)
        if(time.now()-flow.getLong("created_at") !in 0..600000L) {j.remove("flow");vault.write(j);throw AppFailure(Failure.Login)}
        try {
            val code=OAuthProtocol.callback(uri,scheme,flow.getString("nonce"))
            val response=http.request("POST","/auth/v1/token?grant_type=pkce",JSONObject().put("auth_code",code).put("code_verifier",flow.getString("verifier"))) as JSONObject
            j.remove("flow");saveSession(j,response);pending(j)
        } catch(e: AppFailure) {if(e.kind==Failure.Cancelled) {j.remove("flow");j.remove("pending");vault.write(j)};throw e}
    }
    override suspend fun token(): String?=mutex.withLock {
        val j=vault.read();val session=j.optJSONObject("session") ?: return@withLock null
        if(session.getLong("expires_at")>time.now()+60000) return@withLock session.getString("access_token")
        try {
            val response=http.request("POST","/auth/v1/token?grant_type=refresh_token",JSONObject().put("refresh_token",session.getString("refresh_token"))) as JSONObject
            saveSession(j,response);response.getString("access_token")
        } catch(e: AppFailure) {if(e.kind in setOf(Failure.Login,Failure.Forbidden)) {j.remove("session");vault.write(j);mutable.value=null};throw e}
    }
    override suspend fun pending()=mutex.withLock {pending(vault.read())}
    override suspend fun clearPending()=mutex.withLock {val j=vault.read();j.remove("pending");vault.write(j)}
    override suspend fun cancel()=mutex.withLock {val j=vault.read();j.remove("flow");j.remove("pending");vault.write(j)}
    override suspend fun logout() {
        val current=runCatching {token()}.getOrNull()
        try {if(current!=null) http.request("POST","/auth/v1/logout?scope=local",JSONObject(),current)}
        catch(e: Exception) {if(e is kotlinx.coroutines.CancellationException) throw e}
        finally {mutex.withLock {vault.write(JSONObject());mutable.value=null}}
    }
    override suspend fun deleteAccount() {
        val access=token() ?: throw AppFailure(Failure.Login)
        http.request("POST","/functions/v1/delete-account",JSONObject(),access)
        mutex.withLock {vault.write(JSONObject());mutable.value=null}
    }
    override suspend fun cacheNickname(value: String)=mutex.withLock {val j=vault.read();j.optJSONObject("session")?.put("nickname",value);vault.write(j);publish(j)}
}

/** Only the clearly marked demo flavor uses fictional local identities and no OAuth requests. */
class DemoAuthRepository: AuthRepository {
    private val mutable=MutableStateFlow<User?>(null)
    override val user=mutable.asStateFlow()
    private var action: PendingAction?=null
    override suspend fun restore() {}
    override suspend fun begin(provider: Provider,pending: PendingAction?): String? {action=pending;mutable.value=User("00000000-0000-4000-8000-000000000001","示範食客",true);return null}
    override suspend fun callback(uri: String): PendingAction?=throw AppFailure(Failure.Login)
    override suspend fun cancel() {action=null}
    override suspend fun pending()=action
    override suspend fun clearPending() {action=null}
    override suspend fun token()=mutable.value?.let {"demo-local-only"}
    override suspend fun logout() {mutable.value=null;action=null}
    var eraseData: suspend (String)->Unit = {}
    override suspend fun deleteAccount() {mutable.value?.let {eraseData(it.id)};logout()}
    override suspend fun cacheNickname(value: String) {mutable.value=mutable.value?.copy(nickname=value)}
}
