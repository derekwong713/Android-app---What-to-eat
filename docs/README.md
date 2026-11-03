# App documentation website

Buildless Traditional Chinese website, designed for mobile/desktop and light/dark mode with the app's Morandi palette. No scripts, external fonts, analytics or API keys.

- `index.html`: guest flow, four filters, automatic unlimited distance, localities, settings and FAQs.
- `data.html`: catalogue metrics, the nine restored places, complete district/locality listing, repeatable import commands, verification limits and source attribution.
- `mascot.html`: homepage/wheel/result appearances, three costume examples, all 13 theme groups, reduced motion and offline behavior.
- `assets/`: four original transparent mascot PNGs copied from App resources by the generator; no image generation or credentials needed to regenerate the site.
- `catalog-import-report.md`: detailed source report; the website links to GitHub's Markdown renderer.

Regenerate all three HTML pages after a catalogue, locality or mascot change:

    python tools/generate_docs.py --date 2026-11-03

The generator reads the current bundled asset and reviewed locality configuration. Verification counts in its text must be updated only after actual checks. Raw gosom exports remain local and ignored by Git; public source checkouts need their own input files to rerun an import.

GitHub Pages: branch `main`, folder `/docs`, deploy from a branch. Keep `.nojekyll` so the static files are served unchanged. Local preview:

    python -m http.server 8765 --bind 127.0.0.1 --directory docs

Then open http://127.0.0.1:8765/. This is documentation, not a finalized legal privacy policy; it does not publish APKs, production releases, billing resources or credentials.
