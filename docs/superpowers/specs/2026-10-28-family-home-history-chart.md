# Usage chart and profile menu

Show daily use in a seven-day bar chart for each profile. Keep the current overview and timing rules. Use Monday through Sunday, current week initially, and today as the initial focus. Show a recorded week total and the exact selected date and duration. Earlier week and Later week controls navigate saved history. Keep weekday selection on week changes and restore state after Back and recreation.

Use the current 30 recorded dates without a storage migration. An absent record means No recorded use, not zero. Future absent dates say Future date. Recorded zero says 0 min. Retained future records remain accessible after a clock correction. Bound navigation by the current week and the profile's earliest and latest retained dates. Use a shared linear bar scale within each week. Bar focus targets have constant size. Do not animate the scale when focus changes.

Keep one weekly chart with daily selection. Separate Day and Week tabs duplicate the same daily totals. Per-app reports and hourly breakdowns require data that the app does not collect. Keep those outside this increment.

Move home actions into the profile picture at the upper right. The profile control opens a compact native TV menu with Switch profile and Family configuration for the parent, or End session for children. Remove the bottom action row. Keep existing PIN and confirmation behavior. Up from an app reaches the profile control. Down returns to the selected app. Back closes the menu and returns focus to the profile control. Return from settings focuses that control. Empty app lists keep the profile menu reachable.

Use native Compose for TV controls and code-drawn chart bars. Keep the countdown stable, app return focus intact, and the lower-right Clock area clear. No new dependencies, permissions, profile records, or network access.

Digital Wellbeing provides daily screen-time reports. Screen Time provides daily and weekly views. YouTube TV uses profile-picture navigation for account switching. These patterns inform the design without copying phone layouts.

Sources:
- https://support.google.com/android/answer/9346420
- https://support.apple.com/en-gb/guide/iphone/iphbfa595995/18.0/ios/18.0
- https://support.google.com/youtube/answer/3015415

The user also requests full retirement of the old Java launcher from the TV. Remove its package globally without retaining data. Remove the retirement backup created in the previous task. Preserve repository source and unrelated apps.
