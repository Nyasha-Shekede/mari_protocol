package com.Mari.mobileapp.core.network

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Monitors network connectivity status and handles Wi‑Fi Direct (P2P) connections
 */
class NetworkManager(context: Context, enableWifiP2p: Boolean = true) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val wifiP2pManager: WifiP2pManager? =
        if (enableWifiP2p) context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager else null

    // State management
    private val _networkState = MutableStateFlow(NetworkState.DISCONNECTED)
    val networkState: StateFlow<NetworkState> = _networkState

    private val _wifiP2pState = MutableStateFlow<WifiP2pState>(WifiP2pState.DISCONNECTED)
    val wifiP2pState: StateFlow<WifiP2pState> = _wifiP2pState

    private val _availablePeers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val availablePeers: StateFlow<List<WifiP2pDevice>> = _availablePeers

    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo

    private var channel: WifiP2pManager.Channel? = null

    // Network callback for connectivity changes
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { updateNetworkState() }
        override fun onLost(network: Network) { updateNetworkState() }
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) { updateNetworkState() }
    }

    init {
        if (wifiP2pManager != null) {
            initializeWifiP2p(context)
        }
        updateNetworkState()
    }

    /** Initialize Wi‑Fi P2P */
    private fun initializeWifiP2p(context: Context) {
        val mgr = wifiP2pManager ?: return
        channel = mgr.initialize(context, context.mainLooper) {
            // Channel lost
            _wifiP2pState.value = WifiP2pState.ERROR("Wi‑Fi P2P channel lost")
            channel = null
        }
    }

    /** Start monitoring network connectivity */
    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        updateNetworkState()
    }

    /** Stop monitoring network connectivity */
    fun stopMonitoring() {
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (_: Exception) {}
    }

    /** Update current network state */
    private fun updateNetworkState() {
        val active = connectivityManager.activeNetwork
        val caps = active?.let { connectivityManager.getNetworkCapabilities(it) }
        val connected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _networkState.value = if (connected) NetworkState.CONNECTED else NetworkState.DISCONNECTED
    }

    /** Returns true if device has Internet connectivity */
    fun isConnected(): Boolean = networkState.value == NetworkState.CONNECTED

    /** Get device IPv4 addresses (Wi‑Fi/Cellular) */
    fun getLocalIpAddresses(): List<String> {
        val result = mutableListOf<String>()
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
        for (ni in interfaces) {
            val addrs = ni.inetAddresses
            for (addr in addrs) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    val ip = addr.hostAddress
                    if (ip != null) result.add(ip)
                }
            }
        }
        return result
    }

    // -------- Wi‑Fi Direct (P2P) APIs --------

    /** Begin peer discovery */
    fun startPeerDiscovery() {
        val mgr = wifiP2pManager ?: return
        val ch = channel ?: return
        mgr.discoverPeers(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { _wifiP2pState.value = WifiP2pState.DISCOVERING }
            override fun onFailure(reason: Int) { _wifiP2pState.value = WifiP2pState.ERROR("discoverPeers failed: $reason") }
        })
        requestPeers()
    }

    /** Stop peer discovery */
    fun stopPeerDiscovery() {
        val mgr = wifiP2pManager ?: return
        val ch = channel ?: return
        mgr.stopPeerDiscovery(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { if (_wifiP2pState.value == WifiP2pState.DISCOVERING) _wifiP2pState.value = WifiP2pState.DISCONNECTED }
            override fun onFailure(reason: Int) { _wifiP2pState.value = WifiP2pState.ERROR("stopPeerDiscovery failed: $reason") }
        })
    }

    /** Request current peer list */
    fun requestPeers() {
        val mgr = wifiP2pManager ?: return
        val ch = channel ?: return
        mgr.requestPeers(ch) { list ->
            _availablePeers.value = list.deviceList.toList()
        }
    }

    /** Connect to a selected peer */
    fun connectToPeer(device: WifiP2pDevice) {
        val mgr = wifiP2pManager ?: return
        val ch = channel ?: return
        val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
        mgr.connect(ch, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { _wifiP2pState.value = WifiP2pState.CONNECTING }
            override fun onFailure(reason: Int) { _wifiP2pState.value = WifiP2pState.ERROR("connect failed: $reason") }
        })
        requestConnectionInfo()
    }

    /** Cancel ongoing connection attempts */
    fun cancelConnect() {
        val mgr = wifiP2pManager ?: return
        val ch = channel ?: return
        mgr.cancelConnect(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { _wifiP2pState.value = WifiP2pState.DISCONNECTED }
            override fun onFailure(reason: Int) { _wifiP2pState.value = WifiP2pState.ERROR("cancelConnect failed: $reason") }
        })
    }

    /** Remove current P2P group */
    fun removeGroup() {
        val mgr = wifiP2pManager ?: return
        val ch = channel ?: return
        mgr.removeGroup(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { _wifiP2pState.value = WifiP2pState.DISCONNECTED; _connectionInfo.value = null }
            override fun onFailure(reason: Int) { _wifiP2pState.value = WifiP2pState.ERROR("removeGroup failed: $reason") }
        })
    }

    /** Request current connection info */
    fun requestConnectionInfo() {
        val mgr = wifiP2pManager ?: return
        val ch = channel ?: return
        mgr.requestConnectionInfo(ch) { info ->
            _connectionInfo.value = info
            _wifiP2pState.value = if (info.groupFormed) WifiP2pState.CONNECTED(info) else WifiP2pState.DISCONNECTED
        }
    }

    /** Wi‑Fi state helpers */
    fun isWifiEnabled(): Boolean = wifiManager.isWifiEnabled
    fun setWifiEnabled(enabled: Boolean) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = enabled
        } else {
            // Programmatic Wi‑Fi toggle is restricted on Android 10+.
            // Consider surfacing a Settings Panel from the UI layer if needed.
        }
    }

    /**
     * Open system Internet panel (Android 10+) or Wi‑Fi settings (older).
     * Safe to call from UI layer (Activity/Compose with LocalContext).
     */
    fun openInternetPanel(ctx: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use action string to avoid compile-time dependency on Settings.Panel
            Intent("android.settings.panel.action.INTERNET_CONNECTIVITY")
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { ctx.startActivity(intent) } catch (_: Exception) { /* ignore */ }
    }

    // -------- Types --------

    enum class NetworkState { CONNECTED, DISCONNECTED }

    sealed class WifiP2pState {
        object DISCONNECTED : WifiP2pState()
        object DISCOVERING : WifiP2pState()
        object CONNECTING : WifiP2pState()
        data class CONNECTED(val info: WifiP2pInfo) : WifiP2pState()
        data class ERROR(val message: String) : WifiP2pState()
    }
}
