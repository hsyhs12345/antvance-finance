# ANTVANCE FINANCE — iOS

This directory is reserved for the iOS/iPadOS version of ANTVANCE FINANCE.

## Planned structure

- `ANTVANCEFinance/` — SwiftUI app source
- `ANTVANCEFinance/Models/` — market and portfolio models
- `ANTVANCEFinance/Services/` — read-only market-data client
- `ANTVANCEFinance/ViewModels/` — screen state and API orchestration
- `ANTVANCEFinance/Views/` — Home, Market, Portfolio, Analysis, More, and Stock Detail screens
- `ANTVANCEFinance/Resources/` — assets and configuration templates
- `ANTVANCEFinanceTests/` — unit tests
- `ANTVANCEFinanceUITests/` — UI tests

## Platform direction

The iOS app will mirror the Android app's current feature set and visual hierarchy where practical. It will use SwiftUI and Apple's native biometric APIs. Trading remains simulation-only; no brokerage account or real-money order API will be added.

Market data will use the same read-only API concept as Android. API credentials should not be committed to this repository.

## Build

Open the eventual `.xcodeproj` or `.xcworkspace` on macOS with Xcode. Apple documents creating iOS projects with SwiftUI and configuring the bundle identifier, signing team, and deployment target in Xcode.
