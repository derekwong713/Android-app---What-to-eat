package hk.whatoeat.data.backend

import hk.whatoeat.BuildConfig
import hk.whatoeat.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

interface JsonTransport {suspend fun request(method: String,path: String,body: JSONObject?=null,token: String?=null): Any}
class SupabaseHttp(private val base: String,private val publicKey: String): JsonTransport {
    fun configured(): Boolean=runCatching {
        val url=URL(base)
        val local=BuildConfig.DEBUG && url.host in setOf("localhost","127.0.0.1","10.0.2.2")
        require(url.protocol=="https" || local)
        require(publicKey.length>20 && !publicKey.startsWith("sb_secret_") && !publicKey.contains("YOUR_"))
        if(publicKey.count {it=='.'}==2) {
            val role=JSONObject(String(Base64.getUrlDecoder().decode(publicKey.split('.')[1]))).optString("role")
            require(role=="anon")
        } else require(publicKey.startsWith("sb_publishable_"))
    }.isSuccess
    override suspend fun request(method: String,path: String,body: JSONObject?,token: String?): Any = withContext(Dispatchers.IO) {
        if(!configured()) throw AppFailure(Failure.Configuration)
        require(path.startsWith('/') && !path.startsWith("//"))
        val connection=URL(base.trimEnd('/')+path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod=method;connection.connectTimeout=15000;connection.readTimeout=15000
            connection.instanceFollowRedirects=false
            connection.setRequestProperty("apikey",publicKey)
            val bearer=token ?: publicKey.takeIf {it.count {c -> c=='.'}==2}
            bearer?.let {connection.setRequestProperty("Authorization","Bearer $it")}
            connection.setRequestProperty("Content-Type","application/json")
            if(body!=null) {connection.doOutput=true;connection.outputStream.use {it.write(body.toString().toByteArray(Charsets.UTF_8))}}
            val status=connection.responseCode
            val stream=if(status in 200..299) connection.inputStream else connection.errorStream
            val raw=stream?.bufferedReader()?.use {reader ->
                val out=StringBuilder();val buffer=CharArray(4096)
                while(true) {val n=reader.read(buffer);if(n<0) break;out.append(buffer,0,n);if(out.length>2_000_000) throw AppFailure(Failure.Network)}
                out.toString()
            }.orEmpty()
            if(status !in 200..299) {
                val message=runCatching {JSONObject(raw).optString("message")}.getOrDefault("")
                val code=runCatching {JSONObject(raw).optString("code")}.getOrDefault("")
                throw AppFailure(when {
                    "version_conflict" in message->Failure.Conflict
                    "snapshot_expired" in message || "snapshot_changed" in message->Failure.SnapshotExpired
                    "rate_limited" in message || status==429->Failure.RateLimited
                    "auth_required" in message || status==401->Failure.Login
                    "forbidden" in message || status==403->Failure.Forbidden
                    "invalid_input" in message || code in setOf("23514","23502","22P02","23503") || status==422->Failure.InvalidInput
                    code=="PGRST202" || code=="PGRST205"->Failure.Configuration
                    status==404 || "not_found" in message->Failure.DetailsUnavailable
                    else->Failure.Network
                })
            }
            if(raw.isBlank()) JSONObject() else JSONTokener(raw).nextValue()
        } catch(e: AppFailure) {throw e}
        catch(e: Exception) {throw AppFailure(Failure.Network)}
        finally {connection.disconnect()}
    }
}
