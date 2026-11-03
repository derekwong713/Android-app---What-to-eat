# 食咩好？ Pig mascot

The approved big-eyed pig wears a sage cape and a roulette apron. The base pose holds a wok and spatula. Fourteen transparent PNG resources comprise this base and thirteen food-themed costumes, grouped across all 48 current cuisine IDs. Related cuisines share a costume; these are illustrations, not claims about a restaurant's menu. Unknown cuisine IDs use the base pose.

Created with the built-in ImageGen tool in reference-image editing mode. `reference-approved.png` preserves the approved concept; `generation.json` records the exact prompt set and project output paths. `cuisine-themes.json` records the theme families. No API credentials are needed to display these bundled images.

Implementation: `CuisineMascot.kt` selects the costume from the drawn cuisine, including multi-cuisine restaurants. `PigMascot.kt` decodes sampled transparent bitmaps. Home shows the base pose, the wheel adds a finite gentle sway, and results fade/scale in the selected costume. Android's disabled animator setting suppresses these motions. Draw candidates, probabilities, filters and Google Maps links are unchanged.
