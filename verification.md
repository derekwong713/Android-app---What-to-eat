# 2026-11-03 — 9 個地點放回行政區列表

- 以民政事務總署的離線官方邊界 GeoJSON 及來源餐廳座標判定行政區，取代按地名推測行政區；本批 2,665 間全部有匹配。
- 太子、銅鑼灣、薄扶林、半山、荔枝角、九龍塘、科學園、白石角、大嶼山已置於相關地區卡片下。銅鑼灣及九龍塘的本批資料各分屬兩區，會在相關區下顯示；同一 ID 的候選不重複計算。
- 細地點顯示分組從實際已分類資料產生；多區資料標籤只描述本批來源資料，並非斷言整個地名的正式行政範圍。來源座標／地名亦可能有誤。
- 多區地點明確選取後，再選其中一個父區，不會靜默刪掉地點選擇；只有所有所屬區均已選取才移除多餘的地點條件。
- 29 項 JVM 測試與 25 項 Python 匯入／邊界測試通過。覆蓋 9 地點、同地名跨區、邊界點、多邊形孔洞、境外／無效座標及重複匯入一致性。
- demoDebug / liveDebug 建置成功，兩者 APK 內餐廳 asset 已核對一致。
- 本次畫面測試未完成：模擬器因現有 AVD instance/data lock 無法啟動；read-only 重試亦失敗。已新增太子／銅鑼灣畫面測試並成功編譯，但未宣稱執行通過。上一版 UI 測試結果保留於下方。
- 未操作實體手機；安裝檔在 deliverables/district-restored-20261103/。APK 保留於本機，不隨原始碼上傳。

---

# 2026-11-03 — 細地點、自動不限距離及莫蘭迪色系

- 18 區以外新增 93 個來源細地點，多選、可搜尋；2,558 間餐廳有細地點資料，行政區與細地點分開處理。
- 選任何地區／地點即不限距離；UI 即時重設、偏好讀寫及 ViewModel 正規化、CandidateFilter effectiveRadius 防止舊有限距離殘留。清除地區後維持不限，直至明確改選。
- 日／夜 Material 3 莫蘭迪色系、輪盤、圖示及系統列配色；18 組主要文字／背景配對及 6 種輪盤文字對比均至少 4.5:1。
- demoDebug / liveDebug 建置成功，最新 asset 已逐位元核對。
- 28 項 JVM 測試及 20 項 Python 資料測試通過。
- API 37 模擬器在淺／深色模式各通過 1 項篩選畫面測試：500 米 → 選旺角 → 套用結果不限距離，最低評分保留 4.0。
- 首輪截圖遇到模擬器 System UI 無回應，冷啟動後重新測試及視覺檢查通過。有效截圖在 verification-artifacts/morandi-20261103/filter-light-cold.png 及 filter-dark-cold.png；截圖包含系統鍵盤，並非純 App 全畫面展示。
- 沒有更動已連接的實體手機；一般手機、字體縮放及完整歷史流程未在本輪全面重測。
- 安裝檔與 SHA256 在 deliverables/morandi-20261103/。APK 保留於本機，不隨原始碼上傳。

---

# 2026-11-03 — 18 區細分

- 香港 18 區按三大區分組，可多選及全選／取消整個大區；地區未知獨立保留。
- 來源 city 精確對照至細區；2,505 間有細區，160 間含糊／未對照地名保留未知。不是官方邊界多邊形判定。
- 舊大區偏好會展開至所屬細區；新增測試覆蓋相同大區內的細區區分及多選。
- demoDebug / liveDebug 建置成功；26 項 JVM 測試及 19 項 Python 資料測試通過。
- 兩個 APK 已核對包含最新餐廳 asset，副本在 deliverables/districts-20261103/。
- 未做本次真機／模擬器操作驗證；APK 保留於本機，不隨原始碼上傳。

---

# 2026-11-03 — 本機資料、評分及地區篩選

