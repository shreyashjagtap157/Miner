"""
Mining Pool Failover System

Multi-pool support with health checks, automatic failover, and difficulty scaling.

Features:
- Multiple pool management
- Health monitoring
- Automatic failover
- Difficulty scaling
- Share tracking per pool
- Profit switching
"""

import logging
import asyncio
import json
from typing import Dict, List, Optional, Tuple, Any
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from enum import Enum
import hashlib
import aiohttp

logger = logging.getLogger(__name__)


class PoolStatus(Enum):
    """Pool connection status"""
    CONNECTED = "connected"
    CONNECTING = "connecting"
    DISCONNECTED = "disconnected"
    UNHEALTHY = "unhealthy"
    BANNED = "banned"


@dataclass
class PoolStats:
    """Statistics for a mining pool"""
    pool_id: str
    name: str
    url: str
    port: int
    username: str
    
    status: PoolStatus = PoolStatus.DISCONNECTED
    connected_time: Optional[datetime] = None
    last_heartbeat: Optional[datetime] = None
    
    # Mining statistics
    shares_accepted: int = 0
    shares_rejected: int = 0
    shares_stale: int = 0
    difficulty: float = 1.0
    
    # Performance metrics
    hashrate: float = 0.0  # MH/s
    uptime_pct: float = 100.0
    latency_ms: float = 0.0
    last_block_time: Optional[datetime] = None
    
    # Connection metrics
    reconnect_attempts: int = 0
    connection_errors: int = 0
    
    metadata: Dict[str, Any] = field(default_factory=dict)


@dataclass
class Stratum:
    """Stratum protocol connection"""
    pool_id: str
    hostname: str
    port: int
    worker_name: str
    worker_password: str
    
    reader: Optional[asyncio.StreamReader] = None
    writer: Optional[asyncio.StreamWriter] = None
    connected: bool = False
    subscription_id: Optional[str] = None
    notify_id: Optional[str] = None
    
    request_id: int = 1


class PoolHealthMonitor:
    """
    Monitors health of mining pools.
    """
    
    def __init__(
        self,
        heartbeat_interval: float = 30.0,
        timeout_threshold: int = 3,
        health_check_timeout: float = 10.0
    ):
        self.heartbeat_interval = heartbeat_interval
        self.timeout_threshold = timeout_threshold
        self.health_check_timeout = health_check_timeout
        
        self.pool_stats: Dict[str, PoolStats] = {}
        self.health_checks: Dict[str, List[datetime]] = {}
    
    def register_pool(self, stats: PoolStats):
        """Register a pool for monitoring"""
        self.pool_stats[stats.pool_id] = stats
        self.health_checks[stats.pool_id] = []
    
    async def perform_health_check(
        self,
        pool_id: str,
        stratum: Stratum
    ) -> Tuple[bool, float]:
        """
        Perform health check on pool.
        
        Returns: (is_healthy, latency_ms)
        """
        if pool_id not in self.pool_stats:
            return False, 0.0
        
        stats = self.pool_stats[pool_id]
        
        try:
            # Send mining.get_version RPC
            start_time = asyncio.get_event_loop().time()
            
            if stratum.connected:
                # Connection is open
                is_healthy = True
                latency = (asyncio.get_event_loop().time() - start_time) * 1000
            else:
                # Try to connect
                try:
                    reader, writer = await asyncio.wait_for(
                        asyncio.open_connection(stratum.hostname, stratum.port),
                        timeout=self.health_check_timeout
                    )
                    
                    is_healthy = True
                    latency = (asyncio.get_event_loop().time() - start_time) * 1000
                    
                    # Close test connection
                    writer.close()
                    await writer.wait_closed()
                    
                except asyncio.TimeoutError:
                    is_healthy = False
                    latency = self.health_check_timeout * 1000
            
            # Update stats
            stats.latency_ms = latency
            stats.last_heartbeat = datetime.utcnow()
            self.health_checks[pool_id].append(datetime.utcnow())
            
            # Calculate uptime
            self._update_uptime(pool_id)
            
            return is_healthy, latency
            
        except Exception as e:
            logger.error(f"Health check failed for {pool_id}: {e}")
            stats.connection_errors += 1
            return False, 0.0
    
    def _update_uptime(self, pool_id: str, lookback_hours: int = 24):
        """Calculate uptime percentage"""
        if pool_id not in self.health_checks:
            return
        
        checks = self.health_checks[pool_id]
        cutoff = datetime.utcnow() - timedelta(hours=lookback_hours)
        recent_checks = [c for c in checks if c > cutoff]
        
        if not recent_checks:
            return
        
        # Assume each check interval indicates health
        stats = self.pool_stats[pool_id]
        successful = len([c for c in recent_checks if stats.status == PoolStatus.CONNECTED])
        stats.uptime_pct = (successful / len(recent_checks) * 100) if recent_checks else 0.0
    
    async def monitor_loop(self, stratum: Stratum):
        """Continuously monitor pool health"""
        while True:
            try:
                is_healthy, latency = await self.perform_health_check(
                    stratum.pool_id,
                    stratum
                )
                
                stats = self.pool_stats[stratum.pool_id]
                
                if is_healthy:
                    stats.status = PoolStatus.CONNECTED
                    stats.reconnect_attempts = 0
                else:
                    if stats.status != PoolStatus.UNHEALTHY:
                        stats.status = PoolStatus.UNHEALTHY
                    stats.reconnect_attempts += 1
                
                await asyncio.sleep(self.heartbeat_interval)
                
            except Exception as e:
                logger.error(f"Monitor loop error: {e}")
                await asyncio.sleep(5)


