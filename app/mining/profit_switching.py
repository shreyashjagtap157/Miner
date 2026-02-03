"""
Profit Switching Algorithm for Mining

Dynamically switches between mining algorithms based on profitability.

Features:
- Real-time profitability calculation
- Network difficulty tracking
- Power consumption modeling
- Automatic algorithm switching
- Statistics tracking
- Profit prediction
"""

import logging
import asyncio
from typing import Dict, List, Optional, Tuple, Any
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from enum import Enum
import numpy as np

logger = logging.getLogger(__name__)


class AlgorithmType(Enum):
    """Mining algorithms"""
    SHA256D = "sha256d"  # Bitcoin
    ETHASH = "ethash"    # Ethereum
    RANDOMX = "randomx"  # Monero
    KAWPOW = "kawpow"    # Ravencoin
    SCRYPT = "scrypt"    # Litecoin
    CRYPTONIGHT = "cryptonight"  # Various coins


@dataclass
class CoinInfo:
    """Information about a coin"""
    coin_id: str
    name: str
    symbol: str
    algorithm: AlgorithmType
    
    current_price: float = 0.0  # USD per coin
    block_reward: float = 0.0
    network_difficulty: float = 1.0
    block_time_seconds: float = 600.0
    
    last_updated: datetime = field(default_factory=datetime.utcnow)


@dataclass
class MiningHardware:
    """Mining hardware specifications"""
    hardware_id: str
    name: str
    algorithm: AlgorithmType
    
    hashrate: float  # MH/s for GPU, H/s for CPU
    power_consumption: float  # Watts
    efficiency: float = 0.0  # MH/W or H/W
    
    # Temperature and throttling
    current_temp: float = 0.0
    max_safe_temp: float = 80.0
    throttling_percent: float = 0.0
    
    metadata: Dict[str, Any] = field(default_factory=dict)


@dataclass
class ProfitabilityMetrics:
    """Profitability metrics for an algorithm"""
    algorithm: AlgorithmType
    coin_id: str
    
    # Revenue
    coins_per_day: float
    revenue_per_day_usd: float
    revenue_per_hour_usd: float
    
    # Costs
    power_cost_per_day_usd: float
    power_cost_per_hour_usd: float
    
    # Net profit
    net_profit_per_day_usd: float
    net_profit_per_hour_usd: float
    
    # Efficiency metrics
    profit_per_hash: float  # USD per GH/s
    roi_days: float
    efficiency_score: float  # 0 to 100
    
    # Market conditions
    difficulty_trend: str = "stable"  # increasing, stable, decreasing
    price_trend: str = "stable"
    
    timestamp: datetime = field(default_factory=datetime.utcnow)


