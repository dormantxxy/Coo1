package com.example.c001apk.compose.util

import android.content.Context
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 数字联盟 (cn.shuzilm) DUID / 酷安“数字联盟ID”获取算法。
 *
 * 算法来源：https://github.com/kongwufang/szlm_res/
 *  - p = MD5(packageName + "." + SALT_PKG).uppercase()
 *  - body = zlib.compress(JSON) XOR pool_key(0x110DC8)
 *  - POST https://auni.telecome.cn/a/mdna/report?v=8.7&t=m&p=...&r=...&n=0&l=2&h=0
 *  - response body 同样 XOR + zlib，取 JSON 的 n_a 字段即 DUID。
 *
 * 服务端只以 (sfOo, foO, 2cO) 三值识别设备；这里按设备首次运行生成并持久化
 * 一组稳定三值（同时生成 w91/gEd/6yY 三个配套指纹），因此同一设备每次启动
 * 都会得到同一个 DUID。失败时回退到仓库内置固定三值。
 */
object SzlmIdFetcher {

    private const val HOST = "auni.telecome.cn"
    private const val PATH = "/a/mdna/report?v=8.7&t=m"
    private const val SALT_PKG = "37be8a1ee106979a"
    private const val PACKAGE = "com.coolapk.market"
    private const val KEY = "xp7j@&!v5]2k#+3z{n9\$q^r%[8f1*d4e:c>0l?m/6)yb-=w}"
    private const val ALPHA = "!#\$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"

    private const val DEFAULT_SF = "801C14F"
    private const val DEFAULT_FO = "RP|Q|zQKOJ~I~}Q"
    private const val DEFAULT_TS = "1791105420.764024281"

    private const val TEMPLATE_JSON = """{"DUv":"com.coolapk.market","Qg1":1790980402778,"LMi":1790980402778,"Zrs":2609151,"EV2":"16.6.2","SAN":27,"h6Z":"酷安","R41":"8.1","Ra":"ABSENT","R53":"0","R63":"running","QgA":1,"qH9":"0","iZS":0,"ZZA":0,"S6e":1,"90P":"IJ","Q23":"user:e4e40166.0+CN=数据包捕获CA证书;user:87bc3517.0+OU=HttpCanary,O=HttpCanary,CN=HttpCanary Root CA;","YWu":"127.0.0.1+localhost;::1+ip6-localhost;","bzt":"0000","5mZ":0,"0KQ":0,"Chh":0,"Xl9":"0000","D32":4,"LAh":"IIJIIIII","wlM":1,"nur":0,"tth":1,"c2O":"1","PV8":"7","AYk":"13A365:CCAF40:AD4AA9","P1J":"13A365:CCAF40:AD4AA9","R37":"TK Watch","R3d":"TK Watch","AAA":"v1.0","BBB":"v1.0","3mS":"v9.0.1","2cO":"1791105420.764024281","K5f":"F8E4D9D1F967D773DFA6381BA10102E9","GVp":"coolapk","iYB":{"txo":"Iz1(&","Zne":"vOM|S}RL~S}SQSNLNzSIKEOQ}SQJK S MI~KQSNQRM9MHLIII9MH|zP~SPPKNSI~tMH~M OS }QRI~KQSNQRM9MHR}MJSOO JSQSNLNzSIKEKPGGGJEONRS}KJ{SIOSQ ","MCN":"vGGGJ9J SL   SO I~KQSNQRMHJIIIHEOzM~S K}OSQSNLNzSIKt","mch":"bb_p","AWo":"IIIIIJIIII","8tS":1048576,"Hsz":1048576},"7S2":{"0":{"txo":"Iz1(&","Zne":"vOM|S}RL~S}SQSNLNzSIKEOQ}SQJK S MI~KQSNQRM9MHLIII9MH|zP~SPPKNSI~tMH~M OS }QRI~KQSNQRM9MHR}MJSOO JSQSNLNzSIKEKPGGGJEONRS}KJ{SIOSQ ","MCN":"vGGGJ9J SL   SO I~KQSNQRMHJIIIHEOzM~S K}OSQSNLNzSIKt"}},"ne8":"97313e29420c51d0d8cc6e3a3586889a","sfOo":"801C14F","foO":"RP|Q|zQKOJ~I~}Q","dJs":"IMPIIJPK}KJQKIJNI|NMINIMMMLNNIIJ","w91":"529D912CF3C16EC094E0AD57FFD19589","G8U":{"MYA":"IOPOIOKMIEPRQ","H4S":"PIMGEPMMNNOOJNNPGK"},"zvW":"||RJI}JKQKQR{}{OON}KQJQMJR}}RN I","gEd":"9e077ef78ef33b84b98eeb2615feacee","5Tv":"||I}z LRO~| ~PLK}K}z{~KL LzL}{ P","52J":"RLOJREKKKESEKMPELOSLEJIREKOSKEKQRJISMJESNPKOSRKRSRKIIJSIRJLOMNQSORRLOOMISLJEKQNJJMP","p1m":"PIIJ2NMINI","Bos":"PQRRJQJRNPERRRIGJIOLJRRRIGJIOLJPQRRJQJRNP","IJK":"PRMILMMIJPERRROGJRPLJKJINGQNJRJLRRRJKKQNP","ura":"PRMILMMIJPERRROGJRPLJKJINGQNJRJLRRRJKKQNP","ibF":"IIIFFI","IAI":"JPMIMILNNPERRRLGJIOLJOPIRGQOQOJOQRRNRJRNP","aFw":"NRMIRJMIJPERRRKGIRPLJOJIRGLNJRJMRRRPRKQNP","KyU":"JRMIPNLIJPRJIRGRNJRJ","6yY":"0306f8ee3e9e99964ce29c62439d4e5f937e2f9189c18d6a8e844d27eb38b95b","UOv":"NQQLRLRQJPKQRJGKNIRJ","JPd":"0","J7w":0,"zpA":"WIFI","bf7":"0","wSK":"10.0.0.37","fPV":"CN,zh","Db2":2,"QVn":0,"msg":{"custom":""},"dLH":" NNzM~FRQKLFP{K| |JOLRONLQFKMzKFPOO{","4VP":1,"za7":"1_1791105420_","75c":"11e7b222083a4b732b4b14811f6fc05995a01415eb","fxb":100344,"B3k":67020,"cHo":0}"""

