# 食咩好？ Pig mascot

The approved big-eyed pig wears a sage cape and a roulette apron. The base pose holds a wok and spatula. Fourteen transparent PNG resources comprise this base and thirteen food-themed costumes, grouped across all 48 current cuisine IDs. Related cuisines share a costume; these are illustrations, not claims about a restaurant's menu. Unknown cuisine IDs use the base pose.

Created with the built-in ImageGen tool in reference-image editing mode. `reference-approved.png` preserves the approved concept; `generation.json` records the exact prompt set and project output paths. `cuisine-themes.json` records the theme families. No API credentials are needed to display these bundled images.

Implementation: `CuisineMascot.kt` selects the costume from the drawn cuisine, including multi-cuisine restaurants. `PigMascot.kt` retains original static illustrations. Version 0.3.0 uses the approved [sprite animations](motion/README.md) for the homepage, wheel and results; Android's disabled animator setting uses these original static poses. Draw candidates, probabilities, filters and Google Maps links are unchanged. The earlier sway-only version is superseded by `AnimatedPigMascot.kt`.
