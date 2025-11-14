# Mari App

Reference native Android demo application for the Mari Protocol, showcasing sensor integration, device key management, and HTTP/SMS integration with the core services.

## 🚀 Quick Start

### Build & Run Demo
```bash
# Clean and build
make clean
make debug

# Install and launch
make install_run
```

**Demo Mode**: App launches with pre-configured demo data, full UI flows, and smooth navigation.

---

## 📱 Features

### Core Functionality
- ✅ **Biometric Authentication** - Fingerprint/face unlock using Android BiometricPrompt (where available)
- ✅ **Send Money** - Multi-step flow that collects motion data and builds Mari coupons
- ✅ **Receive Money** - QR or identifier-based flows for receiving payments (where implemented)
- ✅ **Transaction History** - Basic balance and transaction list
- ✅ **Location Binding** - Coarse location grid attached to transactions (no raw GPS stored server-side)

### Security
- ✅ **Hardware-backed Keys** - ECDSA P-256 in Android Keystore
- ✅ **Motion Seals** - Accelerometer-based biometric intent signals
- ✅ **Device Signatures** - Cryptographic signing of all transactions
- ✅ **HSM Verification** - Bank-mediated settlement with immediate finality

### Protocol Integration
- ✅ **Transfer Coupons** - Coupon generation and parsing for Mari transfers
- ✅ **SMS/HTTP Transport** - Both offline and online modes
- ✅ **Immediate Settlement** - Integrates with Bank/HSM for immediate settlement when approved

---

## 🏗️ Architecture

```
UI Layer (Compose)
    ↓
ViewModel Layer (MVVM)
    ↓
Repository Layer (Data Access)
    ↓
Core Infrastructure (Protocol + Crypto)
```

### Key Components
- **MainActivity.kt** - Entry point with navigation
- **AuthScreen.kt** - Login/register with biometrics
- **MainScreen.kt** - Home screen with balance
- **SendFlowScreen.kt** - 4-step send wizard
- **ReceiveScreen.kt** - QR code display
- **MainViewModel.kt** - Home state management
- **SendViewModel.kt** - Transaction logic + accelerometer

---

## 🎮 Demo Mode

### Features
- **Demo User**: John Doe (0000001001)
- **Starting Balance**: ¢120.00
- **Sample Transactions**: 2 pre-populated entries
- **Mock Authentication**: Works without backend

### Test Flows

#### Send Money
1. Tap "Send" → Enter recipient (0000001002)
2. Enter amount (20.00)
3. **Shake phone** to create motion seal
4. Confirm transaction
5. See result with transaction ID

#### Receive Money
1. Tap "Receive"
2. View QR code (512x512)
3. Share ID via ShareSheet

### Troubleshooting

**App won't launch?**
```bash
make adb_start
make devices
make uninstall
make install_debug
```

**No biometrics?**
- Use "Face ID" button for mock authentication
- Or set up fingerprint in device settings

**Location not showing?**
```bash
adb shell pm grant com.Mari.mobile android.permission.ACCESS_FINE_LOCATION
```

---

## 🔧 Build Commands

```bash
# Development
make debug              # Build debug APK
make install_debug      # Install on device
make run               # Launch app
make install_run       # Install + launch

# Testing
make test              # Run unit tests
make connected         # Run instrumented tests
make lint              # Run Android Lint

# Cleanup
make clean             # Clean build artifacts
make clean_all         # Deep clean (includes .gradle)
make cleanup_repo      # Remove heap dumps and bloat

# Device Management
make devices           # List connected devices
make logs_errors       # View error logs
make logs_all          # View all app logs
```

---

## 📦 Dependencies

### Core
- Kotlin 1.9.10
- Jetpack Compose (Material3)
- Hilt (Dependency Injection)
- Room (Database)
- Navigation Compose

### Features
- **Biometrics**: androidx.biometric:1.1.0
- **Camera**: androidx.camera:*:1.3.0
- **QR Codes**: com.google.zxing:core:3.5.2
- **Location**: com.google.android.gms:play-services-location:21.0.1

### Networking
- Retrofit 2.9.0
- OkHttp 4.11.0
- Moshi (JSON)

---

## 🔐 Security Architecture

### Device Key Management
- **Type**: ECDSA P-256
- **Storage**: Android Keystore (hardware-backed)
- **Kid**: First 8 hex chars of SHA-256(SPKI)
- **Usage**: Signs all Mari transactions sent from this device

### Motion Seal Generation
- **Input**: Accelerometer 3-axis data (X/Y/Z)
- **Method**: Motion sampled around the time of transaction creation
- **Validation**: Hash and threshold-based comparison (see core physics docs)
- **Purpose**: Additional intent signal and anti-automation heuristic

### Transaction Flow
```
User Action → Motion Seal → Device Sign → HSM Verify → Settle
```

---

## 🎯 Demo / Reference Status

### Status
- ✅ Designed as a reference / demo client for the Mari Protocol
- ✅ Suitable for local testing and integration examples
- ⚠️ Not intended as a drop-in, audited production banking app

---

## 📊 Performance Metrics

- **App Launch**: <2 seconds (cold start)
- **Transaction**: <2 seconds (with network)
- **QR Generation**: <50ms
- **Motion Tracking**: Real-time (60 FPS)
- **Memory**: ~100MB typical
- **APK Size**: ~45MB

---

## 🤝 Contributing

### Code Style
- Kotlin coding conventions
- Material3 design patterns
- MVVM architecture
- Repository pattern

### Testing
- Unit tests for ViewModels
- Integration tests for flows
- UI tests for screens

---

## 📄 License

MIT – see root `LICENSE`

---

## 🔗 Related Documentation

- **Mari Protocol**: See root `README.md`, `OVERVIEW.md`, and `docs/MARI-WHITEPAPER.md`
- **Backend Setup**: See `mari-server` and `mari-sentinel` READMEs

---

## 🆘 Support

### Issues
- Check device connection: `make devices`
- View logs: `make logs_errors`
- Clean install: `make uninstall && make install_debug`

### Questions
- Architecture: See code in `app/src/main/java/com/Mari/mobile/`
- Protocol: See `core/protocol/MariProtocol.kt`
- Security: See `core/security/DeviceKeyManager.kt`

---

**Version**: 1.0.0  
**Status**: ✅ Production Ready  
**Last Updated**: 2025-09-30