- demoDebug / liveDebug APK 建置成功，兩者的 assets/restaurants.json 與專案最新資料完全相同。
- 24 項 JVM 單元測試通過，0 失敗，包括新增四條件交集、未知評分／地區、零候選及重抽不繞過篩選。
- 17 項 Python 匯入測試通過，包括按來源 state 分類地區、不按店名推斷。
- 評分及地區條件透過 DataStore 儲存；舊安裝預設不限。設定／私隱入口取代帳戶入口。
- 篩選 UI、DataStore 重啟保留及 Google Maps 返回尚未完成本次真機／模擬器驗證。
- 建置需同時使用 IPv4 及 .tools/tmp 的 JVM／TEMP 設定（見 tools/verify-community.ps1），避免先前 loopback 錯誤。
- 可安裝檔在忽略 Git 的 deliverables/filters-20261103/，APK 不隨原始碼上傳，未作正式發布。

以下為先前版本的歷史驗證，不能視為新版本已完成所有整合測試。

---

# 2026-10-27「食咩好？」0.2.0 驗證

本報告針對 Supabase／OSM／兩階段抽選新方案。較早 33 單元／13 裝置測試及舊 deliverables 是舊方案，不能混作本次證據。

## 已執行結果

| 項目 | 結果 | 證據 |
|---|---|---|
| Android demoDebug、liveDebug | Kotlin／Java 編譯及 APK 建置成功 | community-final-accepted.log、community-package.log、community-config-verified.log |
| Demo 單元測試 | 20／20 通過 | verification-artifacts/community-0.2.0/demo-unit/ |
| Live 單元測試 | 20／20 通過（同一套 domain／protocol 測試） | verification-artifacts/community-0.2.0/live-unit/ |
| 模擬器整合 | 12／12 通過、0 skipped | verification-artifacts/community-0.2.0/instrumentation.xml |
| 最後餐廳重載入口封裝後 | 完整遊客 UI 抽選／重抽再跑 1 項通過 | community-ui-final.log |
| Lint Demo / Live | 各 0 錯誤、173 警告 | verification-artifacts/community-0.2.0/lint-results-*.html |
| PostgreSQL migrations / 權限及資料規則 | 兩份 migration 成功執行，37 項檢查通過 | community-backend.log |
| OSM 匯入工具 | 6／6 通過 | community-import.log |
| Edge Function TypeScript | Deno check 通過，使用真實 SDK 型別 | edge-typecheck.log |
| 刪帳戶 handler | 5／5 本機測試通過 | edge-tests.log |
| 結構及依賴 | 44 Kotlin 檔、163 資源鍵 × 3 目錄一致；resolved graph 無 Places／Maps SDK；git diff --check 無 whitespace error | tools/verify_project.py、community-package.log |

Lint 警告主要是保留的舊資源未使用（144 項）、依賴升級提示及 KTX 建議；沒有新增通知權限 suppression。未為消除非阻擋提示而刪除舊資源或大幅升級既有架構。

環境為 Windows、Studio AGP 9.4.1 / Gradle 9.8.0，Kotlin 2.2.10 / KSP 2.2.10-2.0.2，Room 2.8.5；沿用 JetBrains21 daemon criteria、Java17 bytecode、compile/target36、min26。裝置為既有 Medium_Phone_API_37.0（Android17）模擬器。CLI 使用獨立輸出，沒有關閉 Android Studio 或刪除其專案快取。

## 實際覆蓋

