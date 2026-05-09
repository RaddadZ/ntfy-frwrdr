# Privacy & Permissions

## Data Handling Summary

Notify Forwarder does not collect, store, or transmit any data to third parties. The only outbound network traffic is HTTP/HTTPS to the ntfy server URL that the user explicitly configures.

| Data | Destination | Condition |
|------|------------|-----------|
| Notification title + body | User-configured ntfy server only | App is in the forwarding list |
| Incoming caller number/name | User-configured ntfy server only | Call forwarding is enabled |
| OTP / verification codes | Redacted or omitted (user's choice) | Configurable in Settings |
| App list (installed apps) | Never leaves the device | Used only for the in-app picker |
| ntfy server URL, topic, access token | Stored on-device with AES-256-GCM | Never transmitted |

No analytics SDK, crash reporter, ad network, or third-party service is present.

## Permissions

### `INTERNET`
**Why:** Required to send HTTP/HTTPS POST requests to the user-configured ntfy server. No other network use exists.

### `READ_PHONE_STATE`
**Why:** Required by Android to identify incoming calls before they are answered. Used only when the "Forward incoming calls" feature is enabled. If the feature is disabled, this permission is never exercised.

### `READ_CALL_LOG`
**Why:** Required by Android's `CallScreeningService` API to resolve call details (number, contact). Used only when call forwarding is enabled. The app never stores or exports call log data — it is used transiently to compose the forwarded ntfy message.

### `READ_CONTACTS`
**Why:** Optional. Used to resolve an incoming caller's name from the device contact list before forwarding the ntfy notification. The user can enable call forwarding without granting this permission; the forwarded message will then show the phone number instead of a name.

### `POST_NOTIFICATIONS`
**Why:** Required on Android 13+ (API 33) to show the persistent foreground-service notification that keeps the NotificationListenerService alive. The app does not send promotional or unsolicited notifications.

### `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
**Why:** The app's core function — listening for notifications 24/7 — requires the `NotificationListenerService` to remain running in the background. Without this permission, Android may kill the service during battery-optimization cycles, causing missed forwards. The user is prompted to grant this in the Settings screen and can revoke it at any time.

## Network Endpoints

The only network endpoint is the ntfy server URL entered by the user. The app makes no connection to any hardcoded server or third-party service.

## Accounts

No account or registration is required. The app is fully functional with only a self-hosted ntfy server.

## Self-Update

The app has no self-update mechanism. Updates are delivered through the distribution channel only (GitHub Releases, F-Droid, or IzzyOnDroid).
