package com.meetmyartist.miner.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.meetmyartist.miner.data.model.CloudMiningConfig
import com.meetmyartist.miner.data.model.CloudWallet
import com.meetmyartist.miner.data.model.MiningConfig
import com.meetmyartist.miner.data.model.WalletInfo
import com.meetmyartist.miner.data.model.WalletService
import com.meetmyartist.miner.data.model.toWalletInfo
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncManager @Inject constructor() {

    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private fun getUserDocRef() = auth.currentUser?.uid?.let { db.collection("users").document(it) }

    suspend fun syncWallet(wallet: WalletInfo, ownerAccountId: String?): Result<Unit> = runCatching {
        val collection = getUserDocRef()?.collection("wallets") ?: throw IllegalStateException("User not authenticated")
        val payload = wallet.toCloudModel(ownerAccountId)
        collection.document(payload.address).set(payload).await()
    }

    suspend fun deleteWallet(address: String): Result<Unit> = runCatching {
        val collection = getUserDocRef()?.collection("wallets") ?: throw IllegalStateException("User not authenticated")
        collection.document(address).delete().await()
    }

    suspend fun syncConfig(config: MiningConfig): Result<Unit> = runCatching {
        val collection = getUserDocRef()?.collection("configs") ?: throw IllegalStateException("User not authenticated")
        val docId = configDocumentId(config)
        val payload = config.toCloudModel(docId)
        collection.document(docId).set(payload).await()
    }

    suspend fun deleteConfig(config: MiningConfig): Result<Unit> = runCatching {
        val collection = getUserDocRef()?.collection("configs") ?: throw IllegalStateException("User not authenticated")
        val docId = configDocumentId(config)
        collection.document(docId).delete().await()
    }

    suspend fun fetchWallets(): Result<List<WalletInfo>> = runCatching {
        val collection = getUserDocRef()?.collection("wallets") ?: throw IllegalStateException("User not authenticated")
        val snapshot = collection.get().await()
        snapshot.documents.mapNotNull { doc ->
            doc.toObject<CloudWallet>()?.toWalletInfo()
        }
    }

    suspend fun fetchConfigs(): Result<List<CloudMiningConfig>> = runCatching {
        val collection = getUserDocRef()?.collection("configs") ?: throw IllegalStateException("User not authenticated")
        val snapshot = collection.get().await()
        snapshot.documents.mapNotNull { doc ->
            doc.toObject<CloudMiningConfig>()?.copy(id = doc.id)
        }
    }

    suspend fun setActiveConfig(config: MiningConfig): Result<Unit> = runCatching {
        val collection = getUserDocRef()?.collection("configs") ?: throw IllegalStateException("User not authenticated")
        val activeId = configDocumentId(config)
        val snapshot = collection.get().await()
        snapshot.documents.forEach { doc ->
            val isActive = doc.id == activeId
            doc.reference.update("isActive", isActive).await()
        }
    }

    private fun walletDocumentId(wallet: WalletInfo): String = wallet.address

    private fun MiningConfig.toCloudModel(id: String): CloudMiningConfig = CloudMiningConfig(
        id = id,
        cryptocurrency = cryptocurrency,
        algorithm = algorithm,
        poolUrl = poolUrl,
        poolPort = poolPort,
        walletAddress = walletAddress,
        workerName = workerName,
        threadCount = threadCount,
        cpuUsagePercent = cpuUsagePercent,
        isActive = isActive,
        createdAt = createdAt
    )

    private fun WalletInfo.toCloudModel(ownerAccountId: String?): CloudWallet = CloudWallet(
        address = walletDocumentId(this),
        cryptocurrency = cryptocurrency,
        label = label,
        walletService = walletService.name,
        linkedGoogleId = ownerAccountId ?: linkedGoogleId,
        balance = balance,
        createdAt = createdAt,
        lastSyncedAt = System.currentTimeMillis()
    )

    private fun configDocumentId(config: MiningConfig): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val raw = "${config.walletAddress}_${config.workerName}_${config.cryptocurrency}"
        return digest.digest(raw.toByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
            .take(40)
    }
}
