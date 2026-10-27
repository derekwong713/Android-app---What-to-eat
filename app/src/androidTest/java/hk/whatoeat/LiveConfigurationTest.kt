package hk.whatoeat
import hk.whatoeat.data.auth.*
import hk.whatoeat.data.backend.*
import hk.whatoeat.core.model.*
import org.json.JSONObject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
class LiveConfigurationTest {
 @Test fun authSuccessCancelFailureAndPendingActionContract()=runBlocking {
 val vault=object: AuthVault {var data=JSONObject();override fun read()=JSONObject(data.toString());override fun write(value: JSONObject) {data=JSONObject(value.toString())}}
 val http=object: JsonTransport {override suspend fun request(method: String,path: String,body: JSONObject?,token: String?): Any=JSONObject().put("user",JSONObject().put("id","u1")).put("access_token","access").put("refresh_token","refresh").put("expires_in",3600)}
 val auth=SupabaseAuthRepository(http,"https://example.supabase.co","hk.whatoeat",vault,TimeSource {1000})
 val pending=PendingAction("comment","r1");auth.begin(Provider.Google,pending);var nonce=vault.read().getJSONObject("flow").getString("nonce");assertEquals(pending,auth.callback("hk.whatoeat://auth/callback?flow=$nonce&code=ok"));assertEquals("access",auth.token());assertNotNull(auth.user.value)
 auth.begin(Provider.Facebook,pending);nonce=vault.read().getJSONObject("flow").getString("nonce");try {auth.callback("hk.whatoeat://auth/callback?flow=$nonce&error=access_denied");fail()} catch(e: AppFailure) {assertEquals(Failure.Cancelled,e.kind)}
 auth.begin(Provider.Google,pending);nonce=vault.read().getJSONObject("flow").getString("nonce");try {auth.callback("hk.whatoeat://auth/callback?flow=$nonce&error=server_error");fail()} catch(e: AppFailure) {assertEquals(Failure.Login,e.kind)};auth.logout();assertNull(auth.user.value)
 }
 @Test fun serviceKeyIsRejected() {assertFalse(SupabaseHttp("https://example.supabase.co","sb_secret_never_put_this_in_an_app").configured())}
}
