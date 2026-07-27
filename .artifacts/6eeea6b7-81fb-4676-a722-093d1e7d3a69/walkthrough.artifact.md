# Walkthrough - Re-opening Initial Configuration

I have added a button in the Admin area to allow re-opening the initial configuration screen (`ConfigActivity`) to edit server credentials.

## Changes Made

### Admin Area
- **Layout**: Added a new button `btnReopenConfig` in [activity_admin.xml](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/res/layout/activity_admin.xml) with the label "CONFIGURAÇÃO INICIAL SERVIDOR". It is positioned above the "Diagnóstico" button.
- **Logic**: In [AdminActivity.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/AdminActivity.java), I initialized this button and set a click listener that starts the `ConfigActivity`.

### Strings
- Added `@string/btn_reopen_config` in [strings.xml](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/res/values/strings.xml) to store the button label.

## Verification Results

### Automated Tests
- The project was successfully compiled with `:app:assembleDebug`.
- `analyze_file` on `AdminActivity.java` confirmed there are no syntax errors.

### Manual Verification
- The button is correctly integrated into the Admin layout.
- The logic uses the existing `ConfigActivity`, which already handles loading and saving credentials from/to `SessionManager`.
