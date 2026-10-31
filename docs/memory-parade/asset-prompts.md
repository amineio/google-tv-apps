# Memory Parade asset production

All raster art uses the built-in OpenAI ImageGen tool. The supplied composite image provides art direction. No image from the supplied concept is bundled. No external stock image, font, or audio sample is used. The repository license covers the new source code and original synthesis script. Generated images remain subject to the service terms. The service does not provide a separate stock license.

The records in `assets.json` contain the bundled dimensions, source generation identifiers, compression, and SHA-256 hashes. Pillow extracts the six atlas cells, trims transparent margins, resizes the results, and preserves alpha. Opaque plates use WebP quality 88. Animal cutouts use quality 92. The source atlas is 1536 by 1024 pixels. Each cell is 512 by 512 pixels.

## Screen concept prompt

Create a polished 16:9 TV game concept screen for Memory Parade. Native television interface, premium stylized 3D storybook forest, warm sunlight through leaves, distant waterfall, moss and little flowers, rich emerald teal and honey gold. Layout: modest wooden title plaque 'Memory Parade' in upper left, small instruction 'Find the pairs'. Huge central 3 rows by 4 columns matching-card grid, twelve cards, teal leaf card backs and thick soft gold edges. Two face up cards show the same adorable fox. One card has a crisp pale cyan focus outline distinct from gold match glow. Right quarter contains a wooden parade stage with ONE adorable matched fox and five quiet empty spaces, moves counter 3 and pairs 1/6. Clear negative space and strong readable TV text, approximately 5 percent safe margins. Do not copy 3:1 layout, compose for 16:9. Full assembled commercial game screen, not flat mobile app, no extra badges or menus. Board dominates, title secondary, right parade reward zone. No watermark.

## Forest prompt

Use case: stylized-concept. Original production game BACKGROUND PLATE, 1920x1080 landscape 16:9. Premium stylized 3D storybook magical forest matching Memory Parade concept: soft lush moss grass, flowers only at edges, giant tree trunks left and right, warm honey sunlight top center, distant waterfall and misty turquoise valley. Keep broad CENTRAL 60 percent softly blurred, quiet, low detail with deep teal-green midtones for separately rendered matching cards. Right quarter a beautiful wide horizontal mossy wooden bridge at mid-height, empty, for separate animal sprites. Foreground plants frame lower corners. No animals, no cards, no panels, no text, no letters, no UI, no glowing icons, no watermark. Cheerful, cinematic depth, jewel greens, gold warm highlights, soft dimensional commercial casual game quality. Camera straight-on at low forest clearing height.

## Animal atlas prompt

Use case: stylized-concept. One production sprite atlas, transparent background, exact evenly spaced 3 columns by 2 rows, six separate adorable full-body seated baby woodland animal cutouts, no overlap. Top row left to right FOX (orange fluffy fox big tail), PANDA (round black white panda), RABBIT (cream rabbit pink long upright ears). Bottom row left to right OWL (lavender-blue fluffy owl golden eyes), DEER (tan spotted baby deer small ears), BEAR (chocolate brown round bear cub). Every animal forward facing with slight three-quarter turn, seated feet visible, friendly face, large warm eyes, small plump bodies, soft fur, premium stylized 3D animated family game quality. Consistent warm upper-left lighting and proportions, attractive detailed dimensional rendering. Each occupies at most 80 percent of its own equally sized atlas cell with wide transparent gutters around. No ground, no grass, no cast shadow outside sprite, no background, no text, no tile borders. Actual transparent alpha.

## Launcher background prompt

Use case: stylized-concept. Dedicated 16:9 1920x1080 launcher BACKGROUND artwork for Memory Parade. Magical sunlit forest with moss-covered woodland bridge, waterfall behind, warm cinematic light, stylized premium 3D family game. Three adorable small fox panda and rabbit sit on the RIGHT HALF of bridge, friendly eyes and soft fur. LEFT HALF quiet dark forest gradient negative space, LOWER QUARTER dark plants with no subjects, title and app tiles will be rendered separately. No text, no cards, no UI, no symbols, no watermark. Rich emerald, turquoise atmosphere, honey sunlight. Do not create screenshot, banner, icon, or border.

