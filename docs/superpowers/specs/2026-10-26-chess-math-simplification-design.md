# Chess and math simplification

The parent directly requested this change and authorized implementation. Keep the native Java Android app and current forest and ivory artwork.

Buttons use one continuous rounded focus outline. Remove decorative corner strokes. Board focus, selection, legal destinations, and captures use coherent forest, ivory, and sage colors. Preserve clear state differences and contrast on both square colors.

Math uses a smaller keypad and fewer instructions. Keep digits, delete, and an optional Hint action. Remove OK and Next actions. An exact answer marks the exercise correct once. Show brief success feedback, then advance automatically after 1200 milliseconds of visible active time. Preserve the existing five-minute and minimum-three-correct phase rules. Partial and wrong entries remain editable. Background, pause, restore, and storage failures must not skip exercises or double-count answers.

Promotion shows each actual piece image above its Dutch name. Keep all four choices, remote navigation, Back cancellation, and content descriptions.

The chosen approach reuses native views, the existing atlas, and controller timing. A new rendering framework or new artwork is unnecessary. No additional dependency, network permission, or saved-session schema is required.
