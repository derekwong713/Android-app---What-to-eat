# 食咩好？

原生 Android、Compose Material 3、Hilt、Room、DataStore，沿用現有配色及輪盤幾何。已重新檢查 Android Studio 的 AGP 9.4.1 / Gradle 9.8.0 設定，使用內建 Kotlin（2.2.10 / KSP 2.2.10-2.0.2），未回退舊工具鏈。沒有發現適用的 AGENTS.md。

## 使用

Android Studio 開啟根目錄，選 `demoDebug` 可測試虛構餐廳及明確標示的模擬 Google/Facebook 登入。`liveDebug` 使用 Supabase 真實後台，缺設定會顯示設定錯誤，絕不以 demo 代替。介面固定香港繁體中文。

```powershell
.\tools\verify-community.ps1 -Isolated -TestApk
# 有可用模擬器／真機後：
.\tools\verify-community.ps1 -Isolated -Connected
python tools/verify_project.py
python -m unittest discover -s tools -p test_import_osm.py
node tools/test_backend.mjs
# 另可用 Deno：
deno check supabase/functions/delete-account/index.ts
deno test supabase/functions/delete-account/handler_test.ts
```

PostgreSQL 本機測試先執行 `python tools/setup_backend_test_runtime.py` 安裝已校驗 npm integrity 的 PGlite runtime 至忽略的 `.tools/`。Android 驗證會使用現有 Gradle cache、獨立建置輸出及 workspace temp，避免 Android Studio 檔案鎖。JDK17 wrapper 依 Studio daemon criteria 使用既有 JetBrains21；SDK compile/target36、min26。

## 主要實作

- `feature/home/HomeViewModel.kt`：篩選→完整候選快照→保存 request ID→抽菜式→抽餐廳→保存結果→動畫揭曉。連點受 busy+Mutex 保護，Room 原子保存歷史；重啟揭曉同一已保存結果。
- `domain/filter/CandidateFilter.kt`、`domain/draw/DrawEngine.kt`：純 Kotlin 距離／狀態／菜式與無權重兩階段抽選；真正後台使用相同規則的 SQL RPC。
- `feature/result/Wheel.kt`、`domain/draw/WheelGeometry.kt`：輪盤使用已抽中的 cuisine index，固定上方指針，支援多於 20 個菜式；略過、背景及減少動畫均不重抽。
- `data/places/GoogleRestaurantRepository.kt`：為保留現有檔案而原位改成 `SupabaseRestaurantRepository`，檔名為遷移痕跡；沒有 Google Places SDK 或 API 呼叫。`DemoRestaurantRepository` 是虛構本機測試資料。
- `data/auth/`：Google/Facebook Supabase PKCE、nonce、Android callback、Keystore AES-GCM 保存 token／待續操作、refresh、登出、刪帳戶。
- `data/location/`：只接受 fresh current location，拒絕過時／無效位置；有限距離需要用家主動定位，預設不限距離。
- `feature/result/CommunityDialogs.kt`：修改、評分、評論、檢舉、修改歷史／還原及最小管理流程；optimistic version conflict 保留草稿，須明確重載。
- `data/ads/`：AdMob Banner + App Open，UMP 同意及私隱入口；startup deadline 後不補播，外部導航／登入回來不播。
- `data/local/EatDatabase.kt`：Room 1→2 additive migration，舊收藏／歷史／round_state 保留。舊 Google IDs 不轉換成 Supabase IDs；僅用家明確清除才刪本機紀錄。
- `supabase/`：可 review migrations、RLS、RPC、server-only OSM import、delete-account Edge Function。

公開欄位與外部設定見 `secrets.properties.example`；伺服器專用見 `.env.example`。完整部署步驟及需提供項目見 [supabase/README.md](supabase/README.md)。實測結果見 [verification.md](verification.md)。

## 資料與私隱

範圍香港，尚未匯入真正香港資料，不能聲稱完整收錄。餐廳起始資料 © OpenStreetMap contributors，ODbL；用家修改不寫回 OSM。沒有 Google Maps／Places／Geocoding／Routes SDK 或 API，導航僅開啟 Google Maps HTTPS URL，不依賴 Place ID。沒有 Resend、付費資料／短訊／地圖資源，沒有正式發佈或啟用計費。

`web/` 提供私隱／條款／帳戶刪除靜態範本，可用免費 GitHub Pages 等服務託管；營運者及聯絡資料待填，尚未上載。

舊 `deliverables/` 或較早 verification log 屬舊方案，不能作為這次產品的驗證。新版本為 0.2.0 / code 2。
