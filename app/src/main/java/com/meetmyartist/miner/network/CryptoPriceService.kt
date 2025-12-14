package com.meetmyartist.miner.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to fetch cryptocurrency prices from public APIs
 * Uses CoinGecko API (free tier, no API key required)
 */
@Singleton
class CryptoPriceService @Inject constructor() {
    
    companion object {
        private const val COINGECKO_API = "https://api.coingecko.com/api/v3"
        private const val TIMEOUT = 10000 // 10 seconds
    }
    
    /**
     * Map of cryptocurrency symbols to CoinGecko IDs
     */
    private val coinGeckoIds = mapOf(
        "BTC" to "bitcoin",
        "ETH" to "ethereum",
        "LTC" to "litecoin",
        "XMR" to "monero",
        "ZEC" to "zcash",
        "RVN" to "ravencoin",
        "ETC" to "ethereum-classic",
        "DOGE" to "dogecoin",
        "BCH" to "bitcoin-cash"
    )
    
    /**
     * Fetch price for a single cryptocurrency
     */
    suspend fun fetchPrice(symbol: String): PriceData? = withContext(Dispatchers.IO) {
        try {
            val coinId = coinGeckoIds[symbol.uppercase()] ?: return@withContext null
            
            val url = URL("$COINGECKO_API/simple/price?ids=$coinId&vs_currencies=usd&include_24hr_change=true&include_market_cap=true&include_24hr_vol=true")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                setRequestProperty("Accept", "application/json")
            }
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                if (json.has(coinId)) {
                    val coinData = json.getJSONObject(coinId)
                    
                    return@withContext PriceData(
                        symbol = symbol.uppercase(),
                        name = coinId.replaceFirstChar { it.uppercase() },
                        priceUsd = coinData.optDouble("usd", 0.0),
                        priceChange24h = coinData.optDouble("usd_24h_change", 0.0),
                        marketCapUsd = coinData.optDouble("usd_market_cap", 0.0),
                        volume24h = coinData.optDouble("usd_24h_vol", 0.0)
                    )
                }
            }
            
            connection.disconnect()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Fetch prices for multiple cryptocurrencies
     */
    suspend fun fetchPrices(symbols: List<String>): List<PriceData> = withContext(Dispatchers.IO) {
        try {
            val coinIds = symbols.mapNotNull { coinGeckoIds[it.uppercase()] }.joinToString(",")
            if (coinIds.isEmpty()) return@withContext emptyList()
            
            val url = URL("$COINGECKO_API/simple/price?ids=$coinIds&vs_currencies=usd&include_24hr_change=true&include_market_cap=true&include_24hr_vol=true")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                setRequestProperty("Accept", "application/json")
            }
            
            val prices = mutableListOf<PriceData>()
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                symbols.forEach { symbol ->
                    val coinId = coinGeckoIds[symbol.uppercase()] ?: return@forEach
                    if (json.has(coinId)) {
                        val coinData = json.getJSONObject(coinId)
                        
                        prices.add(
                            PriceData(
                                symbol = symbol.uppercase(),
                                name = coinId.replaceFirstChar { it.uppercase() },
                                priceUsd = coinData.optDouble("usd", 0.0),
                                priceChange24h = coinData.optDouble("usd_24h_change", 0.0),
                                marketCapUsd = coinData.optDouble("usd_market_cap", 0.0),
                                volume24h = coinData.optDouble("usd_24h_vol", 0.0)
                            )
                        )
                    }
                }
            }
            
            connection.disconnect()
            prices
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Get detailed coin information
     */
    suspend fun getCoinInfo(symbol: String): CoinInfo? = withContext(Dispatchers.IO) {
        try {
            val coinId = coinGeckoIds[symbol.uppercase()] ?: return@withContext null
            
            val url = URL("$COINGECKO_API/coins/$coinId?localization=false&tickers=false&community_data=false&developer_data=false")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                setRequestProperty("Accept", "application/json")
            }
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                val marketData = json.optJSONObject("market_data")
                
                return@withContext CoinInfo(
                    symbol = symbol.uppercase(),
                    name = json.optString("name", ""),
                    description = json.optJSONObject("description")?.optString("en", ""),
                    currentPrice = marketData?.optJSONObject("current_price")?.optDouble("usd", 0.0) ?: 0.0,
                    marketCap = marketData?.optJSONObject("market_cap")?.optDouble("usd", 0.0) ?: 0.0,
                    totalVolume = marketData?.optJSONObject("total_volume")?.optDouble("usd", 0.0) ?: 0.0,
                    high24h = marketData?.optJSONObject("high_24h")?.optDouble("usd", 0.0) ?: 0.0,
                    low24h = marketData?.optJSONObject("low_24h")?.optDouble("usd", 0.0) ?: 0.0,
                    priceChange24h = marketData?.optDouble("price_change_percentage_24h", 0.0) ?: 0.0,
                    circulatingSupply = marketData?.optDouble("circulating_supply", 0.0) ?: 0.0,
                    totalSupply = marketData?.optDouble("total_supply", 0.0) ?: 0.0
                )
            }
            
            connection.disconnect()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    data class PriceData(
        val symbol: String,
        val name: String,
        val priceUsd: Double,
        val priceChange24h: Double,
        val marketCapUsd: Double,
        val volume24h: Double
    )
    
    data class CoinInfo(
        val symbol: String,
        val name: String,
        val description: String?,
        val currentPrice: Double,
        val marketCap: Double,
        val totalVolume: Double,
        val high24h: Double,
        val low24h: Double,
        val priceChange24h: Double,
        val circulatingSupply: Double,
        val totalSupply: Double
    )
}
