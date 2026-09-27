package com.example.reproductordeaudio.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CardIdentityDao {
    @Upsert
    suspend fun upsert(entity: CardIdentityEntity)

    @Query("SELECT * FROM card_identity")
    fun getAllCardIdentities(): Flow<List<CardIdentityEntity>>

    @Query("SELECT * FROM card_identity WHERE mediaStoreId = :mediaStoreId LIMIT 1")
    fun getById(mediaStoreId: Long): Flow<CardIdentityEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM card_identity WHERE mediaStoreId = :mediaStoreId)")
    suspend fun exists(mediaStoreId: Long): Boolean
}
