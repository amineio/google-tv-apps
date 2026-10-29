# Chromecast with Google TV (4K)

This profile describes the repository's physical Chromecast target.
Published capabilities and the observed device state are separate evidence.
The observations below come from read-only ADB queries on 2026-10-29. No device configuration changed during these queries.

## Published hardware

Google lists 2 GB of RAM and 8 GB of nominal storage for Chromecast with Google TV (4K).
These capacities include system use. They are not an app memory budget or free storage measurement.
See [Google's device comparison](https://store.google.com/magazine/google_tv_streamer_compare?hl=en-US).

The device supports output up to 4K at 60 frames per second through HDMI 2.0 with ALLM, an automatic low-latency mode.
Supported HDR formats are Dolby Vision, HDR10, HDR10+, and HLG.
Connectivity includes Wi-Fi 802.11ac on 2.4 GHz and 5 GHz, Bluetooth, HDMI output, and USB-C for power and data.
Ethernet requires an optional power adapter. The supplied power rating is 5 V, 1.5 A.
See [Google's device-specific specifications](https://support.google.com/chromecast/answer/3046409?hl=en).

Google lists Dolby Digital, Dolby Digital Plus, and Dolby Atmos passthrough over HDMI.
Passthrough forwards audio to compatible equipment.
See [Google's hardware specifications](https://support.google.com/chromecast/answer/12578818?hl=en).

## Observed device state: 2026-10-29

| Item | Observation |
| --- | --- |
| Identity | Manufacturer `Google`, model `Chromecast`, device `sabrina`, product `sabrina_prod_stable`. |
| System chip | Amlogic `AMLS905D3`, as reported by Android properties. |
| CPU | Four online ARMv8 cores. CPU IDs `0x41` / `0xd05` identify Cortex-A55 through the [Arm reference](https://documentation-service.arm.com/static/5e7e1405b471823cb9de57ae). Reported maximum frequency: 1,908,000 kHz (1.908 GHz). |
| Graphics | ARM Mali-G31, OpenGL ES 3.2. |
| Firmware | Android 14, API level 34. Security patch `2025-10-01`. Build `UTTC.250917.004`, incremental `14193937`. |
| App ABI | `armeabi-v7a,armeabi`. The 64-bit ABI list is empty. An ABI defines binary compatibility for native libraries. |
| Kernel memory | `MemTotal: 1982996 kB`, about 1.89 GiB. This differs from the nominal 2 GB hardware capacity. |
| App data filesystem | `/data` totals 4,245,768 blocks of 1,024 bytes, about 4.05 GiB. This differs from nominal 8 GB storage. |
| Android base display | Physical size `1920x1080`, density `320`, as reported by `wm`. |
| Build access | Production `user` build, `ro.debuggable=0`, `ro.secure=1`. Developer options provide ADB shell access, not root privileges. |

The CPU frequency is a reported ceiling, not a sustained performance measurement.
The Android base display size does not establish the HDMI output mode or native 4K rendering for every interface.
Firmware updates can change these observations.

Family Home `1.9` (version code `10`) held the Home role.
The Home intent resolver selected `com.google.android.apps.tv.launcherx/.home.HomeActivity`.
Both Google TV Home and `com.google.android.tungsten.setupwraith` were enabled.
Keep both packages enabled for Cast credentials.
Read [Home key routing](../family-home/home-key-routing.md) and the [Family Home guide](../../apps/family-home/README.md) before changing this configuration.

## Development implications

Keep bundled assets small. Limit decoded image dimensions and retain only the images that the current screen needs.
The RAM and storage capacities do not establish a safe per-app allocation or a speed guarantee.
Include `armeabi-v7a` when shipping native libraries for this firmware.
The CPU's 64-bit capability does not provide a 64-bit app ABI on this installation.

Use the Google TV emulator for routine development and delivery.
Use the physical target only when the user requests it or reports an issue.
Published capabilities and emulator results do not establish physical playback, audio focus, Cast, wake behavior, or audible quality.

## Refresh the observations

For an authorized device inspection, set `TV_SERIAL` to the connected ADB address.
These commands read selected fields without changing configuration:

```sh
TV_SERIAL='<connected address>'
adb -s "$TV_SERIAL" shell getprop ro.build.id
adb -s "$TV_SERIAL" shell getprop ro.build.version.security_patch
adb -s "$TV_SERIAL" shell getprop ro.product.cpu.abilist
adb -s "$TV_SERIAL" shell getprop ro.soc.model
adb -s "$TV_SERIAL" shell 'head -n 1 /proc/meminfo'
adb -s "$TV_SERIAL" shell cat /sys/devices/system/cpu/online
adb -s "$TV_SERIAL" shell cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq
adb -s "$TV_SERIAL" shell df -kP /data
adb -s "$TV_SERIAL" shell wm size
adb -s "$TV_SERIAL" shell wm density
```

Record the inspection date. Keep device addresses, account data, and raw dumps out of Git.
