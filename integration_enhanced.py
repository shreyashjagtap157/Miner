"""
Integration of mining pool management, profit switching, and remote monitoring
into the Miner's main operations.
"""

import asyncio
import logging
from datetime import datetime, timedelta
from typing import Optional, List, Dict
import json

from app.mining import MiningPoolManager, PoolFailover, ProfitSwitcher
from app.monitoring import MonitoringCommandCenter, DeviceRegistry, AlertManager

logger = logging.getLogger(__name__)


class EnhancedMiningOperations:
    """
    Enhanced mining operations with pool failover, profit switching,
    and remote monitoring capabilities.
    """
    
    def __init__(
        self,
        enable_pool_management: bool = True,
        enable_profit_switching: bool = True,
        enable_monitoring: bool = True,
        monitor_port: int = 5000
    ):
        # Initialize pool manager
        self.pool_manager: Optional[MiningPoolManager] = None
        if enable_pool_management:
            self.pool_manager = MiningPoolManager(
                pools=[
                    {
                        'name': 'primary',
                        'url': 'stratum+tcp://pool.example.com:3333',
                        'username': 'user1',
                        'password': 'pass1'
                    },
                    {
                        'name': 'secondary',
                        'url': 'stratum+tcp://backup.example.com:3333',
                        'username': 'user2',
                        'password': 'pass2'
                    }
                ],
                healthcheck_interval=60
            )
        
        # Initialize profit switcher
        self.profit_switcher: Optional[ProfitSwitcher] = None
        if enable_profit_switching:
            self.profit_switcher = ProfitSwitcher(
                update_interval=300,  # 5 minutes
                min_profitability_ratio=1.05,  # 5% margin
                cost_per_kwh=0.10
            )
        
        # Initialize monitoring command center
        self.command_center: Optional[MonitoringCommandCenter] = None
        self.alert_manager: Optional[AlertManager] = None
        self.device_registry: Optional[DeviceRegistry] = None
        if enable_monitoring:
            self.device_registry = DeviceRegistry()
            self.alert_manager = AlertManager()
            self.command_center = MonitoringCommandCenter(
                port=monitor_port,
                device_registry=self.device_registry,
                alert_manager=self.alert_manager
            )
        
        self.mining_loop_running = False
    
    async def startup_operations(self):
        """Initialize all mining operations."""
        if self.pool_manager:
            try:
                await self.pool_manager.initialize()
                active_pool = self.pool_manager.get_active_pool()
                logger.info(f"Pool manager initialized, active pool: {active_pool['name']}")
            except Exception as e:
                logger.error(f"Failed to initialize pool manager: {e}")
        
        if self.profit_switcher:
            try:
                await self.profit_switcher.start()
                logger.info("Profit switcher started")
            except Exception as e:
                logger.error(f"Failed to start profit switcher: {e}")
        
        if self.command_center:
            try:
                await self.command_center.start()
                logger.info(f"Monitoring command center started on port {self.command_center.port}")
            except Exception as e:
                logger.error(f"Failed to start command center: {e}")
    
    async def shutdown_operations(self):
        """Shutdown all mining operations."""
        if self.command_center:
            try:
                await self.command_center.stop()
                logger.info("Monitoring command center stopped")
            except Exception as e:
                logger.error(f"Error stopping command center: {e}")
        
        if self.profit_switcher:
            try:
                await self.profit_switcher.stop()
                logger.info("Profit switcher stopped")
            except Exception as e:
                logger.error(f"Error stopping profit switcher: {e}")
        
        if self.pool_manager:
            try:
                await self.pool_manager.shutdown()
                logger.info("Pool manager shutdown")
            except Exception as e:
                logger.error(f"Error during pool manager shutdown: {e}")
    
    async def register_mining_device(
        self,
        device_id: str,
        device_type: str,
        gpu_model: str,
        power_draw_w: float,
        hashrate_mhs: float
    ) -> bool:
        """Register a mining device in the registry."""
        if not self.device_registry:
            return False
        
        try:
            device_info = {
                'device_id': device_id,
                'device_type': device_type,
                'gpu_model': gpu_model,
                'power_draw_w': power_draw_w,
                'hashrate_mhs': hashrate_mhs,
                'status': 'active',
                'registered_at': datetime.utcnow().isoformat()
            }
            
            self.device_registry.add_device(device_id, device_info)
            logger.info(f"Device registered: {device_id} ({gpu_model})")
            return True
        except Exception as e:
            logger.error(f"Failed to register device: {e}")
            return False
    
    async def monitor_mining_status(self) -> Dict:
        """Get comprehensive mining status."""
        status = {
            'timestamp': datetime.utcnow().isoformat(),
            'pool_status': None,
            'profitability': None,
            'devices': None,
            'alerts': None
        }
        
        # Pool status
        if self.pool_manager:
            try:
                status['pool_status'] = {
                    'active_pool': self.pool_manager.get_active_pool()['name'],
                    'shares_accepted': self.pool_manager.get_stats()['shares_accepted'],
                    'shares_rejected': self.pool_manager.get_stats()['shares_rejected'],
                    'current_difficulty': self.pool_manager.get_stats()['difficulty']
                }
                logger.debug(f"Pool status: {status['pool_status']}")
            except Exception as e:
                logger.warning(f"Failed to get pool status: {e}")
        
        # Profitability
        if self.profit_switcher:
            try:
                current_algo = self.profit_switcher.get_current_algorithm()
                profitability = self.profit_switcher.calculate_profitability(current_algo)
                status['profitability'] = {
                    'current_algorithm': current_algo,
                    'estimated_daily_profit': profitability['daily_profit'],
                    'roi_percentage': profitability['roi_pct']
                }
                logger.debug(f"Profitability: {current_algo} ${profitability['daily_profit']:.2f}/day")
            except Exception as e:
                logger.warning(f"Failed to calculate profitability: {e}")
        
        # Device status
        if self.device_registry:
            try:
                devices = self.device_registry.get_all_devices()
                status['devices'] = {
                    'total': len(devices),
                    'active': sum(1 for d in devices.values() if d.get('status') == 'active'),
                    'devices': devices
                }
                logger.debug(f"Devices: {status['devices']['active']}/{status['devices']['total']} active")
            except Exception as e:
                logger.warning(f"Failed to get device status: {e}")
        
        # Alerts
        if self.alert_manager:
            try:
                alerts = self.alert_manager.get_pending_alerts()
                status['alerts'] = {
                    'pending': len(alerts),
                    'recent': alerts[:5] if alerts else []
                }
                if alerts:
                    logger.warning(f"Pending alerts: {len(alerts)}")
            except Exception as e:
                logger.warning(f"Failed to get alerts: {e}")
        
        return status
    
    async def handle_pool_failover(self) -> bool:
        """Handle automatic pool failover if needed."""
        if not self.pool_manager:
            return False
        
        try:
            # Check health of current pool
            if not await self.pool_manager.health_check():
                logger.warning("Active pool health check failed, initiating failover")
                
                # Failover to next pool
                success = await self.pool_manager.failover_to_next_pool()
                if success:
                    new_pool = self.pool_manager.get_active_pool()
                    logger.info(f"Failover successful, now connected to {new_pool['name']}")
                    
                    # Alert about failover
                    if self.alert_manager:
                        self.alert_manager.create_alert(
                            level='warning',
                            message=f"Pool failover occurred, now using {new_pool['name']}"
                        )
                    return True
                else:
                    logger.error("Failover attempt failed")
                    if self.alert_manager:
                        self.alert_manager.create_alert(
                            level='critical',
                            message="Pool failover failed - no available pools"
                        )
                    return False
        except Exception as e:
            logger.error(f"Error during failover: {e}")
            return False
    
    async def check_and_switch_algorithm(self) -> Optional[str]:
        """Check profitability and switch algorithm if beneficial."""
        if not self.profit_switcher:
            return None
        
        try:
            current_algo = self.profit_switcher.get_current_algorithm()
            best_algo, profit_increase = self.profit_switcher.find_most_profitable_algorithm()
            
            if best_algo != current_algo and profit_increase > 0.05:  # 5% threshold
                logger.info(f"Switching algorithm from {current_algo} to {best_algo} "
                          f"(+{profit_increase:.1%} profitability)")
                
                # Perform the switch
                if await self.profit_switcher.switch_to_algorithm(best_algo):
                    if self.alert_manager:
                        self.alert_manager.create_alert(
                            level='info',
                            message=f"Algorithm switched to {best_algo} (+{profit_increase:.1%} profit)"
                        )
                    return best_algo
            
            return current_algo
        except Exception as e:
            logger.error(f"Error checking algorithm profitability: {e}")
            return None
    
    async def mining_operations_loop(self):
        """Main mining operations loop with continuous monitoring."""
        self.mining_loop_running = True
        iteration = 0
        
        try:
            while self.mining_loop_running:
                iteration += 1
                
                # Every 30 seconds: Check pool health and failover if needed
                if iteration % 6 == 0:
                    await self.handle_pool_failover()
                
                # Every 5 minutes: Check algorithm profitability
                if iteration % 60 == 0:
                    await self.check_and_switch_algorithm()
                
                # Every iteration: Log status
                status = await self.monitor_mining_status()
                logger.info(f"Mining status: Pool={status['pool_status']['active_pool'] if status['pool_status'] else 'N/A'}, "
                          f"Devices={status['devices']['active']}/{status['devices']['total'] if status['devices'] else 'N/A'}")
                
                # Check for critical alerts
                if status['alerts'] and status['alerts']['pending'] > 0:
                    logger.warning(f"⚠️  {status['alerts']['pending']} pending alerts")
                
                await asyncio.sleep(5)  # 5 second loop interval
        
        except asyncio.CancelledError:
            logger.info("Mining operations loop cancelled")
        except Exception as e:
            logger.error(f"Error in mining loop: {e}")
        finally:
            self.mining_loop_running = False
    
    async def run(self):
        """Run enhanced mining operations."""
        await self.startup_operations()
        
        # Register some example devices
        await self.register_mining_device(
            device_id="GPU_0",
            device_type="GPU",
            gpu_model="NVIDIA RTX 3090",
            power_draw_w=350,
            hashrate_mhs=120
        )
        
        await self.register_mining_device(
            device_id="GPU_1",
            device_type="GPU",
            gpu_model="NVIDIA RTX 3080",
            power_draw_w=320,
            hashrate_mhs=100
        )
        
        try:
            await self.mining_operations_loop()
        except KeyboardInterrupt:
            logger.info("Mining operations interrupted")
        finally:
            await self.shutdown_operations()


async def main():
    """Main mining system entry point."""
    mining_ops = EnhancedMiningOperations(
        enable_pool_management=True,
        enable_profit_switching=True,
        enable_monitoring=True,
        monitor_port=5000
    )
    
    logger.info("Enhanced mining operations initialized")
    
    try:
        await mining_ops.run()
    except Exception as e:
        logger.error(f"Mining operations failed: {e}")
    finally:
        logger.info("Mining operations shutdown complete")


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    asyncio.run(main())
