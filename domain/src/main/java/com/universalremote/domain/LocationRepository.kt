package com.universalremote.domain

import com.universalremote.core.model.Location
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeLocations(): Flow<List<Location>>
    suspend fun find(id: String): Location?
    suspend fun insert(location: Location)
    suspend fun delete(id: String)
}
