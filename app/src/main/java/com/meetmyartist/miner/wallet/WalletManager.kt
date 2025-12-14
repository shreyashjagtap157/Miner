package com.meetmyartist.miner.wallet

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.meetmyartist.miner.data.local.WalletDao
import com.meetmyartist.miner.data.model.WalletInfo
import com.meetmyartist.miner.data.model.WalletService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val walletDao: WalletDao
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "wallet_secrets",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    suspend fun createWallet(
        cryptocurrency: String,
        address: String,
        privateKey: String?,
        label: String,
        walletService: WalletService,
        googleId: String?
    ): Result<WalletInfo> = withContext(Dispatchers.IO) {
        try {
            // Validate address format
            if (!isValidAddress(cryptocurrency, address)) {
                return@withContext Result.failure(Exception("Invalid wallet address format"))
            }
            
            // Encrypt private key if provided
            val encryptedKey = privateKey?.let { encryptPrivateKey(it) }
            
            val wallet = WalletInfo(
                cryptocurrency = cryptocurrency,
                address = address,
                encryptedPrivateKey = encryptedKey,
                label = label,
                walletService = walletService,
                linkedGoogleId = googleId
            )
            
            val id = walletDao.insertWallet(wallet)
            Result.success(wallet.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateWalletBalance(walletId: Long, balance: Double) = withContext(Dispatchers.IO) {
        try {
            val wallet = walletDao.getWalletById(walletId)
            wallet?.let {
                walletDao.updateWallet(
                    it.copy(
                        balance = balance,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun getDecryptedPrivateKey(walletId: Long): String? = withContext(Dispatchers.IO) {
        try {
            val wallet = walletDao.getWalletById(walletId)
            wallet?.encryptedPrivateKey?.let { decryptPrivateKey(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    fun getAllWallets(): Flow<List<WalletInfo>> {
        return walletDao.getAllWallets()
    }
    
    fun getWalletsByCrypto(crypto: String): Flow<List<WalletInfo>> {
        return walletDao.getWalletsByCrypto(crypto)
    }
    
    fun getWalletsByGoogleId(googleId: String): Flow<List<WalletInfo>> {
        return walletDao.getWalletsByGoogleId(googleId)
    }
    
    suspend fun deleteWallet(wallet: WalletInfo) = withContext(Dispatchers.IO) {
        walletDao.deleteWallet(wallet)
    }

    suspend fun getWalletByAddress(address: String): WalletInfo? = withContext(Dispatchers.IO) {
        walletDao.getWalletByAddress(address)
    }

    suspend fun syncWalletsFromCloud(ownerAccountId: String?, remoteWallets: List<WalletInfo>) = withContext(Dispatchers.IO) {
        val googleId = ownerAccountId ?: remoteWallets.firstOrNull { it.linkedGoogleId != null }?.linkedGoogleId
        googleId ?: return@withContext

        val localWallets = walletDao.getWalletsByGoogleId(googleId).first()
        val remoteAddresses = remoteWallets.map { it.address }.toSet()

        remoteWallets.forEach { remote ->
            val existing = walletDao.getWalletByAddress(remote.address)
            if (existing == null) {
                walletDao.insertWallet(
                    remote.copy(
                        id = 0,
                        encryptedPrivateKey = null,
                        linkedGoogleId = googleId,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                )
            } else {
                walletDao.updateWallet(
                    existing.copy(
                        cryptocurrency = remote.cryptocurrency,
                        label = remote.label,
                        walletService = remote.walletService,
                        balance = remote.balance,
                        linkedGoogleId = googleId,
                        lastSyncedAt = remote.lastSyncedAt
                    )
                )
            }
        }

        localWallets
            .filter { it.linkedGoogleId == googleId && it.address !in remoteAddresses }
            .forEach { walletDao.deleteWallet(it) }
    }
    
    private fun encryptPrivateKey(privateKey: String): String {
        // Store in encrypted shared preferences
        val hash = hashString(privateKey)
        encryptedPrefs.edit().putString(hash, privateKey).apply()
        return hash
    }
    
    private fun decryptPrivateKey(hash: String): String? {
        return encryptedPrefs.getString(hash, null)
    }
    
    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    private fun isValidAddress(cryptocurrency: String, address: String): Boolean {
        return when (cryptocurrency.uppercase()) {
            "BTC", "BITCOIN" -> {
                // Bitcoin address validation (simplified)
                address.length in 26..35 && (address.startsWith("1") || 
                    address.startsWith("3") || address.startsWith("bc1"))
            }
            "ETH", "ETHEREUM", "ETC" -> {
                // Ethereum address validation
                address.length == 42 && address.startsWith("0x")
            }
            "XMR", "MONERO" -> {
                // Monero address validation
                address.length in 95..106 && (address.startsWith("4") || address.startsWith("8"))
            }
            "LTC", "LITECOIN" -> {
                // Litecoin address validation
                address.length in 26..35 && (address.startsWith("L") || 
                    address.startsWith("M") || address.startsWith("ltc1"))
            }
            "DOGE", "DOGECOIN" -> {
                // Dogecoin address validation
                address.length in 34..35 && address.startsWith("D")
            }
            "ZEC", "ZCASH" -> {
                // Zcash address validation
                address.length in 35..95 && (address.startsWith("t") || address.startsWith("z"))
            }
            "RVN", "RAVENCOIN" -> {
                // Ravencoin address validation
                address.length in 34..35 && address.startsWith("R")
            }
            else -> true // Allow any format for unknown cryptocurrencies
        }
    }
    
    // Predefined popular mining pools
    fun getPopularPools(cryptocurrency: String): List<MiningPoolInfo> {
        return when (cryptocurrency.uppercase()) {
            "XMR", "MONERO" -> listOf(
                MiningPoolInfo("SupportXMR", "pool.supportxmr.com", 3333, 0.6),
                MiningPoolInfo("MoneroOcean", "gulf.moneroocean.stream", 10128, 0.0),
                MiningPoolInfo("HashVault", "pool.hashvault.pro", 3333, 0.9)
            )
            "BTC", "BITCOIN" -> listOf(
                MiningPoolInfo("Slush Pool", "stratum.slushpool.com", 3333, 2.0),
                MiningPoolInfo("F2Pool", "btc.f2pool.com", 3333, 2.5),
                MiningPoolInfo("Antpool", "stratum.antpool.com", 3333, 1.0)
            )
            "LTC", "LITECOIN" -> listOf(
                MiningPoolInfo("LitecoinPool", "stratum.litecoinpool.org", 3333, 0.0),
                MiningPoolInfo("F2Pool", "ltc.f2pool.com", 8888, 2.5)
            )
            "ETC" -> listOf(
                MiningPoolInfo("2Miners", "etc.2miners.com", 1010, 1.0),
                MiningPoolInfo("Ethermine", "etc.ethermine.org", 4444, 1.0)
            )
            else -> emptyList()
        }
    }
}

