# Family Home 1.3 confirmation controls

The child duration screen has one centered Start session button with initial focus.
The remote Back key leaves the screen without starting a session.

Switch profile asks a question before it returns to the profile chooser.
End session and Switch profile dialogs have one centered Continue button.
Back dismisses the dialog and preserves the current session.

End session keeps the existing parent PIN protection.
PIN dialogs start on digit 1 and disable Continue until the user enters four digits.
An incorrect PIN keeps the dialog open and shows the existing error message.
Switch profile keeps its existing access policy and does not require a PIN.

Version 1.3 uses version code 4.
The change affects presentation and its remote tests.
The controller, session timer, storage, Home role, accessibility helper, and Cast integration remain unchanged.

## Local evidence

The new remote tests failed against the previous controls before implementation.
The final Google TV emulator run passes all 17 tests: 10 remote tests and seven presentation tests.
The tests use directional keys and Android Back, without forced destination focus.
They cover button centering, initial focus, Back dismissal, session preservation, profile switching, session ending, and PIN rejection and acceptance.

All 32 unit tests pass in both debug and release builds.
Android lint reports zero errors and six existing advisory results.
Release, debug, and Android test APK builds pass.
A fresh independent source review reports no material findings.

The coordinator inspected the centered Start session control in an emulator capture.
Physical installation remains pending while the TV shows an active game.
These results do not establish playback, wake, or Cast behavior.
