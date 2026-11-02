# Current district assignment — official coordinate boundaries

The nine previously unassigned localities are now back under their matching district lists. Import uses tools/district_boundaries.py and the pinned Home Affairs Department boundary GeoJSON in tools/data/. Restaurant coordinates determine district; source city determines locality. These are independent fields. All 2,665 current records match a district. Exact boundaries or unmatched coordinates stay unknown in future imports; coordinate/source quality can still be imperfect.

Run this repeatable import from the project root:

    python tools/review_gosom_catalog.py gosom-data/results.json gosom-data/results-hk-20261103.json --date 2026-11-03 --sync-localities

The --sync-localities flag also refreshes tools/hk-localities.json display memberships and generated LocalityOptions.kt from the resulting records. A locality may display under multiple districts, with the label 多區資料. Selecting it includes all its candidate restaurants. Selecting only one of its parent districts must not erase an explicit multi-district locality selection; overlapping records remain deduplicated. Source: https://data.gov.hk/en-data/dataset/hk-had-json1-hong-kong-administrative-boundaries . Attribution and boundary snapshot checksum: tools/data/README.md. No network or Google Maps API calls occur during app use or repeat imports.

Earlier notes below are retained as history; their city-to-district mapping and unknown-district counts are superseded.

---

# Current catalogue — 2026-11-03

The current bundled asset contains 2,665 restaurants, of which 2,376 are drawable across 48 cuisines. Full counts, exclusions, limitations and repeatable import commands: [import report](docs/catalog-import-report.md). Raw exports and full review lists remain local under ignored gosom-data/. Run tools/review_gosom_catalog.py to merge and review batches before replacing the asset; the single-file converter below does not perform Hong Kong/food-category screening.

The following notes preserve the earlier single-file workflow and import history; their earlier counts are superseded by the report above.

# Offline bundled catalogue

Both app flavors now read `app/src/main/assets/restaurants.json`, with no Supabase or authentication requests. Existing Room favorites/history, filters, fair draw engine and ads remain. Community writes/comments and account controls are removed from the screens. The 2026-11-02 asset contains 16 records from the local Mong Kok ramen search: 15 source-classified Ramen restaurant records map to ramen / 拉麵; 1 Chop bar record remains unclassified and is excluded from draws. This is not a Hong Kong-wide inventory.

Export JSON or JSONL using gosom/google-maps-scraper on your computer. This project does not run a scraper on user devices. Check permission to redistribute the source data before shipping it.

Run:

    python tools/prepare_bundled_catalog.py export.json --date 2026-11-02

Then rebuild the Android APK. No server key or Supabase project is needed. The converter retains title, address, coordinates, rating/count and source date; discards review bodies and personal reviewer details; deduplicates by cid/link, never name. Conflicting duplicate IDs fail explicitly. Update data by repeating conversion and distributing a new APK.

Add `confirmed_cuisines` to each export row as an array such as `[{"id":"noodles","label":"粉麵"}]`. Cuisine is deliberately not guessed from restaurant names or generic categories. Unclassified rows are not eligible for the two-stage draw. Coordinates are needed for distance filters and precise navigation. Unknown hours/status are not treated as closed. Inspect and mark known closed entries in the resulting asset before shipping.

Legacy Supabase implementation and migrations remain for reference but are not selected by dependency injection. Existing community instrumentation tests describe the previous online product and need replacement for this product direction. Privacy/terms pages also need operator review before release.

## Reviewed import (2026-11-02)

    python tools/prepare_bundled_catalog.py gosom-data/results.json --date 2026-11-02 --category-map tools/gosom-cuisine-map.json

Only exact categories in the reviewed mapping supply cuisine, never names or search queries. Explicit confirmed_cuisines takes precedence. Ratings are source snapshots. Source status values contain descriptions rather than reliable operating states, so status and hours remain unknown. Review bodies and personal fields are discarded. Raw exports remain ignored. Only one cuisine currently exists.

## Google Maps restaurant details

The importer preserves each source Google Maps place link in maps_url, removing tracking query parameters. The new restaurant-details button opens this link; the separate navigation button has been removed; users can choose directions within Google Maps. The model/JSON codec retains the link across saved draws. Older records without the link fall back to a name-and-address Maps search, which may require selecting the correct listing. No Places API or API key is used. Android builds attempted on 2026-11-03 still stop at the environment error Unable to establish loopback connection; navigation instrumentation tests were added but could not run here.

## Rating and region filters (2026-11-03)

Filters now include minimum rating (unlimited / 3.0 / 3.5 / 4.0 / 4.5 / 5.0) and multiple regions (Hong Kong Island, Kowloon, New Territories, unknown). Regions use exact source complete_address.state values, not query strings or name guesses. This is a three-region grouping, not the 18 administrative districts; unclear values stay unknown. Unrated restaurants only pass an unlimited rating filter. All four filters intersect before snapshot creation; rerolls reuse that snapshot. Preferences persist both new fields, with unrestricted defaults for existing installs. Account navigation is replaced with Settings to retain advertising consent/privacy and local-data controls.

## 18-district refinement (2026-11-03)

The region selector now offers the 18 districts grouped under Hong Kong Island / Kowloon / New Territories, plus unknown, with multi-select and group select/clear controls. Source complete_address.city is matched exactly (case-insensitive) against the reviewed tools/hk-districts.json locality table. Restaurant names, queries and generic state fields do not infer a district. Ambiguous city labels such as Causeway Bay, Lantau Island, Prince Edward and Mid-Levels stay unknown; this is a locality mapping, not an official polygon boundary calculation. District names/groups follow https://www.had.gov.hk/en/18_districts/my_map.htm . Old saved broad-region preferences expand into the corresponding districts on read, rather than silently becoming unrestricted. Previous three-region instructions are superseded.

## Locality and distance update — Morandi design

The selector now includes 93 named localities in addition to the 18 administrative districts. tools/hk-localities.json maps exact source city values to canonical labels and stable area_ IDs; LocalityOptions.kt mirrors these labels/parents. Administrative districts and source localities remain separate: Causeway Bay can be selected as a locality without guessing its administrative district. Of 2,665 records, 2,558 have a mapped locality; others remain selectable via their district or unrestricted selection. Selecting a whole district includes all its records; multiple districts/localities form a union, intersected with cuisine and minimum rating.

Any nonempty region/locality selection forces unlimited distance. Filter UI resets radius immediately and disables finite distance choices; preferences and ViewModel normalize the radius, and CandidateFilter uses effectiveRadius as a final safeguard. No current-location permission is needed for area-only draws. Clearing the region selection leaves distance unlimited until explicitly changed.

The app uses a static Morandi palette (muted sage, dusty rose, blue-grey and warm stone), including light/dark themes, wheel, launcher and system bars. Dynamic wallpaper colours do not override the palette.