class ProfitabilityCalculator:
    """
    Calculates mining profitability.
    """
    
    def __init__(self, electricity_cost_per_kwh: float = 0.12):
        self.electricity_cost_per_kwh = electricity_cost_per_kwh
        self.difficulty_history: Dict[str, List[Tuple[datetime, float]]] = {}
        self.price_history: Dict[str, List[Tuple[datetime, float]]] = {}
    
    def record_difficulty(self, coin_id: str, difficulty: float):
        """Record difficulty"""
        if coin_id not in self.difficulty_history:
            self.difficulty_history[coin_id] = []
        
        self.difficulty_history[coin_id].append((datetime.utcnow(), difficulty))
        
        # Keep only recent data
        if len(self.difficulty_history[coin_id]) > 10000:
            self.difficulty_history[coin_id].pop(0)
    
    def record_price(self, coin_id: str, price: float):
        """Record price"""
        if coin_id not in self.price_history:
            self.price_history[coin_id] = []
        
        self.price_history[coin_id].append((datetime.utcnow(), price))
        
        if len(self.price_history[coin_id]) > 10000:
            self.price_history[coin_id].pop(0)
    
    def calculate_profitability(
        self,
        coin: CoinInfo,
        hardware: MiningHardware
    ) -> Optional[ProfitabilityMetrics]:
        """
        Calculate profitability for mining a coin.
        """
        if coin.algorithm != hardware.algorithm:
            return None
        
        # Adjust hashrate for throttling
        effective_hashrate = hardware.hashrate * (1 - hardware.throttling_percent / 100)
        
        if effective_hashrate <= 0 or coin.network_difficulty <= 0:
            return None
        
        # Coins per day
        # Formula: (hashrate / difficulty) * block_reward * (86400 / block_time)
        hashrate_per_second = effective_hashrate * 1e6  # Convert MH/s to H/s
        blocks_per_day = 86400 / coin.block_time_seconds
        coins_per_day = (
            (hashrate_per_second / coin.network_difficulty) *
            coin.block_reward *
            blocks_per_day
        )
        
        revenue_per_day = coins_per_day * coin.current_price
        revenue_per_hour = revenue_per_day / 24
        
        # Power cost
        power_consumption_kw = hardware.power_consumption / 1000
        power_cost_per_day = power_consumption_kw * 24 * self.electricity_cost_per_kwh
        power_cost_per_hour = power_cost_per_day / 24
        
        # Net profit
        net_profit_per_day = revenue_per_day - power_cost_per_day
        net_profit_per_hour = revenue_per_hour - power_cost_per_hour
        
        # Efficiency
        profit_per_hash = net_profit_per_day / (hashrate_per_second * 86400) if hashrate_per_second > 0 else 0
        roi_days = (hardware.power_consumption * 1.5) / (net_profit_per_day + 0.01)  # Simplified ROI
        
        # Efficiency score (0-100)
        efficiency_score = min(100, max(0, (net_profit_per_hour / 10) * 100))
        
        # Trends
        difficulty_trend = self._get_trend(
            self.difficulty_history.get(coin.coin_id, [])
        )
        price_trend = self._get_trend(
            self.price_history.get(coin.coin_id, [])
        )
        
        return ProfitabilityMetrics(
            algorithm=coin.algorithm,
            coin_id=coin.coin_id,
            coins_per_day=coins_per_day,
            revenue_per_day_usd=revenue_per_day,
            revenue_per_hour_usd=revenue_per_hour,
            power_cost_per_day_usd=power_cost_per_day,
            power_cost_per_hour_usd=power_cost_per_hour,
            net_profit_per_day_usd=net_profit_per_day,
            net_profit_per_hour_usd=net_profit_per_hour,
            profit_per_hash=profit_per_hash,
            roi_days=roi_days,
            efficiency_score=efficiency_score,
            difficulty_trend=difficulty_trend,
            price_trend=price_trend
        )
    
    def _get_trend(self, history: List[Tuple[datetime, float]]) -> str:
        """Determine trend from history"""
        if len(history) < 5:
            return "stable"
        
        values = [v for _, v in history[-5:]]
        early_avg = np.mean(values[:2])
        recent_avg = np.mean(values[-2:])
        
        change_pct = (recent_avg - early_avg) / early_avg * 100 if early_avg > 0 else 0
        
        if change_pct > 5:
            return "increasing"
        elif change_pct < -5:
            return "decreasing"
        else:
            return "stable"


