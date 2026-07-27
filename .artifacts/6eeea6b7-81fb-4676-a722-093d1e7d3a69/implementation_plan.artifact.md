# Implementation Plan - Re-opening Initial Configuration

Add a way to access the initial server configuration screen (`ConfigActivity`) from the Admin area (`AdminActivity`).

## Research Findings

1.  **Initial Screen**: The initial configuration screen is [ConfigActivity.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/ConfigActivity.java).
2.  **Access Trigger**: Currently, [MainActivity.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/MainActivity.java) only opens it if the API URL is empty.
3.  **Fields**: `ConfigActivity` allows editing the API URL, Client ID, Client Secret, Username, and Password.
4.  **Admin Area**: The [AdminActivity.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/AdminActivity.java) handles the settings screen shown in the user's evidence.

## Proposed Changes

### [Admin]

#### [MODIFY] [activity_admin.xml](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/res/layout/activity_admin.xml)
- Add a new button `btnServerConfig` before the "Salvar e Sair" button.
- Label: "CONFIGURAÇÃO INICIAL SERVIDOR".
- Style: `Widget.MaterialComponents.Button.OutlinedButton`.

#### [MODIFY] [AdminActivity.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/AdminActivity.java)
- Initialize the new `btnServerConfig` button.
- Set a click listener to start `ConfigActivity`.

## Verification Plan

### Automated Tests
- Build the project using `./gradlew :app:assembleDebug`.

### Manual Verification
1.  Open the Admin screen (long press on the top-left trigger and enter password).
2.  Verify the new button "CONFIGURAÇÃO INICIAL SERVIDOR" is present.
3.  Click the button and confirm it opens the `ConfigActivity`.
4.  Verify that existing settings are correctly loaded in the fields.
5.  Change a setting (e.g., API URL) and save to verify it updates in the session.
