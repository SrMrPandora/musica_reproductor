package com.example.reproductordeaudio.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "card_identity")
data class CardIdentityEntity(
    @PrimaryKey val mediaStoreId: Long,
    val colorsJson: String,
    val textColorArgb: Int
)
