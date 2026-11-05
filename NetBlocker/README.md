# NetBlocker 1.0

NetBlocker is an Android per-app internet blocker for locally installed apps. It uses Android's built-in `VpnService` permission and does not need root, Shizuku, an external VPN server, or an `INTERNET` permission.

## How it works

- The app lists installed apps that have a launcher icon.
- The user selects the apps to block.
- NetBlocker requests Android's normal VPN approval dialog.
- The VPN configuration uses an **allowed-app list containing only the selected packages**. Android routes only those selected apps into this local VPN; apps not selected continue to use the device's normal network.
- NetBlocker reads and discards packets received from the selected apps. It does not forward or inspect their payload contents.
- IPv4 and IPv6 default routes are configured to avoid leaving a second IP family open when the device supports it.

The per-app routing behavior is provided by `VpnService.Builder.addAllowedApplication()`. Android requires the app list to be set before the VPN interface is established; changing selections rebuilds the VPN interface.

## Build

The GitHub Actions workflow is `.github/workflows/build-netblocker.yml`. It builds `NetBlocker/app` and uploads an artifact named `NetBlocker-debug-APK`.

- Application ID: `id.wawan.netblocker`
- Version: `1.0` (`versionCode 1`)
- Minimum Android: 8.0 (API 26)
- Target SDK: 35
- Java: 17
- Gradle: 8.9
- Android Gradle Plugin: 8.7.3

## Use

1. Install the APK.
2. Open NetBlocker and switch ON the apps whose internet access should be blocked.
3. Press **Aktifkan Firewall** and approve Android's VPN connection prompt.
4. The persistent notification indicates that the firewall is active. Press **Matikan Firewall** to restore normal access.
5. While active, changing an app switch rebuilds the VPN configuration.

## Important limitations

- Android permits only one active VPN connection per user/profile. NetBlocker cannot run alongside another VPN app or a VPN-based ad blocker at the same time.
- The firewall is active only while the VPN service is connected. Reopen the app and activate it again after a reboot; this starter version does not enable always-on VPN automatically.
- Apps with no launcher icon are not shown in the picker. Some shared system processes or services may not map cleanly to one visible app entry.
- App-specific network behavior, OEM customizations, and system-owned traffic can differ. Test each target app on the device before relying on it.
- Blocking network access does not erase data already downloaded, cached, or stored offline. Some apps may continue showing cached content.
- `QUERY_ALL_PACKAGES` is used because selecting arbitrary installed apps is the core purpose of this app. Google Play distribution requires the appropriate declaration and policy review.

## Privacy

The project contains no analytics SDK, advertisements, remote endpoint, or network-forwarding server. The VPN interface exists only on the device and drops packets from selected apps. The app does not declare the Android `INTERNET` permission.
