# Android phone sign-in concept

Status: generated design reference only, 2026-10-30. This image is not a native capture or authentication evidence.

The concept shows a TV pairing page and a separate Android helper page. The QR panel is a nonfunctional placeholder. It contains no host, token, account, or credential.

## Image record

- Tool: built-in ImageGen; no CLI fallback.
- Use case: `ui-mockup`.
- Output: `exec-ece2ffd0-bca1-47a6-b289-7f90d51eb54c.png`.
- Private location: `$CODEX_HOME/generated_images/01a18e2f-833e-70a1-bc91-7cf8d93ff493/`.
- Actual dimensions: 1672 × 941 pixels, opaque PNG. The prompt requested a 2560 × 1440 sheet.
- Layout: landscape TV reference with a separate portrait Android helper.
- Repository treatment: preview only. Keep the generated bitmap and source capture outside Git. Do not bundle the bitmap as application UI.

## Sources and rights

The source reference was the private, generic emulator capture `emulator-ai-chat-ready-review.png`. It was inspected for AI Chat colors, spacing, controls, and typography. Its pixels were not supplied as an edit target.

The repository root README and `apps/ask-tv/README.md` supplied app context. The concept follows the existing nearblack, ivory, and gold palette. The existing Outfit font retains its SIL Open Font License. The generated image approximates that typography; it does not embed a new font file.

ImageGen created the bitmap from the prompt below. No external stock image or third-party raster insert was used. No separate stock license or exclusivity claim applies. The concept is a private project reference. Existing app names and ChatGPT text retain their respective ownership.

## Implementation limits

Build the interface with native Compose controls. Generate the real QR in code from the active pairing session. Preserve the existing native AI Chat icon; the generated icon is only an approximation.

The planned flow uses the Android app's localhost callback and a pinned local TLS relay. This is an app implementation assumption, not an official device-code grant or provider endorsement. The concept does not establish certificate trust, account binding, sign-in success, or expiry behavior.

Keep large controls and the lower-right Clock area clear in native captures. The generated waiting status sits under the QR card; confirm its final placement against the Clock area during native layout validation.

The user requires independent UX review of real emulator captures only. This concept was not submitted for that review. No build, device, browser, or authentication test was performed for this concept task.

## Exact generation prompt

```text
Use case: ui-mockup
Asset type: PRIVATE preview-only concept sheet, not a shipped raster interface.
Primary request: Create one polished, calm design reference for AI Chat sign-in with an Android phone. Show two distinct flat native UI screen mockups on a plain charcoal presentation sheet: a large landscape 16:9 Google TV screen on the left, and a separate portrait Android helper screen on the right. Make the TV screen the main view, approximately three quarters of the sheet width. Keep the full TV aspect ratio visibly 16:9, not stretched or cropped. No photorealistic physical devices, no perspective, no shadows pretending this is a real screenshot.
Reference style: The existing AI Chat native TV UI has nearblack background #0D0E12, ivory text #F8F5F0, warm gold accent #F6CD83, rounded dark-grey controls, generous padding, Outfit geometric sans-serif text, and a tiny gold two-chat-bubble outline beside the AI Chat title. A restrained soft gold eclipse glow may sit on the far right, but the sign-in content must remain very legible.
TV composition: AI Chat header at upper left. Large left column title “Sign in with your phone”. Below it two concise lines: “Open AI Chat on your Android phone.” and “Scan this QR code to connect.” Add small supporting copy “Keep both devices on the same Wi-Fi.” On the central-right part of the TV view show a generous white QR placeholder card with clearly nonfunctional schematic corner brackets, a simple central scan icon, and exact words “QR PREVIEW”. It must not contain any functional QR code or dense QR-like random pattern. Below the placeholder show a small gold status dot and “Waiting for your phone”. At the lower left show a large native rounded focusable Cancel button with gold focus outline, and beside or below it a small secondary outlined button “Use a Mac”. A minimal bottom-left helper reads “OK Select · Back Cancel”. Leave the lower-right 25 percent of the TV view empty for the separate Clock overlay. No instructions, controls, status, or QR panel in that lower-right area.
Android helper composition: A flat portrait screen with AI Chat header, small lock outline icon, heading “Trusted TV connection”, concise text “Continue to connect AI Chat to your TV.”, one prominent gold native rounded button “Continue with ChatGPT”, and secondary text button “Cancel”. Use native Android spacing and readable typography. Do not show a browser address bar, account avatar, personal account, consent details, localhost address, real host, device identifier, pairing token, certificate, password, or credential.
Text (verbatim): “AI Chat”; “Sign in with your phone”; “Open AI Chat on your Android phone.”; “Scan this QR code to connect.”; “Keep both devices on the same Wi-Fi.”; “QR PREVIEW”; “Waiting for your phone”; “Cancel”; “Use a Mac”; “OK Select · Back Cancel”; “Trusted TV connection”; “Continue to connect AI Chat to your TV.”; “Continue with ChatGPT”.
Constraints: Clean, large TV controls readable from a sofa; spacious visual hierarchy; high contrast; portrait helper must be separate from TV view. This is a local app pairing concept, not an official device-code grant. Do not add OpenAI or Google logos, store badges, marketing copy, progress percentages, fake success states, QR-scanning claims, or unrelated screens. Small sheet labels “TV” and “ANDROID HELPER” may sit outside their mockups. Final app will use native Compose controls and a QR generated in code; this image is a design reference only.
Output: wide 16:9 concept sheet, target 2560×1440, opaque background.
```

