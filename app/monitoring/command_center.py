"""
Remote Monitoring Command Center for Mining

Web-based dashboard for remote monitoring and control of mining operations.

Features:
- Real-time mining metrics
- Remote device control
- Temperature and power monitoring
- Historical data and analytics
- Alert management
- Configuration management
"""

import logging
from typing import Dict, List, Optional, Any, Set
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from enum import Enum
import json

logger = logging.getLogger(__name__)


class AlertSeverity(Enum):
    """Alert severity levels"""
    INFO = "info"
    WARNING = "warning"
    CRITICAL = "critical"


class DeviceCommand(Enum):
    """Remote device commands"""
    START_MINING = "start_mining"
    STOP_MINING = "stop_mining"
    RESTART_MINER = "restart_miner"
    REBOOT_DEVICE = "reboot_device"
    CHANGE_ALGORITHM = "change_algorithm"
    CHANGE_POOL = "change_pool"
    UPDATE_SETTINGS = "update_settings"
    COLLECT_LOGS = "collect_logs"


@dataclass
class MiningDevice:
    """Mining device information"""
    device_id: str
    name: str
    device_type: str  # GPU, ASIC, CPU
    location: str
    
    # Status
    is_online: bool = True
    is_mining: bool = False
    
    # Hardware info
    gpu_memory_mb: float = 0.0
    compute_capability: str = ""
    driver_version: str = ""
    
    # Current mining
    algorithm: Optional[str] = None
    pool: Optional[str] = None
    hashrate_mhs: float = 0.0
    
    # Temperature and power
    current_temp_c: float = 0.0
    max_temp_c: float = 80.0
    power_draw_watts: float = 0.0
    power_limit_watts: float = 0.0
    
    # Performance
    uptime_hours: float = 0.0
    shares_submitted: int = 0
    shares_accepted: int = 0
    shares_rejected: int = 0
    
    # Last update
    last_seen: datetime = field(default_factory=datetime.utcnow)
    
    # Metadata
    metadata: Dict[str, Any] = field(default_factory=dict)


@dataclass
class Alert:
    """System alert"""
    alert_id: str
    device_id: str
    severity: AlertSeverity
    title: str
    message: str
    
    timestamp: datetime = field(default_factory=datetime.utcnow)
    acknowledged: bool = False
    acknowledged_by: Optional[str] = None
    acknowledged_time: Optional[datetime] = None
    
    # Auto-resolution
    auto_resolve: bool = False
    resolved: bool = False
    resolved_time: Optional[datetime] = None


@dataclass
class MetricsSample:
    """Metrics sample for a device"""
    timestamp: datetime
    device_id: str
    
    hashrate_mhs: float
    temp_c: float
    power_watts: float
    
    shares_accepted: int
    shares_rejected: int
    efficiency: float  # MH/W


class RemoteCommand:
    """Remote command queue entry"""
    
    def __init__(
        self,
        device_id: str,
        command: DeviceCommand,
        parameters: Optional[Dict[str, Any]] = None
    ):
        self.command_id = f"{device_id}_{datetime.utcnow().timestamp()}"
        self.device_id = device_id
        self.command = command
        self.parameters = parameters or {}
        
        self.created_at = datetime.utcnow()
        self.sent_at: Optional[datetime] = None
        self.executed_at: Optional[datetime] = None
        self.status = "pending"  # pending, sent, executed, failed
        self.result: Optional[Dict[str, Any]] = None