    private val mediaType = "application/x-www-form-urlencoded".toMediaType()

    @Volatile
    private var cached: String? = null

    fun getCached(): String? = cached

    /**
     * 丢弃本机缓存的身份三值并重新生成，用于“刷新数字联盟 ID”。
     * 调用后需要重新 fetch 才能拿到新的 DUID。
     */
    fun resetIdentity(context: Context) {
        cached = null
        context.getSharedPreferences("szlm_identity", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    /**
     * 强制重新获取 DUID（跳过 cached），成功后写入缓存。
     * Blocking — 调用方需放在 Dispatchers.IO。
     */
    fun refresh(context: Context): String? {
        cached = null
        return fetch(context)
    }

    /**
     * Fetch DUID. Blocking — call from Dispatchers.IO.
     */
    fun fetch(context: Context): String? {
        cached?.let { return it }
        val identity = getOrCreateIdentity(context)
        val result = request(context, identity)
            ?: request(
                context,
                Identity(DEFAULT_SF, DEFAULT_FO, DEFAULT_TS,
                    "529D912CF3C16EC094E0AD57FFD19589",
                    "9e077ef78ef33b84b98eeb2615feacee",
                    "0306f8ee3e9e99964ce29c62439d4e5f937e2f9189c18d6a8e844d27eb38b95b")
            )
        if (!result.isNullOrEmpty() && !result.all { it == '0' }) {
            cached = result
            return result
        }
        return null
    }

    private fun request(context: Context, identity: Identity): String? {
        return try {
            val obj = JSONObject(TEMPLATE_JSON)
            val now = System.currentTimeMillis()
            obj.put("DUv", PACKAGE)
            obj.put("Qg1", now)
            obj.put("LMi", now)
            obj.put("Zrs", 2609151)
            obj.put("EV2", "16.6.2")
            obj.put("SAN", Build.VERSION.SDK_INT)
            obj.put("h6Z", appName(context))
            obj.put("sfOo", identity.sf)
            obj.put("foO", identity.fo)
            obj.put("2cO", identity.ts)
            obj.put("w91", identity.w91)
            obj.put("gEd", identity.gEd)
            obj.put("6yY", identity.sixY)

            val json = obj.toString().replace("/", "\\/").toByteArray(Charsets.UTF_8)
            val body = xor(zlibCompress(json), KEY.toByteArray(Charsets.UTF_8))
            val p = md5Upper("$PACKAGE.$SALT_PKG")
            val r = randomHex(32)
            val url = "https://$HOST$PATH&p=$p&r=$r&n=0&l=2&h=0"

            val client = insecureClient()
            val request = Request.Builder()
                .url(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/${Build.ID})")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "identity")
                .post(body.toRequestBody(mediaType))
                .build()
            client.newCall(request).execute().use { response ->
                val raw = response.body?.bytes() ?: return null
                val decoded = runCatching { zlibDecompress(xor(raw, KEY.toByteArray(Charsets.UTF_8))) }
                    .getOrElse { xor(raw, KEY.toByteArray(Charsets.UTF_8)) }
                val jsonObj = JSONObject(String(decoded, Charsets.UTF_8))
                val duid = jsonObj.optString("n_a", "")
                if (duid.isNotEmpty() && !duid.all { it == '0' }) duid else null
            }
        } catch (e: Exception) {
            Log.e("SzlmIdFetcher", "fetch failed: ${e.message}", e)
            null
        }
    }

    private fun appName(context: Context): String {
        return try {
            val appInfo = context.applicationInfo
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            "酷安"
        }
    }

    private fun getOrCreateIdentity(context: Context): Identity {
        val sp = context.getSharedPreferences("szlm_identity", Context.MODE_PRIVATE)
        val sf = sp.getString("sfOo", null)
        val fo = sp.getString("foO", null)
        val ts = sp.getString("2cO", null)
        val w91 = sp.getString("w91", null)
        val gEd = sp.getString("gEd", null)
        val sixY = sp.getString("6yY", null)
        if (!sf.isNullOrEmpty() && !fo.isNullOrEmpty() && !ts.isNullOrEmpty() &&
            !w91.isNullOrEmpty() && !gEd.isNullOrEmpty() && !sixY.isNullOrEmpty()
        ) {
            return Identity(sf, fo, ts, w91, gEd, sixY)
        }
        val rnd = SecureRandom()
        val newSf = String.format(Locale.US, "%07X", rnd.nextInt(0x10000000))
        val newFo = buildString { repeat(15) { append(ALPHA[rnd.nextInt(ALPHA.length)]) } }
        val newTs = String.format(Locale.US, "%.0f.%09d", System.currentTimeMillis() / 1000.0, rnd.nextInt(1_000_000_000))
        val newW91 = randomHexUpper(32)
        val newGEd = randomHex(32)
        val new6yY = randomHex(64)
        sp.edit().putString("sfOo", newSf).putString("foO", newFo).putString("2cO", newTs)
            .putString("w91", newW91).putString("gEd", newGEd).putString("6yY", new6yY).apply()
        return Identity(newSf, newFo, newTs, newW91, newGEd, new6yY)
    }

    private fun md5Upper(text: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02X".format(it) }
    }

    private fun randomHex(length: Int): String {
        val chars = "0123456789abcdef"
        val rnd = SecureRandom()
        return buildString { repeat(length) { append(chars[rnd.nextInt(16)]) } }
    }

    private fun randomHexUpper(length: Int): String = randomHex(length).uppercase(Locale.US)

    private fun zlibCompress(data: ByteArray): ByteArray {
        val deflater = Deflater(6, false)
        deflater.setInput(data)
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (!deflater.finished()) {
            val n = deflater.deflate(buffer)
            out.write(buffer, 0, n)
        }
        deflater.end()
        return out.toByteArray()
    }

    private fun zlibDecompress(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (!inflater.finished()) {
            val n = inflater.inflate(buffer)
            if (n == 0) {
                if (inflater.needsInput()) break
                continue
            }
            out.write(buffer, 0, n)
        }
        inflater.end()
        return out.toByteArray()
    }

    private fun xor(data: ByteArray, key: ByteArray): ByteArray {
        return ByteArray(data.size) { i -> (data[i].toInt() xor key[i % key.size].toInt()).toByte() }
    }

    private fun insecureClient(): OkHttpClient {
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = emptyArray()
        })
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAll, SecureRandom())
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAll[0] as X509TrustManager)
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private data class Identity(
        val sf: String,
        val fo: String,
        val ts: String,
        val w91: String,
        val gEd: String,
        val sixY: String,
    )
}
