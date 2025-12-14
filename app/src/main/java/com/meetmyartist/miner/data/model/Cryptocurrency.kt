package com.meetmyartist.miner.data.model

data class Cryptocurrency(
    val symbol: String,
    val name: String,
    val algorithm: MiningAlgorithm,
    val minableUnit: String, // Smallest unit (e.g., "satoshi" for Bitcoin, "wei" for Ethereum)
    val unitsPerCoin: Long, // How many smallest units in one coin
    val currentPrice: Double = 0.0, // Price per coin in USD
    val networkDifficulty: Double = 0.0,
    val blockReward: Double = 0.0,
    val isEnabled: Boolean = true
)

enum class MiningAlgorithm(val displayName: String, val description: String) {
    SHA256("SHA-256", "Bitcoin's Proof-of-Work algorithm"),
    SCRYPT("Scrypt", "Memory-hard algorithm for Litecoin"),
    ETHASH("Ethash", "Ethereum's memory-hard algorithm"),
    RANDOMX("RandomX", "CPU-optimized for Monero"),
    KAWPOW("KawPoW", "Ravencoin's ASIC-resistant algorithm"),
    ETCHASH("Etchash", "Ethereum Classic's algorithm"),
    CUCKATOO32("Cuckatoo32", "Grin's graph-based algorithm"),
    PROGPOW("ProgPow", "Programmatic Proof-of-Work"),
    X11("X11", "Dash's chained hashing algorithm"),
    EQUIHASH("Equihash", "Zcash's memory-oriented algorithm"),
    CRYPTONIGHT("CryptoNight", "Privacy-focused algorithm"),
    BLAKE2S("Blake2s", "Fast and secure hashing"),
    BLAKE3("Blake3", "Next-gen Blake algorithm"),
    XELIS("Xelis", "Xelis blockchain algorithm"),
    THETA_EDGE("Theta Edge", "Theta network edge computing"),
    HELIUM("Helium", "IoT network proof-of-coverage"),
    CHIA("Chia", "Proof of space and time"),
    VERTCOIN("Vertcoin", "One-click mining algorithm"),
    KASPA("Kaspa", "BlockDAG architecture"),
    FLUX("Flux", "Decentralized cloud infrastructure"),
    ERGO("Ergo", "Autolykos v2 algorithm"),
    CONFLUX("Conflux", "Tree-graph consensus"),
    ALEPHIUM("Alephium", "Sharded blockchain algorithm"),
    NERVOS("Nervos", "Eaglesong algorithm")
}

