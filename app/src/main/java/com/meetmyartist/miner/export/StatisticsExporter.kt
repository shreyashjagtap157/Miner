package com.meetmyartist.miner.export

import android.content.Context
import android.net.Uri
import com.meetmyartist.miner.data.model.ExportData
import com.meetmyartist.miner.data.model.MiningStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    suspend fun exportToCSV(data: List<ExportData>): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = "mining_stats_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            FileOutputStream(file).use { outputStream ->
                // Header
                val header = "Timestamp,Hashrate (H/s),CPU Temp (°C),CPU Usage (%),Accepted Shares,Rejected Shares,Uptime (s),Power (W),Earnings\n"
                outputStream.write(header.toByteArray())
                
                // Data rows
                data.forEach { record ->
                    val row = "${record.timestamp},${record.hashrate},${record.cpuTemp},${record.cpuUsage}," +
                            "${record.acceptedShares},${record.rejectedShares},${record.uptime}," +
                            "${record.powerUsage},${record.earnings}\n"
                    outputStream.write(row.toByteArray())
                }
            }
            
            Uri.fromFile(file)
        }
    }
    
    suspend fun exportToJSON(data: List<ExportData>): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = "mining_stats_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            val jsonArray = JSONArray()
            data.forEach { record ->
                val jsonObject = JSONObject().apply {
                    put("timestamp", record.timestamp)
                    put("timestampFormatted", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(record.timestamp)))
                    put("hashrate", record.hashrate)
                    put("cpuTemp", record.cpuTemp)
                    put("cpuUsage", record.cpuUsage)
                    put("acceptedShares", record.acceptedShares)
                    put("rejectedShares", record.rejectedShares)
                    put("uptime", record.uptime)
                    put("powerUsage", record.powerUsage)
                    put("earnings", record.earnings)
                }
                jsonArray.put(jsonObject)
            }
            
            val rootObject = JSONObject().apply {
                put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put("recordCount", data.size)
                put("data", jsonArray)
            }
            
            FileOutputStream(file).use { outputStream ->
                outputStream.write(rootObject.toString(2).toByteArray())
            }
            
            Uri.fromFile(file)
        }
    }
    
    fun createExportData(stats: MiningStats): ExportData {
        return ExportData(
            timestamp = System.currentTimeMillis(),
            hashrate = stats.hashrate,
            cpuTemp = stats.cpuTemp,
            cpuUsage = stats.cpuUsage,
            acceptedShares = stats.acceptedShares,
            rejectedShares = stats.rejectedShares,
            uptime = stats.uptime,
            powerUsage = stats.powerUsage,
            earnings = stats.earnings
        )
    }
}
