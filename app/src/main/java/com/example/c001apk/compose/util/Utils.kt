package com.example.c001apk.compose.util

import android.app.DownloadManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Environment
import androidx.core.text.HtmlCompat
import com.example.c001apk.compose.c001Application
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random


object Utils {

    fun getAppVersion(context: Context, packageName: String): Pair<String, Long> {
        return try {
            val info = context.packageManager.getPackageInfo(packageName, 0)
            Pair(info.versionName.orEmpty(), info.longVersionCodeCompat)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair("-1", -1)
        }
    }

    fun downloadApk(context: Context, url: String, name: String) {
        val downloadManager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager
            .Request(Uri.parse(url))
            .setMimeType("application/vnd.android.package-archive")
            .setTitle(name)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
        if (SDK_INT < 29) {
            request.allowScanningByMediaScanner()
            request.setVisibleInDownloadsUi(true)
        }
        downloadManager.enqueue(request)
    }

    /**
     * 检测设备宽度是否大于等于800dp
     *
     * @receiver Configuration
     * @return boolean
     */
    fun Configuration.isTable() = this.screenWidthDp >= 800

    /**
     * 二进制数组转十六进制字符串
     *
     * @receiver byte array to be converted
     * @return string containing hex values
     */
    fun ByteArray.byteArrayToHexString(): String {
        val sb = StringBuilder(size * 2)
        for (element in this) {
            val v = element.toInt() and 0xff
            if (v < 16) {
                sb.append('0')
            }
            sb.append(Integer.toHexString(v))
        }
        return sb.toString().uppercase(Locale.US)
    }

    /**
     * 十六进制字符串转二进制数组
     *
     * @receiver string of hex-encoded values
     * @return decoded byte array
     */
    fun String.hexStringToByteArray(): ByteArray {
        val data = ByteArray(length / 2)
        for (index in indices step 2) {
            data[index / 2] = (
                    (Character.digit(this[index], 16) shl 4) +
                            Character.digit(this[index + 1], 16)
                    ).toByte()
        }
        return data
    }

    /**
     * 根据字符串生成Base64码
     *
     * @receiver 原生字符串
     * @param isRaw 是否省略“=”符号
     * @return Base64码
     */
    fun String.getBase64(isRaw: Boolean = true): String {
        var result =
            Base64Utils.encode(this.toByteArray())//Base64.getEncoder().encodeToString(this.toByteArray())
        if (isRaw) {
            result = result.replace("=", "")
        }
        return result
    }

    /**
     * 根据字符串生成MD5值
     *
     * @receiver 原生字符串
     * @return MD5值
     */
    fun String.getMD5(): String {
        val instance: MessageDigest = MessageDigest.getInstance("MD5")

        val digest = instance.digest(this.toByteArray())

        val sb = StringBuffer()

        for (b in digest) {
            val i = b.toInt() and 0xff
            var hexString = Integer.toHexString(i)
            if (hexString.length < 2) {
                hexString = "0$hexString"
            }
            sb.append(hexString)
        }

        return sb.toString().replace("-", "")
    }


    fun getInstalledAppMd5(appInfo: ApplicationInfo): String {

        return generateRandomMD5()
        // 猜测MD5与增量更新有关，因不计划支持，且获取MD5耗时较长，现随机生成

//            try {
//                // 获取应用程序的APK文件路径
//                val apkPath = appInfo.sourceDir
//
//                // 计算MD5值
//                return FileInputStream(File(apkPath)).calculateMd5()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//            return ""
    }

    fun FileInputStream.calculateMd5(): String {
        val md = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(1024)
        var length: Int
        while (read(buffer, 0, 1024).also { length = it } != -1) {
            md.update(buffer, 0, length)
        }
        close()

        val md5Bytes = md.digest()
        var i: Int
        val buf = StringBuffer("")
        for (offset in md5Bytes.indices) {
            i = md5Bytes[offset].toInt()
            if (i < 0) i += 256
            if (i < 16) buf.append("0")
            buf.append(Integer.toHexString(i))
        }
        //32位加密
        return buf.toString()
    }

    fun generateRandomMD5(): String {
        val hexChars = "0123456789abcdef"
        val stringBuilder = StringBuilder(32)

        repeat(32) {
            val randomIndex = Random.nextInt(hexChars.length)
            stringBuilder.append(hexChars[randomIndex])
        }

        return stringBuilder.toString()
    }

