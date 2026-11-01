package com.example.c001apk.compose

import java.io.InputStream
import java.io.OutputStream

/**
 * Lightweight, hand-written replacement for the protobuf-generated
 * `UserPreferences` class. The original project generates this class with
 * protoc, but protoc is not executable on the Android device used for this
 * build. This implementation keeps the same field names and a compatible
 * protobuf wire format so existing DataStore files can still be read.
 */
class UserPreferences(
    var themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    var materialYou: Boolean = false,
    var pureBlack: Boolean = false,
    var fontScale: Float = 0f,
    var contentScale: Float = 0f,
    var szlmId: String = "",
    var imageQuality: Int = 0,
    var imageFilter: Boolean = false,
    var openInBrowser: Boolean = false,
    var showSquare: Boolean = false,
    var recordHistory: Boolean = true,
    var showEmoji: Boolean = false,
    var checkUpdate: Boolean = false,
    var checkCount: Boolean = false,
    var versionName: String = "",
    var apiVersion: String = "",
    var versionCode: String = "",
    var manufacturer: String = "",
    var brand: String = "",
    var model: String = "",
    var buildNumber: String = "",
    var sdkInt: String = "",
    var androidVersion: String = "",
    var userAgent: String = "",
    var xAppDevice: String = "",
    var xAppToken: String = "",
    var isLogin: Boolean = false,
    var userAvatar: String = "",
    var username: String = "",
    var level: String = "",
    var experience: String = "",
    var nextLevelExperience: String = "",
    var uid: String = "",
    var token: String = "",
    var followType: FollowType = FollowType.ALL,
    var recentIds: String = "",
    var checkCountPeriod: Int = 0,
    var installTime: String = "",
    var themeType: ThemeType = ThemeType.Default,
    var seedColor: String = "",
    var paletteStyle: Int = 0,
) {

    fun writeTo(output: OutputStream) {
        writeVarintField(output, 1, themeMode.ordinal)
        writeBoolField(output, 2, materialYou)
        writeBoolField(output, 3, pureBlack)
        writeFloatField(output, 4, fontScale)
        writeFloatField(output, 5, contentScale)
        writeStringField(output, 6, szlmId)
        writeVarintField(output, 7, imageQuality)
        writeBoolField(output, 8, imageFilter)
        writeBoolField(output, 9, openInBrowser)
        writeBoolField(output, 10, showSquare)
        writeBoolField(output, 11, recordHistory)
        writeBoolField(output, 12, showEmoji)
        writeBoolField(output, 13, checkUpdate)
        writeBoolField(output, 14, checkCount)
        writeStringField(output, 15, versionName)
        writeStringField(output, 16, apiVersion)
        writeStringField(output, 17, versionCode)
        writeStringField(output, 18, manufacturer)
        writeStringField(output, 19, brand)
        writeStringField(output, 20, model)
        writeStringField(output, 21, buildNumber)
        writeStringField(output, 22, sdkInt)
        writeStringField(output, 23, androidVersion)
        writeStringField(output, 24, userAgent)
        writeStringField(output, 25, xAppDevice)
        writeStringField(output, 26, xAppToken)
        writeBoolField(output, 27, isLogin)
        writeStringField(output, 28, userAvatar)
        writeStringField(output, 29, username)
        writeStringField(output, 30, level)
        writeStringField(output, 31, experience)
        writeStringField(output, 32, nextLevelExperience)
        writeStringField(output, 33, uid)
        writeStringField(output, 34, token)
        writeVarintField(output, 35, followType.ordinal)
        writeStringField(output, 36, recentIds)
        writeVarintField(output, 37, checkCountPeriod)
        writeStringField(output, 38, installTime)
        writeVarintField(output, 39, themeType.ordinal)
        writeStringField(output, 40, seedColor)
        writeVarintField(output, 41, paletteStyle)
    }

    companion object {
        fun parseFrom(input: InputStream): UserPreferences {
            val result = UserPreferences()
            while (true) {
                val tag = readVarint(input)
                if (tag < 0) break
                val field = tag ushr 3
                when (field) {
                    1 -> result.themeMode = ThemeMode.entries.getOrElse(readVarint(input)) { ThemeMode.FOLLOW_SYSTEM }
                    2 -> result.materialYou = readVarint(input) != 0
                    3 -> result.pureBlack = readVarint(input) != 0
                    4 -> result.fontScale = readFloat(input)
                    5 -> result.contentScale = readFloat(input)
                    6 -> result.szlmId = readString(input)
                    7 -> result.imageQuality = readVarint(input)
                    8 -> result.imageFilter = readVarint(input) != 0
                    9 -> result.openInBrowser = readVarint(input) != 0
                    10 -> result.showSquare = readVarint(input) != 0
                    11 -> result.recordHistory = readVarint(input) != 0
                    12 -> result.showEmoji = readVarint(input) != 0
                    13 -> result.checkUpdate = readVarint(input) != 0
                    14 -> result.checkCount = readVarint(input) != 0
                    15 -> result.versionName = readString(input)
                    16 -> result.apiVersion = readString(input)
                    17 -> result.versionCode = readString(input)
                    18 -> result.manufacturer = readString(input)
                    19 -> result.brand = readString(input)
                    20 -> result.model = readString(input)
                    21 -> result.buildNumber = readString(input)
                    22 -> result.sdkInt = readString(input)
                    23 -> result.androidVersion = readString(input)
                    24 -> result.userAgent = readString(input)
                    25 -> result.xAppDevice = readString(input)
                    26 -> result.xAppToken = readString(input)
                    27 -> result.isLogin = readVarint(input) != 0
                    28 -> result.userAvatar = readString(input)
                    29 -> result.username = readString(input)
                    30 -> result.level = readString(input)
                    31 -> result.experience = readString(input)
                    32 -> result.nextLevelExperience = readString(input)
                    33 -> result.uid = readString(input)
                    34 -> result.token = readString(input)
                    35 -> result.followType = FollowType.entries.getOrElse(readVarint(input)) { FollowType.ALL }
                    36 -> result.recentIds = readString(input)
                    37 -> result.checkCountPeriod = readVarint(input)
                    38 -> result.installTime = readString(input)
                    39 -> result.themeType = ThemeType.entries.getOrElse(readVarint(input)) { ThemeType.Default }
                    40 -> result.seedColor = readString(input)
                    41 -> result.paletteStyle = readVarint(input)
                    else -> skipField(input, tag)
                }
            }
            return result
        }

        private fun writeTag(output: OutputStream, field: Int, wireType: Int) {
            writeVarint(output, (field shl 3) or wireType)
        }

        private fun writeVarintField(output: OutputStream, field: Int, value: Int) {
            if (value == 0) return
            writeTag(output, field, 0)
            writeVarint(output, value)
        }

        private fun writeBoolField(output: OutputStream, field: Int, value: Boolean) {
            if (!value) return
            writeTag(output, field, 0)
            writeVarint(output, 1)
        }

        private fun writeFloatField(output: OutputStream, field: Int, value: Float) {
            if (value == 0f) return
            writeTag(output, field, 5)
            writeLittleEndianInt(output, java.lang.Float.floatToRawIntBits(value))
        }

        private fun writeStringField(output: OutputStream, field: Int, value: String) {
            if (value.isEmpty()) return
            val bytes = value.toByteArray(Charsets.UTF_8)
            writeTag(output, field, 2)
            writeVarint(output, bytes.size)
            output.write(bytes)
        }

        private fun writeVarint(output: OutputStream, value: Int) {
            var v = value
            while (v and -128 != 0) {
                output.write((v and 127) or 128)
                v = v ushr 7
            }
            output.write(v and 127)
        }

        private fun writeLittleEndianInt(output: OutputStream, value: Int) {
            output.write(value and 0xff)
            output.write((value ushr 8) and 0xff)
            output.write((value ushr 16) and 0xff)
            output.write((value ushr 24) and 0xff)
        }

        private fun readVarint(input: InputStream): Int {
            var result = 0
            var shift = 0
            while (true) {
                val b = input.read()
                if (b == -1) return -1
                result = result or ((b and 127) shl shift)
                if (b and 128 == 0) return result
                shift += 7
                if (shift > 28) return result
            }
        }

        private fun readString(input: InputStream): String {
            val length = readVarint(input)
            if (length <= 0) return ""
            val bytes = ByteArray(length)
            var offset = 0
            while (offset < length) {
                val read = input.read(bytes, offset, length - offset)
                if (read == -1) break
                offset += read
            }
            return String(bytes, 0, offset, Charsets.UTF_8)
        }

        private fun readFloat(input: InputStream): Float {
            var value = 0
            for (i in 0 until 4) {
                val b = input.read()
                if (b == -1) break
                value = value or ((b and 0xff) shl (8 * i))
            }
            return java.lang.Float.intBitsToFloat(value)
        }

        private fun skipField(input: InputStream, tag: Int) {
            when (tag and 7) {
                0 -> readVarint(input)
                1 -> repeat(8) { input.read() }
                2 -> {
                    val length = readVarint(input)
                    repeat(length.coerceAtLeast(0)) { input.read() }
                }
                5 -> repeat(4) { input.read() }
                else -> Unit
            }
        }
    }
}

enum class ThemeType {
    Default, Red, Pink, Purple, Indigo, Blue, LightBlue, Cyan, Teal, Green,
    LightGreen, Lime, Yellow, Amber, Orange, DeepOrange, Brown, BlueGrey,
    Sakura, Custom, UNRECOGNIZED
}

enum class FollowType {
    ALL, USER, TOPIC, PRODUCT, APP, UNRECOGNIZED
}

enum class ThemeMode {
    FOLLOW_SYSTEM, ALWAYS_ON, ALWAYS_OFF, UNRECOGNIZED
}