class DeviceRegistry:
    """Registry of mining devices"""
    
    def __init__(self):
        self.devices: Dict[str, MiningDevice] = {}
        self.offline_threshold_seconds = 300  # 5 minutes
    
    def register_device(self, device: MiningDevice):
        """Register a device"""
        self.devices[device.device_id] = device
        logger.info(f"Registered device {device.device_id}: {device.name}")
    
    def update_device_status(
        self,
        device_id: str,
        **updates
    ):
        """Update device status"""
        if device_id not in self.devices:
            return
        
        device = self.devices[device_id]
        for key, value in updates.items():
            if hasattr(device, key):
                setattr(device, key, value)
        
        device.last_seen = datetime.utcnow()
    
    def get_device(self, device_id: str) -> Optional[MiningDevice]:
        """Get device by ID"""
        return self.devices.get(device_id)
    
    def get_all_devices(self) -> List[MiningDevice]:
        """Get all registered devices"""
        return list(self.devices.values())
    
    def get_online_devices(self) -> List[MiningDevice]:
        """Get online devices"""
        now = datetime.utcnow()
        return [
            d for d in self.devices.values()
            if (now - d.last_seen).total_seconds() < self.offline_threshold_seconds
        ]
    
    def get_mining_devices(self) -> List[MiningDevice]:
        """Get devices currently mining"""
        return [d for d in self.devices.values() if d.is_mining]
    
    def get_devices_by_location(self, location: str) -> List[MiningDevice]:
        """Get devices by location"""
        return [d for d in self.devices.values() if d.location == location]


class AlertManager:
    """Manages system alerts"""
    
    def __init__(self, max_alerts: int = 10000):
        self.alerts: Dict[str, Alert] = {}
        self.max_alerts = max_alerts
        
        self.alert_history: List[str] = []  # Alert IDs in order
        self.alert_rules: List[Dict[str, Any]] = []
    
    def create_alert(
        self,
        device_id: str,
        severity: AlertSeverity,
        title: str,
        message: str,
        auto_resolve: bool = False
    ) -> Alert:
        """Create new alert"""
        alert_id = f"{device_id}_{datetime.utcnow().timestamp()}"
        
        alert = Alert(
            alert_id=alert_id,
            device_id=device_id,
            severity=severity,
            title=title,
            message=message,
            auto_resolve=auto_resolve
        )
        
        self.alerts[alert_id] = alert
        self.alert_history.append(alert_id)
        
        # Enforce max alerts
        if len(self.alerts) > self.max_alerts:
            oldest_id = self.alert_history.pop(0)
            self.alerts.pop(oldest_id, None)
        
        logger.warning(f"Alert created: {severity.value} - {title}")
        
        return alert
    
    def acknowledge_alert(
        self,
        alert_id: str,
        user: str
    ):
        """Acknowledge alert"""
        if alert_id in self.alerts:
            alert = self.alerts[alert_id]
            alert.acknowledged = True
            alert.acknowledged_by = user
            alert.acknowledged_time = datetime.utcnow()
    
    def resolve_alert(self, alert_id: str):
        """Mark alert as resolved"""
        if alert_id in self.alerts:
            alert = self.alerts[alert_id]
            alert.resolved = True
            alert.resolved_time = datetime.utcnow()
    
    def get_active_alerts(self) -> List[Alert]:
        """Get unresolved alerts"""
        return [a for a in self.alerts.values() if not a.resolved]
    
    def get_critical_alerts(self) -> List[Alert]:
        """Get critical unresolved alerts"""
        return [
            a for a in self.alerts.values()
            if not a.resolved and a.severity == AlertSeverity.CRITICAL
        ]
    
    def get_alerts_for_device(self, device_id: str) -> List[Alert]:
        """Get alerts for specific device"""
        return [a for a in self.alerts.values() if a.device_id == device_id]
    
    def add_alert_rule(self, rule: Dict[str, Any]):
        """Add alerting rule"""
        self.alert_rules.append(rule)
    
    def check_alert_rules(self, device: MiningDevice) -> List[Alert]:
        """Check rules against device status"""
        triggered_alerts = []
        
        for rule in self.alert_rules:
            condition = rule.get("condition")
            
            # Temperature rule
            if condition == "high_temp" and device.current_temp_c > rule.get("threshold", 80):
                alert = self.create_alert(
                    device.device_id,
                    AlertSeverity.WARNING,
                    f"High temperature on {device.name}",
                    f"Temperature: {device.current_temp_c}°C (threshold: {rule.get('threshold')}°C)",
                    auto_resolve=True
                )
                triggered_alerts.append(alert)
            
            # Power rule
            elif condition == "high_power" and device.power_draw_watts > rule.get("threshold", 300):
                alert = self.create_alert(
                    device.device_id,
                    AlertSeverity.INFO,
                    f"High power draw on {device.name}",
                    f"Power: {device.power_draw_watts}W (threshold: {rule.get('threshold')}W)",
                    auto_resolve=True
                )
                triggered_alerts.append(alert)
            
            # Offline rule
            elif condition == "offline" and not device.is_online:
                alert = self.create_alert(
                    device.device_id,
                    AlertSeverity.CRITICAL,
                    f"Device offline: {device.name}",
                    f"Last seen: {device.last_seen}"
                )
                triggered_alerts.append(alert)
            
            # Low hash rate
            elif condition == "low_hashrate" and device.is_mining and device.hashrate_mhs < rule.get("threshold", 1):
                alert = self.create_alert(
                    device.device_id,
                    AlertSeverity.WARNING,
                    f"Low hashrate on {device.name}",
                    f"Hashrate: {device.hashrate_mhs} MH/s (threshold: {rule.get('threshold')} MH/s)",
                    auto_resolve=True
                )
                triggered_alerts.append(alert)
        
        return triggered_alerts