class DifficultyScaler:
    """
    Automatically scales difficulty based on hash rate.
    """
    
    def __init__(
        self,
        target_block_time_seconds: float = 30.0,
        adjustment_interval: int = 100
    ):
        self.target_block_time = target_block_time_seconds
        self.adjustment_interval = adjustment_interval
        
        self.block_times: List[float] = []
        self.difficulties: List[float] = []
    
    def record_block_time(self, block_time: float, current_difficulty: float):
        """Record block time and difficulty"""
        self.block_times.append(block_time)
        self.difficulties.append(current_difficulty)
        
        # Keep only recent data
        if len(self.block_times) > 1000:
            self.block_times.pop(0)
            self.difficulties.pop(0)
    
    def calculate_next_difficulty(self) -> Optional[float]:
        """Calculate next difficulty target"""
        if len(self.block_times) < self.adjustment_interval:
            return None
        
        # Average of last N block times
        recent_times = self.block_times[-self.adjustment_interval:]
        avg_time = sum(recent_times) / len(recent_times)
        current_diff = self.difficulties[-1]
        
        # Adjust difficulty
        # If blocks are too fast, increase difficulty
        # If blocks are too slow, decrease difficulty
        ratio = avg_time / self.target_block_time
        
        # Use smooth adjustment (max 4x per adjustment)
        if ratio > 1.0:  # Blocks too slow
            adjustment = min(ratio, 4.0)
        else:  # Blocks too fast
            adjustment = max(ratio, 0.25)
        
        new_difficulty = current_diff * adjustment
        
        return new_difficulty
    
    def should_adjust(self) -> bool:
        """Check if difficulty adjustment is needed"""
        return len(self.block_times) >= self.adjustment_interval


class PoolFailover:
    """
    Manages pool failover and selection.
    """
    
    def __init__(self):
        self.pools: Dict[str, PoolStats] = {}
        self.primary_pool: Optional[str] = None
        self.current_pool: Optional[str] = None
        self.failover_history: List[Dict[str, Any]] = []
    
    def add_pool(
        self,
        pool_id: str,
        name: str,
        url: str,
        port: int,
        username: str,
        priority: int = 0,
        backup: bool = False
    ) -> PoolStats:
        """Add mining pool"""
        stats = PoolStats(
            pool_id=pool_id,
            name=name,
            url=url,
            port=port,
            username=username,
            metadata={"priority": priority, "backup": backup}
        )
        
        self.pools[pool_id] = stats
        
        if not backup and (self.primary_pool is None or priority > self.pools[self.primary_pool].metadata["priority"]):
            self.primary_pool = pool_id
        
        return stats
    
    def select_best_pool(self) -> Optional[str]:
        """
        Select best performing pool.
        Priority: healthy status > uptime > low latency > high hashrate
        """
        if not self.pools:
            return None
        
        candidates = []
        
        for pool_id, stats in self.pools.items():
            # Filter out banned and disconnected pools
            if stats.status in [PoolStatus.BANNED, PoolStatus.DISCONNECTED]:
                continue
            
            # Score pools
            score = 0.0
            
            # Status (healthy pools get 100 points)
            if stats.status == PoolStatus.CONNECTED:
                score += 100
            elif stats.status == PoolStatus.CONNECTING:
                score += 50
            else:
                score += 0
            
            # Uptime (max 50 points)
            score += (stats.uptime_pct / 100) * 50
            
            # Latency (lower is better, max 30 points)
            if stats.latency_ms > 0:
                score += max(0, 30 - (stats.latency_ms / 10))
            
            # Hashrate (higher is better, max 20 points)
            score += min(stats.hashrate / 1000, 20)
            
            candidates.append((pool_id, score))
        
        if not candidates:
            return None
        
        # Select highest scoring pool
        best_pool = max(candidates, key=lambda x: x[1])
        return best_pool[0]
    
    async def attempt_failover(self) -> bool:
        """
        Attempt to switch to better pool.
        """
        current = self.current_pool
        best = self.select_best_pool()
        
        if best and best != current:
            logger.info(f"Failing over from {current} to {best}")
            
            self.failover_history.append({
                "timestamp": datetime.utcnow(),
                "from_pool": current,
                "to_pool": best,
                "reason": "better_performance"
            })
            
            self.current_pool = best
            return True
        
        return False
    
    def get_pool_stats(self, pool_id: str) -> Optional[PoolStats]:
        """Get statistics for pool"""
        return self.pools.get(pool_id)
    
    def get_all_stats(self) -> Dict[str, Dict[str, Any]]:
        """Get stats for all pools"""
        return {
            pool_id: {
                "name": stats.name,
                "status": stats.status.value,
                "shares_accepted": stats.shares_accepted,
                "shares_rejected": stats.shares_rejected,
                "shares_stale": stats.shares_stale,
                "difficulty": stats.difficulty,
                "hashrate": stats.hashrate,
                "uptime_pct": stats.uptime_pct,
                "latency_ms": stats.latency_ms,
                "reconnect_attempts": stats.reconnect_attempts
            }
            for pool_id, stats in self.pools.items()
        }