## Additional TV polish preview

Status: private design reference only, 2026-10-30. This preview is not a native capture, UX review, runtime result, or bundled asset.
The existing bundled artwork is unchanged.
The preview shows three complete TV screens: signed-in home, answer reading, and phone sign-in.
Its QR is nonfunctional and contains no live pairing data.

ImageGen created `exec-4c6955bb-c3fa-4355-a200-36f34046b701.png` with the built-in image tool and imagegen skill.
The requested dimensions were 1536 × 2592 pixels. The actual output is 965 × 1629 pixels.
Each of its three screens is approximately 965 × 543 pixels.
Keep the generated raster and source captures outside Git.

The source references were private native emulator captures `isolated-flow-09-example.png` and `isolated-flow-01-ready.png`.
They supplied colors, typography, eclipse artwork, controls, and layout through visual inspection.
The generation used a text description. The source captures were not attached to ImageGen.
ImageGen introduced no third-party raster asset. This is an AI-generated project preview, with no exclusivity claim.
Existing app artwork and the Outfit font license remain the implementation references.

Use native Compose controls and the real QR from the active session.
The preview's Mac instruction mentions Configuration, but the native sign-in panel offers a direct **Use Mac** action.
Do not copy that inconsistent instruction.
Do not use generated helper text as a substitute for native readability, focus, navigation, or state-transition checks.
The independent UX review uses actual native captures, not this preview.

### Exact additional generation prompt

```text
Use case: ui-mockup.
Asset type: preview-only concept board for a native Android TV Compose app called AI Chat.
Create one polished high-fidelity board containing THREE complete 16:9 TV screens stacked vertically, equal size, edge-to-edge with thin neutral separators. Target image 1536 x 2592 pixels. Every screen must look like the SAME native app, no device frames, no perspective, full-frame composition, safe margins about 5.5%.
Visual source to preserve from current real app: near-black charcoal background #0B0D10; charcoal opaque rounded buttons #23242B with neutral thin edges; off-white text; muted light gray supporting text; warm pale gold #F7CF7F for focus only. Outfit-like geometric, friendly readable sans typography. Small outline double-chat icon left of AI Chat header. Restrained amber eclipse artwork: very large almost-black planet circle entering from right edge, thin muted amber upper-left arc and soft amber halo, never behind body reading text. No glass, no drop-shadow overload, no neon. Use simple native Compose-feasible controls.
TOP SCREEN: signed-in Ready home. Header AI Chat upper left, small compact neutral connected badge saying "Connected", Configuration neutral button at upper right. Main left heading "What are you\ncurious about?" in large friendly off-white type. Supporting line "Ask a question. Hear the answer." Under it ONE prominent focused pale gold button "Ask a question" with black text; adjacent secondary charcoal button "Type a question", no gold border. Small helper "Microphone starts when you ask". Two low-emphasis neutral example cards "Why is the sky blue?" and "A simple dinner idea" below under small "TRY AN EXAMPLE". Footer "OK Select · Back Exit". Do not include Sign in action on this signed-in screen. Leave generous breathing room and art on right.
MIDDLE SCREEN: answer view substantially improved for reading. Compact header AI Chat left, small Connected badge and neutral Configuration button right. Compact one-line question "Why is the sky blue?" below header, followed immediately by a much TALLER reading panel occupying about 58% of screen height. Reading panel width about 72% screen width, intentionally shorter lines, left aligned, substantial body font and 1.4 line spacing. Use four short paragraphs: "Sunlight looks white, but it contains many colors." "As sunlight enters Earth's atmosphere, tiny gas molecules scatter it in all directions." "Blue light has a shorter wavelength than red light, so the air scatters blue light more strongly." "Some of that scattered blue light reaches your eyes from every part of the sky." The answer reading panel is the ONLY focused target: thin pale gold outline, with discreet scrollbar at right. Keep all text crisp and contained. Under reading panel compact neutral buttons "Read aloud", "Follow-up", "New conversation". Footer "Up / Down Scroll · Back Return". Artwork stays dim and on right, with no decorative content disrupting reading.
BOTTOM SCREEN: dedicated phone sign-in page, same frame and visual system. Header AI Chat upper left. Large heading "Sign in with your phone". Left column has large white square QR PLACEHOLDER with a simple generic QR-like pattern, non-functional and no encoded real URL. Label "Scan with your phone". Right column has short readable ordered instructions with generous spacing: "1  Connect your phone to the same Wi-Fi as this TV." "2  Use an Android phone with Android 14 or later." "3  Scan the code and follow the steps on your phone." Below in normal supporting size "Using a Mac? Open Configuration for the Mac option." At bottom left one focused pale gold button "Cancel"; next to it neutral charcoal button "Use a Mac". Footer "Back Cancel". No pretend account, email, token, device IP, actual URL, fine print or camera illustration.
Focus rule: exactly ONE gold focus target on each screen: Ask a question, answer reading panel, and Cancel respectively. All unfocused controls use neutral edges. Never add gold borders to unfocused controls. Maintain stable full-frame background and safe margins across all panels. No annotation arrows, no additional text outside the screens. This is a design preview, not an actual screenshot or runtime proof.
```
