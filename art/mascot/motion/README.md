# Approved sprite animation integration

The V3 review assets are now bundled in the native Android app as sixteen `mascot_motion_*.png` atlases in `drawable-nodpi`. Thirteen original cuisine costume groups map to all 48 current cuisine IDs, using the existing `CuisineMascot` mapping. Three additional clips are Welcome, Wok and Drooling. The exact built-in ImageGen prompts and original V3 approval provenance are retained in `manifest.json`; its review-only scope describes the source review package, not this integration.

`AnimatedPigMascot.kt` draws one of eight atlas cells directly. Odd atlas dimensions are handled by calculating both crop edges. Decoding happens off the main thread, with a 20 MiB LRU cache; it does not allocate a new bitmap per frame. Results play twice and hold their last pose, the homepage greets then shows hungry anticipation, and the wheel loops the wok clip until the original draw animation completes. Playback pauses outside the resumed activity lifecycle. Android's disabled animator setting displays the original static costume.

Frame timing is pure Kotlin in `MascotTimeline.kt`. Animations do not consume random numbers or affect filters, candidate counts or the saved draw result. APK resources are local; the review web server is not required by the app. Previous preview packages remain under ignored `deliverables/`.
