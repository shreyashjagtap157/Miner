# Miner - Android Cryptocurrency Mining Application

A modern Android application for mobile cryptocurrency mining with a focus on efficiency, user experience, and earnings optimization.

## Features

### Mining
- **Multi-algorithm Support**: CPU and GPU-optimized mining algorithms
- **Power-efficient Mining**: Battery and thermal management
- **Background Mining**: Continue mining when app is minimized
- **Mining Pools**: Connect to various mining pools
- **Hashrate Monitoring**: Real-time performance tracking

### Wallet
- **Built-in Wallet**: Secure cryptocurrency storage
- **Transaction History**: Complete transaction tracking
- **QR Code Support**: Easy address sharing
- **Multiple Currencies**: Support for various cryptocurrencies

### Analytics
- **Earnings Dashboard**: Track daily/weekly/monthly earnings
- **Performance Graphs**: Visualize mining performance
- **Power Consumption**: Monitor energy usage
- **Profitability Calculator**: Estimate potential earnings

### Gamification
- **Achievement System**: Unlock badges and rewards
- **Leaderboards**: Compare with other miners
- **Daily Challenges**: Complete tasks for bonuses
- **Referral Program**: Earn from referrals

## Project Structure

```
Miner/
├── app/
│   └── src/main/
│       ├── java/com/meetmyartist/miner/
│       │   ├── MainActivity.kt           # Main entry point
│       │   ├── MinerApplication.kt        # Application class
│       │   ├── auth/                      # Authentication
│       │   ├── data/                      # Data layer (Room, Repos)
│       │   ├── di/                        # Dependency injection
│       │   ├── mining/                    # Mining logic
│       │   ├── network/                   # API clients
│       │   ├── service/                   # Background services
│       │   ├── ui/                        # UI components
│       │   ├── wallet/                    # Wallet management
│       │   └── widget/                    # Home screen widgets
│       ├── cpp/                           # Native mining code
│       └── res/                           # Resources
└── features/                              # Future modular features
```

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Clean Architecture
- **DI**: Hilt
- **Database**: Room
- **Networking**: Retrofit + OkHttp
- **Native**: C++ for mining algorithms
- **Async**: Kotlin Coroutines + Flow

## Requirements

- Android 8.0 (API 26) or higher
- ARM64 or x86_64 processor
- Minimum 2GB RAM
- 100MB free storage

## Building

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34
- NDK 25.2 or later (for native code)

### Build Steps

```bash
# Clone repository
git clone https://github.com/yourname/miner.git
cd miner

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test
```

## Configuration

Create `local.properties` in the project root:

```properties
sdk.dir=/path/to/android/sdk
ndk.dir=/path/to/android/ndk
```

API keys should be stored in `app/src/main/res/values/secrets.xml` (not committed):

```xml
<resources>
    <string name="api_key">your_api_key</string>
    <string name="pool_api_key">your_pool_key</string>
</resources>
```

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                         │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐    │
│  │  Screens   │  │  ViewModels│  │   Compose  │    │
│  └────────────┘  └────────────┘  └────────────┘    │
├─────────────────────────────────────────────────────┤
│                  Domain Layer                       │
│  ┌────────────┐  ┌────────────┐                    │
│  │  Use Cases │  │  Entities  │                    │
│  └────────────┘  └────────────┘                    │
├─────────────────────────────────────────────────────┤
│                   Data Layer                        │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐    │
│  │   Room DB  │  │ Repository │  │  Network   │    │
│  └────────────┘  └────────────┘  └────────────┘    │
├─────────────────────────────────────────────────────┤
│                  Native Layer                       │
│  ┌────────────────────────────────────────────┐    │
│  │           C++ Mining Algorithms             │    │
│  └────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

## Performance Tips

1. **Mining Efficiency**
   - Use WiFi instead of mobile data
   - Keep device plugged in during mining
   - Enable background optimization exceptions

2. **Battery Management**
   - Set mining intensity based on battery level
   - Use scheduled mining during charging
   - Monitor device temperature

3. **Network**
   - Use closest mining pool server
   - Enable connection redundancy
   - Monitor stratum connection

## Security

- Private keys are encrypted with AES-256
- Biometric authentication support
- No plain-text storage of sensitive data
- SSL pinning for API connections

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## License

```
Mozilla Public License Version 2.0
```

See [LICENSE](LICENSE) for full details.

## Disclaimer

Cryptocurrency mining on mobile devices may:
- Cause excessive battery drain
- Generate significant heat
- Reduce device lifespan

Mine responsibly and at your own risk.