class ProfitSwitcher:
    """
    Automatically switches mining algorithm based on profitability.
    """
    
    def __init__(
        self,
        electricity_cost_per_kwh: float = 0.12,
        switch_threshold_percent: float = 5.0,
        update_interval_seconds: float = 300.0
    ):
        self.profitability_calc = ProfitabilityCalculator(electricity_cost_per_kwh)
        self.switch_threshold = switch_threshold_percent
        self.update_interval = update_interval_seconds
        
        self.coins: Dict[str, CoinInfo] = {}
        self.hardware: Dict[str, MiningHardware] = {}
        
        self.current_algorithm: Optional[AlgorithmType] = None
        self.current_coin: Optional[str] = None
        
        self.switch_history: List[Dict[str, Any]] = []
        self.profitability_cache: Dict[Tuple[str, str], ProfitabilityMetrics] = {}
    
    def add_coin(self, coin: CoinInfo):
        """Add coin to monitor"""
        self.coins[coin.coin_id] = coin
    
    def add_hardware(self, hardware: MiningHardware):
        """Add mining hardware"""
        self.hardware[hardware.hardware_id] = hardware
    
    def update_coin_data(
        self,
        coin_id: str,
        price: float,
        network_difficulty: float
    ):
        """Update coin price and difficulty"""
        if coin_id in self.coins:
            self.coins[coin_id].current_price = price
            self.coins[coin_id].network_difficulty = network_difficulty
            self.coins[coin_id].last_updated = datetime.utcnow()
            
            # Record for history
            self.profitability_calc.record_price(coin_id, price)
            self.profitability_calc.record_difficulty(coin_id, network_difficulty)
    
    def calculate_best_algorithm(self) -> Tuple[Optional[AlgorithmType], Optional[str]]:
        """
        Calculate most profitable algorithm to mine.
        
        Returns: (algorithm, coin_id)
        """
        best_profit = float('-inf')
        best_algo = None
        best_coin = None
        
        # Check each combination of hardware and coin
        for hw_id, hardware in self.hardware.items():
            for coin_id, coin in self.coins.items():
                if coin.algorithm != hardware.algorithm:
                    continue
                
                metrics = self.profitability_calc.calculate_profitability(coin, hardware)
                
                if metrics and metrics.net_profit_per_hour > best_profit:
                    best_profit = metrics.net_profit_per_hour
                    best_algo = coin.algorithm
                    best_coin = coin_id
        
        return best_algo, best_coin
    
    def should_switch(
        self,
        new_algorithm: Optional[AlgorithmType],
        new_coin: Optional[str]
    ) -> bool:
        """
        Determine if switch is warranted.
        """
        if not self.current_algorithm or not self.current_coin:
            return True
        
        if new_algorithm is None or new_coin is None:
            return False
        
        # Get current profitability
        current_metrics = self.profitability_calc.calculate_profitability(
            self.coins[self.current_coin],
            list(self.hardware.values())[0]  # Simplified: use first hardware
        )
        
        # Get new profitability
        new_metrics = self.profitability_calc.calculate_profitability(
            self.coins[new_coin],
            list(self.hardware.values())[0]
        )
        
        if not current_metrics or not new_metrics:
            return False
        
        # Calculate improvement percentage
        improvement = (
            (new_metrics.net_profit_per_hour - current_metrics.net_profit_per_hour) /
            (abs(current_metrics.net_profit_per_hour) + 0.01) * 100
        )
        
        return improvement > self.switch_threshold
    
    async def run_switching_loop(self):
        """
        Run profit switching algorithm.
        """
        while True:
            try:
                # Calculate best algorithm
                best_algo, best_coin = self.calculate_best_algorithm()
                
                # Check if switch is needed
                if self.should_switch(best_algo, best_coin):
                    await self._perform_switch(best_algo, best_coin)
                
                await asyncio.sleep(self.update_interval)
                
            except Exception as e:
                logger.error(f"Switching loop error: {e}")
                await asyncio.sleep(5)
    
    async def _perform_switch(
        self,
        algorithm: Optional[AlgorithmType],
        coin_id: Optional[str]
    ):
        """
        Perform switch to new algorithm/coin.
        """
        old_algo = self.current_algorithm
        old_coin = self.current_coin
        
        self.current_algorithm = algorithm
        self.current_coin = coin_id
        
        logger.info(
            f"Switching from {old_algo}/{old_coin} to {algorithm}/{coin_id}"
        )
        
        # Record switch
        self.switch_history.append({
            "timestamp": datetime.utcnow(),
            "from_algorithm": old_algo.value if old_algo else None,
            "from_coin": old_coin,
            "to_algorithm": algorithm.value if algorithm else None,
            "to_coin": coin_id,
            "reason": "profit_optimization"
        })
    
    def get_profitability_report(self) -> Dict[str, Any]:
        """
        Get detailed profitability report for all coins.
        """
        report = {
            "timestamp": datetime.utcnow().isoformat(),
            "current_algorithm": self.current_algorithm.value if self.current_algorithm else None,
            "current_coin": self.current_coin,
            "coins": {}
        }
        
        hardware = list(self.hardware.values())[0] if self.hardware else None
        
        for coin_id, coin in self.coins.items():
            if not hardware:
                continue
            
            metrics = self.profitability_calc.calculate_profitability(coin, hardware)
            
            if metrics:
                report["coins"][coin_id] = {
                    "coin_name": coin.name,
                    "algorithm": metrics.algorithm.value,
                    "revenue_per_day_usd": float(metrics.revenue_per_day_usd),
                    "power_cost_per_day_usd": float(metrics.power_cost_per_day_usd),
                    "net_profit_per_day_usd": float(metrics.net_profit_per_day_usd),
                    "net_profit_per_hour_usd": float(metrics.net_profit_per_hour_usd),
                    "efficiency_score": float(metrics.efficiency_score),
                    "difficulty_trend": metrics.difficulty_trend,
                    "price_trend": metrics.price_trend
                }
        
        return report
    
    def get_switch_history(self, limit: int = 50) -> List[Dict[str, Any]]:
        """Get recent switch history"""
        return [
            {
                "timestamp": s["timestamp"].isoformat(),
                "from_algorithm": s["from_algorithm"],
                "from_coin": s["from_coin"],
                "to_algorithm": s["to_algorithm"],
                "to_coin": s["to_coin"],
                "reason": s["reason"]
            }
            for s in self.switch_history[-limit:]
        ]