class MiningPoolManager:
    """
    Coordinates mining pool management and failover.
    """
    
    def __init__(self):
        self.failover = PoolFailover()
        self.health_monitor = PoolHealthMonitor()
        self.difficulty_scaler = DifficultyScaler()
        
        self.stratum_connections: Dict[str, Stratum] = {}
        self.monitor_tasks: Dict[str, asyncio.Task] = {}
    
    def add_pool(
        self,
        pool_id: str,
        name: str,
        url: str,
        port: int,
        username: str,
        password: str = "",
        priority: int = 0
    ) -> PoolStats:
        """Add mining pool"""
        stats = self.failover.add_pool(pool_id, name, url, port, username, priority)
        self.health_monitor.register_pool(stats)
        
        # Create Stratum connection
        stratum = Stratum(
            pool_id=pool_id,
            hostname=url,
            port=port,
            worker_name=username,
            worker_password=password
        )
        self.stratum_connections[pool_id] = stratum
        
        return stats
    
    async def start_monitoring(self):
        """Start health monitoring for all pools"""
        for pool_id, stratum in self.stratum_connections.items():
            task = asyncio.create_task(
                self.health_monitor.monitor_loop(stratum)
            )
            self.monitor_tasks[pool_id] = task
    
    async def failover_loop(self, check_interval: float = 60.0):
        """Periodically check if failover is needed"""
        while True:
            try:
                success = await self.failover.attempt_failover()
                if success:
                    logger.info("Failover completed")
                
                await asyncio.sleep(check_interval)
                
            except Exception as e:
                logger.error(f"Failover loop error: {e}")
                await asyncio.sleep(5)
    
    def record_share(self, pool_id: str, accepted: bool, stale: bool = False):
        """Record share submission"""
        stats = self.failover.get_pool_stats(pool_id)
        if stats:
            if accepted:
                if stale:
                    stats.shares_stale += 1
                else:
                    stats.shares_accepted += 1
            else:
                stats.shares_rejected += 1
    
    def update_difficulty(self, pool_id: str, difficulty: float):
        """Update pool difficulty"""
        stats = self.failover.get_pool_stats(pool_id)
        if stats:
            stats.difficulty = difficulty
    
    def update_hashrate(self, pool_id: str, hashrate: float):
        """Update measured hashrate"""
        stats = self.failover.get_pool_stats(pool_id)
        if stats:
            stats.hashrate = hashrate
    
    def get_status_report(self) -> Dict[str, Any]:
        """Get status report for all pools"""
        return {
            "timestamp": datetime.utcnow().isoformat(),
            "current_pool": self.failover.current_pool,
            "pools": self.failover.get_all_stats(),
            "failover_history": [
                {
                    "timestamp": fh["timestamp"].isoformat(),
                    "from_pool": fh["from_pool"],
                    "to_pool": fh["to_pool"],
                    "reason": fh["reason"]
                }
                for fh in self.failover.failover_history[-10:]
            ]
        }
