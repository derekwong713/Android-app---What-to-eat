# Offline bundled catalogue

Both app flavors now read `app/src/main/assets/restaurants.json`, with no Supabase or authentication requests. Existing Room favorites/history, filters, fair draw engine and ads remain. Community writes/comments and account controls are removed from the screens. The asset is intentionally empty: no real export was supplied; no scraped records or fictional records are presented as real.

Export JSON or JSONL using gosom/google-maps-scraper on your computer. This project does not run a scraper on user devices. Check permission to redistribute the source data before shipping it.

Run:

    python tools/prepare_bundled_catalog.py export.json --date 2026-11-02

Then rebuild the Android APK. No server key or Supabase project is needed. The converter retains title, address, coordinates, rating/count and source date; discards review bodies and personal reviewer details; deduplicates by cid/link, never name. Conflicting duplicate IDs fail explicitly. Update data by repeating conversion and distributing a new APK.

Add `confirmed_cuisines` to each export row as an array such as `[{"id":"noodles","label":"粉麵"}]`. Cuisine is deliberately not guessed from restaurant names or generic categories. Unclassified rows are not eligible for the two-stage draw. Coordinates are needed for distance filters and precise navigation. Unknown hours/status are not treated as closed. Inspect and mark known closed entries in the resulting asset before shipping.

Legacy Supabase implementation and migrations remain for reference but are not selected by dependency injection. Existing community instrumentation tests describe the previous online product and need replacement for this product direction. Privacy/terms pages also need operator review before release.