class MetricsCollector:
    """Collects and stores metrics"""
    
    def __init__(self, max_samples: int = 100000):
        self.samples: List[MetricsSample] = []
        self.max_samples = max_samples
        
        self.device_last_sample: Dict[str, MetricsSample] = {}
    
    def record_sample(self, sample: MetricsSample):
        """Record metrics sample"""
        self.samples.append(sample)
        self.device_last_sample[sample.device_id] = sample
        
        # Enforce max samples
        if len(self.samples) > self.max_samples:
            self.samples.pop(0)
    
    def get_samples_for_device(
        self,
        device_id: str,
        hours: int = 24
    ) -> List[MetricsSample]:
        """Get samples for device in last N hours"""
        cutoff = datetime.utcnow() - timedelta(hours=hours)
        
        return [
            s for s in self.samples
            if s.device_id == device_id and s.timestamp > cutoff
        ]
    
    def get_device_stats(self, device_id: str) -> Optional[Dict[str, Any]]:
        """Get statistics for device"""
        samples = self.get_samples_for_device(device_id, hours=24)
        
        if not samples:
            return None
        
        hashrates = [s.hashrate_mhs for s in samples]
        temps = [s.temp_c for s in samples]
        powers = [s.power_watts for s in samples]
        
        return {
            "avg_hashrate": float(sum(hashrates) / len(hashrates)) if hashrates else 0,
            "max_hashrate": float(max(hashrates)) if hashrates else 0,
            "min_hashrate": float(min(hashrates)) if hashrates else 0,
            
            "avg_temp": float(sum(temps) / len(temps)) if temps else 0,
            "max_temp": float(max(temps)) if temps else 0,
            
            "avg_power": float(sum(powers) / len(powers)) if powers else 0,
            "max_power": float(max(powers)) if powers else 0,
            
            "total_shares_accepted": sum(s.shares_accepted for s in samples),
            "total_shares_rejected": sum(s.shares_rejected for s in samples),
            
            "samples_count": len(samples)
        }


class CommandQueue:
    """Manages remote command queue"""
    
    def __init__(self):
        self.commands: Dict[str, RemoteCommand] = {}
        self.pending_commands: Dict[str, List[RemoteCommand]] = {}  # Per device
    
    def queue_command(
        self,
        device_id: str,
        command: DeviceCommand,
        parameters: Optional[Dict[str, Any]] = None
    ) -> RemoteCommand:
        """Queue command for device"""
        cmd = RemoteCommand(device_id, command, parameters)
        
        self.commands[cmd.command_id] = cmd
        
        if device_id not in self.pending_commands:
            self.pending_commands[device_id] = []
        
        self.pending_commands[device_id].append(cmd)
        
        logger.info(f"Queued command {command.value} for device {device_id}")
        
        return cmd
    
    def get_pending_commands(self, device_id: str) -> List[RemoteCommand]:
        """Get pending commands for device"""
        return self.pending_commands.get(device_id, [])
    
    def mark_command_sent(self, command_id: str):
        """Mark command as sent"""
        if command_id in self.commands:
            cmd = self.commands[command_id]
            cmd.sent_at = datetime.utcnow()
            cmd.status = "sent"
    
    def mark_command_executed(
        self,
        command_id: str,
        result: Optional[Dict[str, Any]] = None
    ):
        """Mark command as executed"""
        if command_id in self.commands:
            cmd = self.commands[command_id]
            cmd.executed_at = datetime.utcnow()
            cmd.status = "executed"
            cmd.result = result or {}
            
            # Remove from pending
            if cmd.device_id in self.pending_commands:
                self.pending_commands[cmd.device_id] = [
                    c for c in self.pending_commands[cmd.device_id]
                    if c.command_id != command_id
                ]