- 遊客不限距離、只選距離／菜式、載入 3 間虛構候選／3 菜式、先抽菜式再抽餐廳、重抽餐廳；登入入口只顯示明確標示的模擬 Google/Facebook。
- 純 domain：零候選不放寬、單一候選、不重複上一階段結果、多菜式去重、不以餐廳數量加權菜式、停業／停用排除、缺時間仍可推薦、未分類排除、有限距離需要真實來源位置。
- 輪盤幾何 1–64 候選、負角度及多圈對齊；動畫只揭曉已選結果；App 背景／重啟以已存結果恢復。最後新增明確的放棄草稿／重新讀取入口後再次建置，並重跑遊客 UI 及正常離線啟動。
- ViewModel + Room：30 次連按只執行一次 prepare、重複 reveal 只產生一次歷史、餐廳重抽保留菜式且避重、登入後返回評分、有限距離拒絕缺位置及明確不限距離；候選快照未過期也不能使用超過五分鐘的位置，重新定位失敗會清除舊位置。
- 真實 Android 定位拒絕／已拒絕狀態：原生權限畫面及 App 不限距離入口；測試等待資料寫入完成及讀取 native view IDs，沒有用測試跳過或修改 App 來掩蓋失敗。
- Room v1→v2 真正 schema migration 保留舊收藏／歷史；已修正 Room migration helper 與 target APK serialization runtime 不一致，統一至 Room 所需 1.8.1。歷史最多 200 筆，揭曉 transaction 去重。
- OAuth RFC PKCE、nonce／scheme 防錯、取消／失敗區分、mock HTTP 成功回跳、pending action、登出、App 拒絕 server secret key；不是實際 provider 帳戶測試。
- 導航測試：座標優先、名稱保留、含 &/#/空格的地址正確編碼、沒有 Place ID、沒有 handler 時可恢復；只開外部 HTTPS Google Maps URL。
- PostgreSQL 以 PGlite 執行實際 SQL，使用最小 Auth claim/role stubs：遊客／越權寫入被拒、provider 限制、評分唯一及平均、評論冪等、版本必填／衝突、不能偽造修改者及管理欄位、能力 token（含 null）驗證、快照過期、頻率限制、檢舉管理、OSM type+id／重複匯入／保護覆寫、還原及刪帳戶 cascade／匿名歷史。
- Edge handler 不信任 body 中的 user ID；只刪已驗證 Google/Facebook 身份，錯誤不洩漏伺服器細節。測試使用明確 mock API，沒有刪除任何雲端帳戶。

## 正常 App 離線／回網測試

在最終 demo APK 執行一次遊客抽選後，結束 instrumentation，重新以一般 App process 啟動（沒有測試的停用廣告屬性）。暫停模擬器 Wi-Fi／流動數據，確認 `Active default network: none`。App 正常恢復「粉麵／示範茶餐廳（虛構）」；網絡恢復十秒後仍是同一餐廳及 MainActivity，沒有延遲開屏廣告。原有兩個網絡開關已還原。

證據：`community-offline.log`、`verification-artifacts/community-0.2.0/offline.png`、`offline.xml`、`network-return.xml`。此為 SDK 啟動／失敗及畫面恢復檢查；不代表已驗證正式廣告或真實 UMP 同意訊息。

## 外部阻礙及尚未驗證

1. 未提供 Supabase project URL / public key、未連接可部署帳戶：沒有雲端執行 migrations、Edge Function 或實際 RLS/Auth gateway 整合。本機 PostgreSQL 不能代替完整 Supabase 平台端到端驗證。
2. 未提供 Google / Facebook provider 設定：尚未以真實帳戶測成功、取消、失敗、refresh、登出及刪帳戶。所有 client secrets 只應設於 Supabase dashboard。
3. 未設定自己的 AdMob App／單元及 UMP message：正式投放、真實同意選擇／撤回與實際 App Open 展示未驗證。debug/demo 單元固定官方測試 ID；可填 ADMOB_CONSENT_TEST_APP_ID 測試自己設定的同意訊息，仍使用官方測試廣告單元。
4. 未取得／匯入香港完整範圍 OSM export。小型 Overpass 讀取嘗試返回 HTTP406；已從 OSM 官方頁面核對香港邊界 CN-HK。可用所附 Overpass Turbo 查詢匯出 JSON，再執行匯入工具。沒有以 fixture 冒充香港資料，也不聲稱 OSM 完整收錄餐廳。
5. 真機目前位置成功、不同廠牌定位、Google Maps 真機返回、不同尺寸／字體／TalkBack、正式流量及資料量壓力未驗證；目前只有模擬器、mock 定位及純距離測試。
6. web/ 是未發佈範本，欠營運者／聯絡資料、政策審核及免費託管網址；未建立付費資源、未開啟計費、未簽署或發佈正式版本。

執行步驟／設定清單見 supabase/README.md、secrets.properties.example、.env.example、release-checklist.md。
