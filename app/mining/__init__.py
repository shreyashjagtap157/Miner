"""
Mining pool failover and management module.
"""

from mining.pool_failover import (
    MiningPoolManager,
    PoolFailover,
    PoolHealthMonitor,
    DifficultyScaler,
    PoolStats,
    PoolStatus,
)

__all__ = [
    "MiningPoolManager",
    "PoolFailover",
    "PoolHealthMonitor",
    "DifficultyScaler",
    "PoolStats",
    "PoolStatus",
]
