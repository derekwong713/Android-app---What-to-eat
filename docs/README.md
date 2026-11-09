# App documentation website

Buildless Traditional Chinese website, designed for mobile/desktop and light/dark mode with the app's Morandi palette. No external fonts, analytics or API keys. The optional mascot player uses a small local script with reduced-motion support; all other content works without JavaScript.

- `index.html`: guest flow, collapsible cuisine and locality filters, automatic unlimited distance, settings, FAQs and the signed APK download link.
- `data.html`: catalogue metrics, the nine restored places, complete district/locality listing, repeatable import commands, verification limits and source attribution.
- `mascot.html`: native sprite integration, a player for all 13 theme groups and 3 extra clips, original costume examples, reduced motion and offline behavior.
- `assets/`: four original costume PNGs plus 16 approved animation atlases copied from App resources. The player loads one selected atlas at a time; no image generation or credentials needed to regenerate the site.
- `motion-preview.js`: one-at-a-time canvas preview of approved eight-frame clips, with play/pause and reduced-motion support. No restaurant draw or permission requests.
- `catalog-import-report.md`: detailed source report; the website links to GitHub's Markdown renderer.

Regenerate all three HTML pages after a catalogue, locality or mascot change:

    python tools/generate_docs.py --date 2026-11-09

The generator reads the current bundled asset and reviewed locality configuration. Verification counts in its text must be updated only after actual checks. Raw gosom exports remain local and ignored by Git; public source checkouts need their own input files to rerun an import.

GitHub Pages: branch `main`, folder `/docs`, deploy from a branch. Keep `.nojekyll` so the static files are served unchanged. Local preview:

    python -m http.server 8765 --bind 127.0.0.1 --directory docs

Then open http://127.0.0.1:8765/. The APK download points to the signed asset on GitHub Releases.