object CryptocurrencyDefaults {
    fun getAllSupportedCryptos(): List<Cryptocurrency> = listOf(
        // Major cryptocurrencies
        Cryptocurrency(
            symbol = "BTC",
            name = "Bitcoin",
            algorithm = MiningAlgorithm.SHA256,
            minableUnit = "satoshi",
            unitsPerCoin = 100_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "ETH",
            name = "Ethereum",
            algorithm = MiningAlgorithm.ETHASH,
            minableUnit = "wei",
            unitsPerCoin = 1_000_000_000_000_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "XMR",
            name = "Monero",
            algorithm = MiningAlgorithm.RANDOMX,
            minableUnit = "piconero",
            unitsPerCoin = 1_000_000_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "LTC",
            name = "Litecoin",
            algorithm = MiningAlgorithm.SCRYPT,
            minableUnit = "litoshi",
            unitsPerCoin = 100_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "ETC",
            name = "Ethereum Classic",
            algorithm = MiningAlgorithm.ETCHASH,
            minableUnit = "wei",
            unitsPerCoin = 1_000_000_000_000_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "RVN",
            name = "Ravencoin",
            algorithm = MiningAlgorithm.KAWPOW,
            minableUnit = "raven",
            unitsPerCoin = 100_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "DASH",
            name = "Dash",
            algorithm = MiningAlgorithm.X11,
            minableUnit = "duff",
            unitsPerCoin = 100_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "ZEC",
            name = "Zcash",
            algorithm = MiningAlgorithm.EQUIHASH,
            minableUnit = "zatoshi",
            unitsPerCoin = 100_000_000L,
            currentPrice = 0.0
        ),
        
        // Theta and Edge Computing
        Cryptocurrency(
            symbol = "THETA",
            name = "Theta Network",
            algorithm = MiningAlgorithm.THETA_EDGE,
            minableUnit = "thetawei",
            unitsPerCoin = 1_000_000_000_000_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "TFUEL",
            name = "Theta Fuel",
            algorithm = MiningAlgorithm.THETA_EDGE,
            minableUnit = "tfuelwei",
            unitsPerCoin = 1_000_000_000_000_000_000L,
            currentPrice = 0.0
        ),
        
        // CPU-Friendly Coins
        Cryptocurrency(
            symbol = "GRIN",
            name = "Grin",
            algorithm = MiningAlgorithm.CUCKATOO32,
            minableUnit = "grin",
            unitsPerCoin = 1_000_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "VTC",
            name = "Vertcoin",
            algorithm = MiningAlgorithm.VERTCOIN,
            minableUnit = "vertoshi",
            unitsPerCoin = 100_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "ERG",
            name = "Ergo",
            algorithm = MiningAlgorithm.ERGO,
            minableUnit = "nanoerg",
            unitsPerCoin = 1_000_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "FLUX",
            name = "Flux",
            algorithm = MiningAlgorithm.FLUX,
            minableUnit = "flux",
            unitsPerCoin = 100_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "KAS",
            name = "Kaspa",
            algorithm = MiningAlgorithm.KASPA,
            minableUnit = "sompi",
            unitsPerCoin = 100_000_000L,
            currentPrice = 0.0
        ),
        
        // Alternative Coins
        Cryptocurrency(
            symbol = "XCH",
            name = "Chia",
            algorithm = MiningAlgorithm.CHIA,
            minableUnit = "mojo",
            unitsPerCoin = 1_000_000_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "HNT",
            name = "Helium",
            algorithm = MiningAlgorithm.HELIUM,
            minableUnit = "bone",
            unitsPerCoin = 100_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "CFX",
            name = "Conflux",
            algorithm = MiningAlgorithm.CONFLUX,
            minableUnit = "drip",
            unitsPerCoin = 1_000_000_000_000_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "ALPH",
            name = "Alephium",
            algorithm = MiningAlgorithm.ALEPHIUM,
            minableUnit = "alph",
            unitsPerCoin = 1_000_000_000_000_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "CKB",
            name = "Nervos Network",
            algorithm = MiningAlgorithm.NERVOS,
            minableUnit = "shannon",
            unitsPerCoin = 100_000_000L,
            currentPrice = 0.0
        ),
        
        // More Altcoins
        Cryptocurrency(
            symbol = "XEL",
            name = "Xelis",
            algorithm = MiningAlgorithm.XELIS,
            minableUnit = "xel",
            unitsPerCoin = 100_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "BEAM",
            name = "Beam",
            algorithm = MiningAlgorithm.EQUIHASH,
            minableUnit = "groth",
            unitsPerCoin = 100_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "FIRO",
            name = "Firo",
            algorithm = MiningAlgorithm.PROGPOW,
            minableUnit = "satang",
            unitsPerCoin = 100_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "RTM",
            name = "Raptoreum",
            algorithm = MiningAlgorithm.CRYPTONIGHT,
            minableUnit = "rtm",
            unitsPerCoin = 100_000_000L,
            currentPrice = 0.0
        ),
        Cryptocurrency(
            symbol = "DERO",
            name = "Dero",
            algorithm = MiningAlgorithm.BLAKE3,
            minableUnit = "dero",
            unitsPerCoin = 100_000L,
            currentPrice = 0.0
        )
    )
    
    fun getCryptoBySymbol(symbol: String): Cryptocurrency? {
        return getAllSupportedCryptos().find { it.symbol == symbol }
    }
    
    fun getCryptosByAlgorithm(algorithm: MiningAlgorithm): List<Cryptocurrency> {
        return getAllSupportedCryptos().filter { it.algorithm == algorithm }
    }
    
    fun getCPUFriendlyCoins(): List<Cryptocurrency> {
        val cpuFriendlyAlgorithms = listOf(
            MiningAlgorithm.RANDOMX,
            MiningAlgorithm.CUCKATOO32,
            MiningAlgorithm.VERTCOIN,
            MiningAlgorithm.ERGO,
            MiningAlgorithm.KASPA,
            MiningAlgorithm.CRYPTONIGHT,
            MiningAlgorithm.BLAKE3
        )
        return getAllSupportedCryptos().filter { it.algorithm in cpuFriendlyAlgorithms }
    }
}
