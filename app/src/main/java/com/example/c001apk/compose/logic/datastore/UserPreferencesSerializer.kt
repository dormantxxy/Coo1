package com.example.c001apk.compose.logic.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.example.c001apk.compose.UserPreferences
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class UserPreferencesSerializer @Inject constructor() : Serializer<UserPreferencesCompat> {
    override val defaultValue = UserPreferencesCompat.default()

    override suspend fun readFrom(input: InputStream) =
        try {
            UserPreferences.parseFrom(input).let(::UserPreferencesCompat)
        } catch (e: Exception) {
            throw CorruptionException("cannot read proto", e)
        }

    override suspend fun writeTo(t: UserPreferencesCompat, output: OutputStream) {
        t.toProto().writeTo(output)
    }
}