    /**
     * 随机生成Mac地址
     *
     * @return Mac地址
     */
    fun randomMacAddress(): String {
        val sb = StringBuilder()

        for (i in 0..5) {
            if (sb.isNotEmpty()) {
                sb.append(":")
            }
            val value = Random.nextInt(256)
            val element = Integer.toHexString(value)
            if (element.length < 2) {
                sb.append(0)
            }
            sb.append(element)
        }

        return sb.toString().uppercase()
    }

    /**
     * 随机生成oaid 32位字符串
     *
     * @return oaid
     */
    fun randomOaid(): String {
        val oaidLength = 32
        val sb = StringBuilder(oaidLength)
        for (i in 0 until oaidLength) {
            val randomChar = (Random.nextInt(26) + 'a'.code).toChar()
            sb.append(randomChar)
        }
        return sb.toString()
    }


    fun randomAndroidId(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    /** 机型 + 厂商/品牌/构建号/安卓版本一一对应，避免出现「厂商 vivo、品牌华为」这种组合。 */
    data class DeviceBuild(
        val manufacturer: String,
        val brand: String,
        val model: String,
        val buildNumber: String,
        val sdkInt: String,
        val androidVersion: String,
    )

    /**
     * 7 家厂商 × 每家 5 款 = 35 款，全部为当季在售旗舰，厂商与品牌严格对应。
     */
    private val recentDevices = listOf(
        // 华为
        DeviceBuild("HUAWEI", "HUAWEI", "PLR-AL00", "HUAWEI_PLR-AL00_6.1.0.175", "36", "16"),
        DeviceBuild("HUAWEI", "HUAWEI", "PLA-AL10", "HUAWEI_PLA-AL10_6.1.0.172", "36", "16"),
        DeviceBuild("HUAWEI", "HUAWEI", "ICL-AL10", "HUAWEI_ICL-AL10_6.0.0.158", "36", "16"),
        DeviceBuild("HUAWEI", "HUAWEI", "HBN-AL00", "HUAWEI_HBN-AL00_6.1.0.180", "36", "16"),
        DeviceBuild("HUAWEI", "HUAWEI", "GRL-AL10", "HUAWEI_GRL-AL10_6.0.0.150", "36", "16"),

        // 三星
        DeviceBuild("samsung", "samsung", "SM-S938B", "BP2A.250705.008", "36", "16"),
        DeviceBuild("samsung", "samsung", "SM-S936B", "BP2A.250705.008", "36", "16"),
        DeviceBuild("samsung", "samsung", "SM-S931B", "BP2A.250705.008", "36", "16"),
        DeviceBuild("samsung", "samsung", "SM-F966B", "BP2A.250705.009", "36", "16"),
        DeviceBuild("samsung", "samsung", "SM-F766B", "BP2A.250705.009", "36", "16"),

        // 苹果
        DeviceBuild("Apple", "Apple", "iPhone 17 Pro Max", "AP4A.250705.001", "36", "16"),
        DeviceBuild("Apple", "Apple", "iPhone 17 Pro", "AP4A.250705.001", "36", "16"),
        DeviceBuild("Apple", "Apple", "iPhone 17", "AP4A.250705.001", "36", "16"),
        DeviceBuild("Apple", "Apple", "iPhone Air", "AP4A.250705.002", "36", "16"),
        DeviceBuild("Apple", "Apple", "iPhone 16 Pro Max", "AP4A.241205.003", "35", "15"),

        // 小米
        DeviceBuild("Xiaomi", "Xiaomi", "25010PN30C", "OS3.0.2.0.WPBCNXM", "36", "16"),
        DeviceBuild("Xiaomi", "Xiaomi", "25053PC47C", "OS3.0.2.0.WPBCNXM", "36", "16"),
        DeviceBuild("Xiaomi", "Xiaomi", "24129PN74C", "OS3.0.1.0.WOBCNXM", "36", "16"),
        DeviceBuild("Xiaomi", "Xiaomi", "2405CPX3DG", "OS3.0.1.0.WOBCNXM", "36", "16"),
        DeviceBuild("Xiaomi", "Xiaomi", "24069PC21G", "OS2.1.5.0.VOBCNXM", "35", "15"),

        // vivo
        DeviceBuild("vivo", "vivo", "V2509A", "PD2509_A_16.1.10", "36", "16"),
        DeviceBuild("vivo", "vivo", "V2505A", "PD2505_A_16.0.9", "36", "16"),
        DeviceBuild("vivo", "vivo", "V2502A", "PD2502_A_16.0.9", "36", "16"),
        DeviceBuild("vivo", "vivo", "V2405A", "PD2405_A_16.0.5", "36", "16"),
        DeviceBuild("vivo", "vivo", "V2359A", "PD2359_A_15.2.8", "35", "15"),

        // 一加
        DeviceBuild("OnePlus", "OnePlus", "PJZ110", "CPH2653_16.0.1.200", "36", "16"),
        DeviceBuild("OnePlus", "OnePlus", "PKG110", "CPH2649_16.0.1.200", "36", "16"),
        DeviceBuild("OnePlus", "OnePlus", "PJD110", "CPH2573_16.0.1.100", "36", "16"),
        DeviceBuild("OnePlus", "OnePlus", "PHK110", "CPH2449_15.0.1.400", "35", "15"),
        DeviceBuild("OnePlus", "OnePlus", "PJA110", "CPH2447_15.0.1.300", "35", "15"),

        // OPPO
        DeviceBuild("OPPO", "OPPO", "PKZ110", "CPH2781_16.0.1.300", "36", "16"),
        DeviceBuild("OPPO", "OPPO", "PKC110", "CPH2791_16.0.1.300", "36", "16"),
        DeviceBuild("OPPO", "OPPO", "PKD110", "CPH2765_16.0.1.200", "36", "16"),
        DeviceBuild("OPPO", "OPPO", "PJT110", "CPH2609_15.0.1.400", "35", "15"),
        DeviceBuild("OPPO", "OPPO", "PHY110", "CPH2581_15.0.1.300", "35", "15"),
    )

    /** 随机取一台机型，厂商/品牌/型号/构建号/安卓版本彼此匹配。 */
    fun randomRecentDevice(): DeviceBuild = recentDevices.random()

    fun randomDeviceModel(): String {
        val assetManager = c001Application.assets

        try {
            val inputStream = assetManager.open("devicemodel.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = ArrayList<String>()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                lines.add(line ?: "")
            }

            val randomIndex = Random.nextInt(lines.size)
            return lines[randomIndex]
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return "null"
    }

    fun randomSdkInt(): String {
        return Random.nextInt(21, 34).toString()
    }

    fun randomAndroidVersionRelease(): String {
        val androidVersionRelease = listOf(
            "5.0.1", // Lollipop
            "6.0",   // Marshmallow
            "7.0",   // Nougat
            "7.1.1", // Nougat
            "8.0.0", // Oreo
            "8.1.0", // Oreo
            "9",     // Pie
            "10",    // Android 10
            "11",    // Android 11
            "12",     // Android 12
            "13",
            "14",
        )

        return androidVersionRelease[Random.nextInt(androidVersionRelease.size)]
    }

    /**
     * 拓展Modifier无点击效果点击方法
     *
     * @receiver Modifier
     * @return Modifier
     */
    /*fun Modifier.clickableNoRipple(onClick: () -> Unit) = composed {
        clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) {
            onClick()
        }
    }*/

    /**
     * 格式化时间戳
     *
     * @receiver 时间戳
     * @return 格式化的时间
     */
    fun Long.secondToDateString(pattern: String = "yyyy-MM-dd HH:mm:ss") =
        SimpleDateFormat(pattern, Locale.CHINA).format(Date(this))

    /**
     * 计算时间差
     *
     * @receiver old timestamp
     * @param currentTime current timestamp
     */
    fun Long.timeStampInterval(currentTime: Long): String {
        val calendarOld = Calendar.getInstance()
        val calendarNow = Calendar.getInstance()

        val dateOld = Date(this)
        val dateNow = Date(currentTime)

        calendarOld.time = dateOld
        calendarNow.time = dateNow

        val dayOld = calendarOld.get(Calendar.DAY_OF_YEAR)
        val dayNow = calendarNow.get(Calendar.DAY_OF_YEAR)

        val hourOld = calendarOld.get(Calendar.HOUR_OF_DAY)
        val hourNow = calendarNow.get(Calendar.HOUR_OF_DAY)

        val minuteOld = calendarOld.get(Calendar.MINUTE)
        val minuteNow = calendarNow.get(Calendar.MINUTE)

        return if (dayOld == dayNow) {
            if (hourOld == hourNow) {
                "${minuteNow - minuteOld}分钟前"
            } else {
                "${hourNow - hourOld}小时前"
            }
        } else {
            "${dayNow - dayOld}天前"
        }
    }

    /**
     * 格式化富文本
     *
     * @receiver 原始文本
     * @return 格式化后文本
     */
    fun String.richToString(htmlCompat: Int = HtmlCompat.FROM_HTML_MODE_COMPACT) =
        HtmlCompat.fromHtml(this, htmlCompat).toString()
}