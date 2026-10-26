# Release checklist

The project is a development MVP. Do not publish the debug APK as a production release.

- [ ] Supply a publisher-owned, publicly accessible privacy-policy URL and terms URL in configuration. Incorporate Google requirements and relevant data disclosures. **Currently missing: release blocker.**
- [ ] Configure a securely managed release keystore and CI signing; preserve upload-key recovery procedures. **Not supplied: release blocker.**
- [ ] Provide separate debug and release Places keys; validate both with real provider responses. Restrict application package and signing SHA-1, restrict required APIs, enable Places API (New) and billing. **No real key supplied: live integration verification blocker.**
- [ ] Review applicable target SDK / Play policy deadlines at submission time; current target is API 36.
- [ ] Configure per-API quotas, monitoring, budget alerts and operational kill/disable procedures. Billing alerts are notifications, **not a guaranteed spending cap**. Search/details field lists can invoke paid SKUs.
- [ ] Confirm keys never enter source control, screenshots, crash reports or logs. Embedded APK keys are extractable and must be restricted/rotatable.
- [ ] Re-check the current provider agreement for the billing entity/region. Only IDs and user interaction data persist. No provider content caching is enabled.
- [ ] Review Google Maps attribution in real nearby/details responses, including returned third-party HTML and links, contrast, visibility, proximity, and logo/text requirements. Add official logo assets if the final production layout has adequate space; compact cards currently use the policy's text alternative.
- [ ] Validate live missing-key, invalid-key, disabled API, billing, quota, network, zero results and removed Place ID cases using a controlled Cloud project.
- [ ] Verify foreground permission denial, permanent denial, approximate permission, service disabled, stale fix and timeout on physical devices / multiple Android versions.
- [ ] Verify custom system animation scales, TalkBack focus, largest font/display sizes, light/dark themes and landscape on device.
- [ ] Review all allergy language. Live source is Unknown for every restaurant; Conservative mode cannot select a restaurant with avoidance preferences. Never market a result as safe/allergen-free.
- [ ] Review dependency updates and rerun unit/Room/UI/lint checks. Pinned stable compatibility baseline intentionally predates newest versions.
- [ ] Run lifecycle/process-kill and storage failure drills with device tools; do not infer device success from unit tests alone.
- [ ] Complete Play Data safety disclosures, third-party notices, release signing/minification validation and store listing.
- [ ] Check [verification.md](verification.md) for the commands actually completed in the implementation environment.

No analytics SDK is included. No precise coordinates, keys, or allergy preferences are logged by app code. Backup is disabled. Local deletion requires explicit confirmation.
