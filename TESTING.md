# Manual test checklist (core build)

Run on at least one OPPO/ColorOS phone, one other brand, and one emulator. Tick each when verified.

Activation
- [ ] Enable ProKey in Android keyboard list; select it from the picker
- [ ] Main screen status text updates (not enabled / not selected / ready)
- [ ] Globe key switches to the next keyboard; settings key opens ProKey settings

Mobile mode (try Notes, a browser address bar, a chat app, a multi-line field)
- [ ] Every letter a-z and A-Z; every digit; every symbol on both `?123` pages
- [ ] Shift one-shot, double-tap caps lock, auto-capitalization at sentence start
- [ ] Backspace (tap and hold), Enter (newline vs Go/Search/Send), Space, double-space period
- [ ] Spacebar swipe moves the cursor; fast two-thumb typing drops no keys
- [ ] Number/phone fields open on the symbols page; password fields type correctly

PC mode
- [ ] Every key in compact (portrait) and full (landscape) layouts, with and without function row and numpad
- [ ] Ctrl+A/C/X/V, Ctrl+Z, Shift+arrows selection, Alt/Win flags in a terminal-type app
- [ ] F1-F12, Esc, Tab, Home, End, PgUp, PgDn, Ins, Del, arrows (note which apps react)
- [ ] Caps Lock, Fn toggle, ABC/PC switch

Settings and look
- [ ] All 15 themes readable (text vs key contrast), in portrait and landscape
- [ ] Height, spacing, radius sliders update the open keyboard live
- [ ] Key sound (with system touch sounds on), volume, vibration on/off and intensity
- [ ] Rotation while the keyboard is open

Release
- [ ] Install over an older build (upgrade), signature verifies, no INTERNET permission in the APK manifest