class MonitoringCommandCenter:
    """
    Main coordinator for remote monitoring and control.
    """
    
    def __init__(self):
        self.device_registry = DeviceRegistry()
        self.alert_manager = AlertManager()
        self.metrics_collector = MetricsCollector()
        self.command_queue = CommandQueue()
        
        # Setup default alert rules
        self._setup_default_rules()
    
    def _setup_default_rules(self):
        """Setup default alerting rules"""
        self.alert_manager.add_alert_rule({
            "condition": "high_temp",
            "threshold": 80
        })
        
        self.alert_manager.add_alert_rule({
            "condition": "high_power",
            "threshold": 350
        })
        
        self.alert_manager.add_alert_rule({
            "condition": "offline"
        })
        
        self.alert_manager.add_alert_rule({
            "condition": "low_hashrate",
            "threshold": 5  # MH/s
        })
    
    def register_device(self, device: MiningDevice):
        """Register mining device"""
        self.device_registry.register_device(device)
    
    def update_device_metrics(
        self,
        device_id: str,
        hashrate: float,
        temp: float,
        power: float,
        shares_accepted: int,
        shares_rejected: int
    ):
        """Update device metrics"""
        device = self.device_registry.get_device(device_id)
        if not device:
            return
        
        # Update device
        self.device_registry.update_device_status(
            device_id,
            hashrate_mhs=hashrate,
            current_temp_c=temp,
            power_draw_watts=power,
            shares_accepted=shares_accepted,
            shares_rejected=shares_rejected,
            is_online=True
        )
        
        # Record metrics
        efficiency = hashrate / power if power > 0 else 0
        sample = MetricsSample(
            timestamp=datetime.utcnow(),
            device_id=device_id,
            hashrate_mhs=hashrate,
            temp_c=temp,
            power_watts=power,
            shares_accepted=shares_accepted,
            shares_rejected=shares_rejected,
            efficiency=efficiency
        )
        self.metrics_collector.record_sample(sample)
        
        # Check alert rules
        device = self.device_registry.get_device(device_id)
        self.alert_manager.check_alert_rules(device)
    
    def get_dashboard_data(self) -> Dict[str, Any]:
        """Get comprehensive dashboard data"""
        devices = self.device_registry.get_all_devices()
        online_devices = self.device_registry.get_online_devices()
        mining_devices = self.device_registry.get_mining_devices()
        
        return {
            "timestamp": datetime.utcnow().isoformat(),
            "total_devices": len(devices),
            "online_devices": len(online_devices),
            "mining_devices": len(mining_devices),
            
            "devices": [
                {
                    "device_id": d.device_id,
                    "name": d.name,
                    "is_online": d.is_online,
                    "is_mining": d.is_mining,
                    "hashrate": d.hashrate_mhs,
                    "temperature": d.current_temp_c,
                    "power": d.power_draw_watts,
                    "algorithm": d.algorithm,
                    "shares_accepted": d.shares_accepted,
                    "uptime_hours": d.uptime_hours
                }
                for d in devices
            ],
            
            "critical_alerts": len(self.alert_manager.get_critical_alerts()),
            "active_alerts": len(self.alert_manager.get_active_alerts()),
            
            "pending_commands": sum(
                len(cmds) for cmds in self.command_queue.pending_commands.values()
            )
        }
