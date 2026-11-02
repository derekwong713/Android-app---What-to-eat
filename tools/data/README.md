# Hong Kong administrative district boundaries

Provider: Home Affairs Department, Government of the Hong Kong SAR.
Downloaded for offline import on 2026-11-03. Coordinates are longitude / latitude; the original 18-feature GeoJSON is retained unchanged.

- Dataset and usage information: https://data.gov.hk/en-data/dataset/hk-had-json1-hong-kong-administrative-boundaries
- Source: https://www.had.gov.hk/psi/hong-kong-administrative-boundaries/hksar_18_district_boundary.json
- Specification: https://www.had.gov.hk/datagovhk/dataspec/en/dataspec_district_boundary_en.pdf

Only the development import tool reads this file; the Android app receives district IDs in its bundled catalogue. No Google Geocoding / Places API calls or runtime downloads are used. Exact boundary points, overlapping matches and points outside the dataset remain unknown. District accuracy depends on source restaurant coordinate accuracy and this boundary snapshot. Refresh deliberately from the source and rerun tests/import when the dataset changes.

SHA256: ab2123b5a48c2e39e29ce594a0c51590cf6081922ab2c5c14ff78338b788f760
