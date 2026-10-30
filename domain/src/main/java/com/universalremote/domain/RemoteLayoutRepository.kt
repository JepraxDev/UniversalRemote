package com.universalremote.domain

import com.universalremote.core.model.RemoteLayoutPreference

interface RemoteLayoutRepository {
    suspend fun getLayout(deviceId: String): RemoteLayoutPreference?
    suspend fun saveLayout(preference: RemoteLayoutPreference)
    suspend fun deleteLayout(deviceId: String)
}
