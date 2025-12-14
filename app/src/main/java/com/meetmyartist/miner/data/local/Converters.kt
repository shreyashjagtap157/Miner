package com.meetmyartist.miner.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.meetmyartist.miner.data.model.CoreConfig
import com.meetmyartist.miner.data.model.TransactionStatus
import com.meetmyartist.miner.data.model.TransactionType
import com.meetmyartist.miner.data.model.WalletService

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromWalletService(value: WalletService?): String? {
        return value?.name
    }

    @TypeConverter
    fun toWalletService(value: String?): WalletService? {
        return value?.let { WalletService.valueOf(it) }
    }

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? {
        return value?.let { TransactionType.valueOf(it) }
    }

    @TypeConverter
    fun fromTransactionStatus(value: TransactionStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toTransactionStatus(value: String?): TransactionStatus? {
        return value?.let { TransactionStatus.valueOf(it) }
    }

    @TypeConverter
    fun fromCoreConfigMap(value: Map<Int, CoreConfig>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toCoreConfigMap(value: String?): Map<Int, CoreConfig>? {
        if (value == null) return null
        val type = object : TypeToken<Map<Int, CoreConfig>>() {}.type
        return gson.fromJson(value, type)
    }
}
