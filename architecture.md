# Architecture and invariants

One app module, packages matching the requested core/data/domain/features layout. Hilt provides repositories, `TimeSource`, `IdSource`, `RandomSource`, I/O dispatcher and the Room database. Kotlin-only models and domain components do not import Android. Compose renders `HomeState` and invokes ViewModel events. `StateFlow` updates are atomic; lifecycle-aware collection stops inactive UI work. Navigation Compose owns bottom-tab/detail navigation; a modal result keeps an active prepared session above tabs and blocks settings changes.

## Data flow

User event → HomeViewModel → repository / pure domain component → immutable state → composable.

- `RestaurantRepository`: demo local fixtures or live Places New adapter, chosen once by flavor.
- `LocationRepository`: Fused Location, foreground permission guard, service state, 15-second deadline, timestamp/accuracy, explicit stale-fix choice.
- `AllergenInfoRepository`: fictional evidence in demo, Unknown in live. Evidence assessment is deterministic at injected time.
- `CandidateFilter`: provider+ID deduplication, stable ordering, reason sets and independent reason counts (a restaurant may have more than one reason).
- `PreferencesRepository`: DataStore filter, animation/mode preferences and avoidance/policy acknowledgment fingerprint.
- `FavoritesRepository` / `HistoryRepository` / `RoundStore`: Room through `LocalStore`.

Search and draw are separate. A monotonically increasing request number plus cancellation rejects obsolete search responses. A new successful search creates an opaque candidate version and resets the round. Effective filter changes create another version and reset the round; radius changes also search again. Presentation changes neither search nor reset. No composition or animation frame calls randomness. Requests are never automatically widened or relaxed. There is no automatic retry; the user can explicitly refresh.

## Filtering

Within-category sets use OR; categories combine with AND. Coordinates must be finite/in range. Selected cuisines exclude unclassified entries. Active price selection excludes Unknown unless opted in. Open-only requires an explicit Open status. Permanently/temporarily closed businesses cannot enter new draws. Unknown business status alone is not treated as closed.

Avoidance preferences are separate from general reset. With no selected allergens, no allergy exclusion applies. Applicable, restaurant-wide, sourced evidence within 90 days is assessed; narrow, future, stale or missing evidence becomes Unknown. KnownContains takes precedence over CrossContactRisk, then current SourceReportedAccommodation, then Unknown. Accommodation remains a qualified source statement, never a guarantee. Warning mode excludes conflicts and retains Unknown with an acknowledgment tied to selected allergens, policy mode and `allergy-policy-1`. Conservative mode also excludes Unknown. The warning remains visible for any selected avoidance preference, including qualified accommodation results.

## Draw state machine

`Idle → Prepared (durably saved) → Revealed (transaction) → Dismissed → Idle`.

1. ViewModel immediately sets busy before launching work. A coordinator Mutex serializes prepare, reveal, dismissal and round restart.
2. Existing active session is always returned before making another random choice.
3. Candidate IDs are deduplicated and sorted. Current-round IDs are removed. Empty pool returns Empty; completely drawn pool returns Exhausted. One remaining ID requires no random call; multiple IDs call `nextInt(size)` exactly once.
4. Engine prepares drawId, ordered available IDs, winner ID/index, version, mode, timestamp and user-authored filter summary.
5. Coordinator persists Prepared before publishing it. If saving fails, an in-memory retry buffer preserves that exact prepared choice until it can be saved or a deliberate new round invalidates it. An unsaved session is never animated/revealed. A process killed before any persistence has no durably established session.
6. Direct mode reveals immediately. Wheel animates the already selected result. Skip/back/background completion calls reveal on the same drawId.
7. A Room transaction atomically inserts history with unique drawId, trims to newest 200, updates drawn IDs, and marks Revealed. Repeated completion does nothing after the first transition. Draw time is reveal time, not a visit.
8. Dismissal removes the active session but retains drawn IDs. Exhaustion requires explicit restart confirmation. A new search or effective filter change starts a new round; changing mode never does.

Rotation uses the existing ViewModel session. Backgrounding calls reveal, so an interrupted spin cannot pick again. Process startup loads the round, reveals any existing prepared session, and fetches its winner by ID; it does not animate/reselect. Provider hydration failure preserves the session and history. Restored IDs remain durable; search results are deliberately not cached on disk. A user-requested fresh search after restoration starts a new round.

## Wheel conventions

`WheelGeometry` is pure Kotlin and exhaustive deterministic tests cover 1–20 candidates, every winner index, and rotations well beyond one turn.

- Canvas angular origin: 0° at right (3 o'clock).
- Positive arc and rotation: clockwise.
- Pointer: fixed top, 270° / -90°.
- Index i covers `[i × 360/N, (i+1) × 360/N]` in stable session-ID order.
- Center: `(i + 0.5) × 360/N`.
- Normalize: `((angle % 360) + 360) % 360`.
- Alignment: `normalize(270 − center)`.
- Target: `current + 5×360 + normalize(alignment − normalize(current))`.

Five complete turns plus the alignment remainder takes 3.6 seconds with deceleration. Counts >8 use numbers; the full numbered restaurant list is also provided. A one-candidate session reveals directly. App Reduce motion or disabled system animators reveals immediately. The wheel announces start and the result text announces the winner; per-frame angles are not accessibility announcements.

## Persistence and content boundaries

Room stores provider+Place ID favorites, timestamps, history IDs/mode/summary, round version, candidate IDs, winner ID/index and state. DataStore stores only user preferences. Provider names/addresses/coordinates/reviews/photos/search snapshots are never serialized. Only runtime memory holds provider content. Favorites remain independent of the current filter. Saved lists show honest placeholders/times until one item is opened; no aggressive hydration or fabricated offline names.

The whole application opts out of Android backup. Clear-local-data controls explicitly delete favorites, history, round state and preferences (including allergies) after confirmation. Clear-history confirmation is separate. General filter reset preserves allergy preferences.

## Intent handling

Live navigation uses an HTTPS Google Maps directions URL with destination and destination_place_id. The app first tries Google Maps, then a generic external HTTPS handler, with a recoverable error if both fail. Demo uses fictional coordinates without fake Google IDs. Phone/site actions exist only when values are available. Launching navigation writes no visit or meal record.
