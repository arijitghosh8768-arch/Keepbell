# Manifest Implementation Notes

This is a checklist, not a complete manifest. Have the AI verify exact declarations against the current Android API documentation and the chosen target SDK.

- Declare a service extending `NotificationListenerService`.
- Use the platform-required service binding permission.
- Add the notification-listener service intent filter required by Android.
- Do not add `INTERNET` permission in the local-only MVP.
- Do not add SMS, contacts, call-log, location, accessibility, or broad storage permissions.
- Notification access is enabled by the user through Android system settings; it is not requested like a normal runtime permission.
- If the app later posts its own notifications, evaluate any separate notification-posting permission and avoid self-capture loops.
- Keep exported status and component exposure aligned with the platform's service requirements.