## Icon prompt

Use case: stylized-concept. Square 1:1 original app icon illustration for Memory Parade. Close central adorable orange baby fox smiling with huge warm eyes and fluffy cheek fur holding a single teal playing card with simple embossed gold leaf. Soft dimensional premium 3D casual forest game quality, deep teal green rounded square background, warm gold rim and soft sunlight. Fox face and card silhouette remain recognizable at 64px. Important objects central 70 percent, generous margin, no letters or words, no watermark.

## Banner prompt

Use case: stylized-concept. Original 16:9 TV app banner for Memory Parade. Large clearly readable exact two-line title 'Memory Parade' in warm gold and bright sky blue dimensional storybook lettering on LEFT two thirds, with charming baby fox and lavender owl on RIGHT third. Rich soft magical emerald forest background, warm sunlight, premium stylized 3D family game. Title stays in safe inset 10 percent, readable when downscaled to384x216, clean bold composition, no extra words, no cards, no UI counters, no watermark.

## Native art

Native Compose code draws card geometry, borders, panels, labels, focus outlines, and short particle effects. These elements use gradients, paths, shadows, and transforms. Card and stage skins use separate generated artwork. They contain no externally licensed artwork.

## Audio synthesis

The current Python script creates twelve original mono PCM16 WAV effects at 44.1 kHz. It uses additive, modal, and noise synthesis, DC removal, short entrance/exit fades, and per-effect peak normalization. The exact parameters, playback gains, durations, and hashes are in `audio-upgrade-2026-10-31.json`. SoundPool uses four voices, throttles navigation, gives rewards priority, and lets the finale clear other voices. Mute, pause, and app departure clear voices. No queued sound replays on return.

Audio decoding and event playback do not establish audible quality. The coordinator records listening evidence separately. No music or external audio service is used.

## Runtime review improvements

The first real emulator capture showed that the native leaf backs and two thin shelves did not match the concept's material depth. The replacement assets below preserve the board size. Native code retains borders, focus outlines, labels, and counters. A single connected stage replaces the floating shelves.

Card-back prompt:

Use case stylized-concept. One isolated production MEMORY GAME CARD BACK PLATE viewed perfectly straight-on orthographic, rectangular landscape 1152 by 1024, almost square. Rounded corners. Rich emerald teal softly textured aged leather and stone enamel, beautiful thick dimensional beveled warm GOLD frame, stylized carved vines and tiny leaves around corners. Center is one elegant embossed dimensional cluster of three bright green leaves, central stem. Premium polished 3D casual forest game artwork, warm sunlight top left, subtle ambient occlusion, softly luminous honey rim highlights and deep teal shadow gradient. The card fills canvas, straight aligned edges, no perspective tilt, no shadow outside, no table, no extra cards, no text, no watermark. Thick rounded border with beautifully crafted materials, readable silhouette at 200 pixels. The card is a single raster decorative skin; focus rings will be drawn separately.

Stage prompt:

Use case stylized-concept. One isolated EMPTY woodland PARADE DISPLAY STAGE cutout, real transparent background. Straight-on orthographic view. Shape tall nearly square, 1024 by1024. Beautiful sturdy storybook wooden shelf stage with TWO horizontal platforms, three animal spots each. Shelves at38 percent and85 percent of total image height. Both platforms connected by thick wooden supporting vertical side posts, soft bark grain, round log edges, leafy vines, little white flowers, moss cushions on each shelf, faint golden engraved leaf ornaments. Empty open area above each shelf to place separate baby animal sprites. Top crossbeam at5 percent height has no sign or lettering and does not obstruct characters. Cohesive premium stylized3D family game object, warm upper-left lighting, emerald moss and golden honey wood. No animals, no cards, no text, no labels, no floor or scenery, no background, no watermark. Natural dimensional stage structure connected with supports, not floating bars. Actual alpha background, include entire object with small transparent margin.

A fresh gameplay capture exposed a detached deer-ear fragment below the panda's feet. A deterministic bottom crop removes the fragment. The crop preserves every genuine panda pixel. All six animal sprites received visual inspection. No other sprite contained a detached neighboring fragment. The panda dimensions and hash in `assets.json` reflect the correction.
