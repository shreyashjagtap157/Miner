"""
Mining Coordinator - Main entry point for enhanced mining operations.
Integrates pool management, profit switching, and remote monitoring.
"""

import asyncio
import logging
import yaml
from pathlib import Path
from datetime import datetime

# Import Miner enhancements
from app.mining import MiningPoolManager, ProfitSwitcher
from app.monitoring import MonitoringCommandCenter, DeviceRegistry, AlertManager

logger = logging.getLogger(__name__)


class MiningCoordinator:
    """Orchestrates all mining operations with enhancements."""
    
    def __init__(self, config_path: str = None):
        self.config_path = config_path or Path(__file__).parent / "config" / "mining.yaml"
        self.config = {}
        self.pool_manager = None
        self.profit_switcher = None
        self.command_center = None
        self.device_registry = None
        self.alert_manager = None
        self.mining_active = False
        
        self._load_config()
        self._initialize_components()
    
    def _load_config(self):
        """Load configuration from YAML file."""
        try:
            if Path(self.config_path).exists():
                with open(self.config_path) as f:
                    self.config = yaml.safe_load(f) or {}
                logger.info(f"Configuration loaded from {self.config_path}")
            else:
                logger.warning(f"Configuration file not found at {self.config_path}, using defaults")
        except Exception as e:
            logger.error(f"Error loading configuration: {e}")
    
    def _initialize_components(self):
        """Initialize all mining components."""
        try:
            # Initialize pool manager
            if self.config.get("pool_management", {}).get("enabled", False):
                pools = []
                pool_config = self.config.get("pool_management", {})
                
                # Add primary pool
                if "primary_pool" in pool_config:
                    pools.append(pool_config["primary_pool"])
                
                # Add backup pools
                for backup in pool_config.get("backup_pools", []):
                    pools.append(backup)
                
                if pools:
                    self.pool_manager = MiningPoolManager(
                        pools=pools,
                        healthcheck_interval=pool_config.get("health_check_interval_sec", 60)
                    )
                    logger.info(f"Pool manager initialized with {len(pools)} pools")
            
            # Initialize profit switcher
            if self.config.get("profit_switching", {}).get("enabled", False):
                profit_config = self.config.get("profit_switching", {})
                self.profit_switcher = ProfitSwitcher(
                    update_interval=profit_config.get("update_interval_sec", 300),
                    min_profitability_ratio=profit_config.get("min_profitability_ratio", 1.05),
                    cost_per_kwh=profit_config.get("cost_factors", {}).get("cost_per_kwh", 0.10)
                )
                logger.info("Profit switcher initialized")
            
            # Initialize device registry
            if self.config.get("device_registry", {}).get("enabled", False):
                self.device_registry = DeviceRegistry()
                logger.info("Device registry initialized")
            
            # Initialize alert manager
            if self.config.get("monitoring", {}).get("enabled", False):
                self.alert_manager = AlertManager()
                logger.info("Alert manager initialized")
            
            # Initialize command center
            if self.config.get("monitoring", {}).get("command_center_enabled", False):
                monitoring_config = self.config.get("monitoring", {})
                self.command_center = MonitoringCommandCenter(
                    port=monitoring_config.get("monitor_port", 5000),
                    device_registry=self.device_registry,
                    alert_manager=self.alert_manager
                )
                logger.info(f"Command center initialized on port {monitoring_config.get('monitor_port', 5000)}")
        
        except Exception as e:
            logger.error(f"Error initializing components: {e}")
    
    async def startup(self):
        """Start all mining services."""
        try:
            if self.pool_manager:
                await self.pool_manager.initialize()
                logger.info("Pool manager started")
            
            if self.profit_switcher:
                await self.profit_switcher.start()
                logger.info("Profit switcher started")
            
            if self.command_center:
                await self.command_center.start()
                logger.info("Command center started")
            
            self.mining_active = True
            logger.info("Mining coordinator ready")
        
        except Exception as e:
            logger.error(f"Error during startup: {e}")
            self.mining_active = False
    
    async def shutdown(self):
        """Shutdown all mining services."""
        try:
            self.mining_active = False
            
            if self.command_center:
                await self.command_center.stop()
                logger.info("Command center stopped")
            
            if self.profit_switcher:
                await self.profit_switcher.stop()
                logger.info("Profit switcher stopped")
            
            if self.pool_manager:
                await self.pool_manager.shutdown()
                logger.info("Pool manager stopped")
            
            logger.info("Mining coordinator shutdown complete")
        
        except Exception as e:
            logger.error(f"Error during shutdown: {e}")
    
    async def register_device(self, device_id: str, device_info: dict):
        """Register a mining device."""
        if self.device_registry:
            try:
                self.device_registry.add_device(device_id, device_info)
                logger.info(f"Device registered: {device_id}")
                
                # Log alert
                if self.alert_manager:
                    self.alert_manager.create_alert(
                        level='info',
                        message=f"Device {device_id} registered"
                    )
            except Exception as e:
                logger.error(f"Error registering device: {e}")
    
    async def get_status(self) -> dict:
        """Get comprehensive mining status."""
        status = {
            'timestamp': datetime.utcnow().isoformat(),
            'mining_active': self.mining_active,
            'pool_status': None,
            'profitability': None,
            'devices': None,
            'alerts': None
        }
        
        try:
            if self.pool_manager:
                active_pool = self.pool_manager.get_active_pool()
                pool_stats = self.pool_manager.get_stats()
                status['pool_status'] = {
                    'active_pool': active_pool['name'] if active_pool else 'none',
                    'shares_accepted': pool_stats.get('shares_accepted', 0),
                    'shares_rejected': pool_stats.get('shares_rejected', 0)
                }
            
            if self.profit_switcher:
                current_algo = self.profit_switcher.get_current_algorithm()
                profitability = self.profit_switcher.calculate_profitability(current_algo)
                status['profitability'] = {
                    'algorithm': current_algo,
                    'daily_profit': profitability.get('daily_profit', 0),
                    'roi_pct': profitability.get('roi_pct', 0)
                }
            
            if self.device_registry:
                devices = self.device_registry.get_all_devices()
                status['devices'] = {
                    'total': len(devices),
                    'active': sum(1 for d in devices.values() if d.get('status') == 'active')
                }
            
            if self.alert_manager:
                alerts = self.alert_manager.get_pending_alerts()
                status['alerts'] = {
                    'pending': len(alerts) if alerts else 0
                }
        
        except Exception as e:
            logger.warning(f"Error getting status: {e}")
        
        return status
    
    async def mining_loop(self):
        """Main mining operations loop."""
        iteration = 0
        
        try:
            while self.mining_active:
                iteration += 1
                
                # Every 30 seconds: check pool health
                if iteration % 6 == 0:
                    if self.pool_manager:
                        try:
                            if not await self.pool_manager.health_check():
                                logger.warning("Pool health check failed, attempting failover")
                                await self.pool_manager.failover_to_next_pool()
                        except Exception as e:
                            logger.error(f"Error in failover: {e}")
                
                # Every 5 minutes: check algorithm profitability
                if iteration % 60 == 0:
                    if self.profit_switcher:
                        try:
                            current = self.profit_switcher.get_current_algorithm()
                            best, improvement = self.profit_switcher.find_most_profitable_algorithm()
                            
                            if best != current and improvement > 0.05:
                                logger.info(f"Switching to {best} (+{improvement:.1%})")
                                await self.profit_switcher.switch_to_algorithm(best)
                        except Exception as e:
                            logger.error(f"Error checking profitability: {e}")
                
                # Log status
                if iteration % 10 == 0:
                    status = await self.get_status()
                    logger.info(f"Mining status: Pool={status['pool_status']['active_pool'] if status['pool_status'] else 'N/A'}, "
                              f"Devices={status['devices']['active']}/{status['devices']['total'] if status['devices'] else 'N/A'}")
                
                await asyncio.sleep(5)
        
        except asyncio.CancelledError:
            logger.info("Mining loop cancelled")
        except Exception as e:
            logger.error(f"Error in mining loop: {e}")
        finally:
            self.mining_active = False
    
    async def run(self):
        """Run mining coordinator."""
        await self.startup()
        try:
            await self.mining_loop()
        except KeyboardInterrupt:
            logger.info("Mining interrupted by user")
        finally:
            await self.shutdown()


async def main():
    """Main entry point."""
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    coordinator = MiningCoordinator()
    try:
        await coordinator.run()
    except Exception as e:
        logger.error(f"Fatal error: {e}")
    finally:
        logger.info("Mining coordinator stopped")


if __name__ == "__main__":
    asyncio.run(main())
