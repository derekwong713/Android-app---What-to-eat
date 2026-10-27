# 「食咩好？」後台設定

本目錄尚未部署至任何雲端專案，沒有建立付費資源。先使用 Supabase 免費專案，備份現有資料並 review SQL。

## 執行 migration

依序在 Supabase Dashboard SQL Editor 執行 `migrations/20261027000100_community.sql`、`20261027000200_osm.sql`，每份各一次；或在已安裝 Supabase CLI 並登入後：

```sh
supabase link --project-ref YOUR_PROJECT_REF
supabase db push
supabase functions deploy delete-account --no-verify-jwt
```

函式不依賴 gateway 的 JWT 驗證；內部使用 `auth.getUser(bearer)` 真正驗證用家，不能以 request body 指定被刪帳戶。伺服器 service role 只存在 Edge Function 環境與匯入工具。不要放入 Android `secrets.properties`。兩份 migration 新增 `eat_*` 資料表，不刪除既有資料。若已有同名資料表，停止並先對照 schema，不要重跑或 drop table。

## Google / Facebook

1. Supabase Auth 只啟用 Google、Facebook。停用 Email、Phone 及其他 provider；不需要 Resend、SMTP、短訊服務或購買網域。
2. Google Cloud 建立 OAuth Web client，設定 consent screen、測試用家及 Supabase Dashboard 顯示的 redirect URI，通常為 `https://<project>.supabase.co/auth/v1/callback`。Client ID/secret 放在 Supabase Google provider；不放 APK。
3. Facebook Developers 建立適合的 Facebook Login app；OAuth redirect URI 同上。App ID/secret 放 Supabase Facebook provider。設定 privacy policy、terms、data deletion URL、測試角色；正式開放的審核／Live 模式由帳戶擁有人完成。
4. Supabase URL Configuration allowlist 加入 `hk.whatoeat://auth/callback**` 及 `hk.whatoeat.demo://auth/callback**`。App 會在 query 加一次性 `flow` nonce，採 PKCE S256；不要移除 query。
5. Android 使用外部瀏覽器 OAuth，Manifest 已加入 singleTask 回跳。並非 Google/Facebook 原生 SDK；不需要在 App 填 provider secret。實際成功、拒絕、取消和登出仍需自己的 provider 專案作端到端測試。

## Android 公開設定

將根目錄 `secrets.properties.example` 複製為 `secrets.properties`，填 managed Supabase URL 及 publishable key（或舊 anon JWT）。Service role / secret key 會被程式拒絕。重建 `liveDebug`；缺設定會清楚顯示錯誤，不會改用示範餐廳。

## 公平候選及成本

裝置只下載菜式目錄、候選數量、單次結果、每頁 10 則評論／修改紀錄。篩選 snapshot 在後台包含全部符合條件的已分類餐廳，以 distinct cuisine 均勻抽一次，再以 cuisine 內 distinct restaurant 均勻抽一次。不是抽前 N 筆。有效期 10 分鐘；餐廳／菜式資料有變更即 catalog version 失效，必須明確更新，不會放寬篩選。評分及廣告不參與抽選。每個 snapshot 上限 50,000 間、同時最多 1,000 個未過期 snapshot；超限會失敗，不返回被截斷的假完整候選。此取捨需以實際香港匯入量／流量再作效能評估，未做生產壓力測試。

Draw request UUID 與 capability token 讓網絡重試在 24 小時內返回同一結果；App 在發請求前寫入 Room，重試不能偷偷換籤。過期可明確捨棄重新抽選。Token hash 留在後台，snapshot 不儲存用家座標。公開建立 snapshot 仍需要部署層 request rate limit／監測防濫用；目前硬性容量限制是可測試版本的保護，並非大流量容量保證。

管理員可按需要以伺服器呼叫 `eat_cleanup_expired()`，清理過期 snapshot、draw receipt、兩日以前頻率 bucket。可選擇 Supabase Cron；本工作沒有建立排程或開啟計費。請按實際用量監察免費額度。

## OSM 匯入／更新

已核對 [香港 OSM 邊界關係 913110](https://www.openstreetmap.org/relation/913110) 的 `ISO3166-2=CN-HK` 標籤。可在 [Overpass Turbo](https://overpass-turbo.eu/) 貼上查詢並匯出 JSON。

在管理員電腦向 Overpass 執行 `tools/hong_kong.overpassql` 一次取得 JSON（`out meta center`）；公共 endpoint 不是 App runtime 後台。按服務政策安排批次，不要大量重試。

```sh
python tools/import_osm.py hong-kong.json
# 上一步 dry-run review 後，在伺服器環境設定 .env.example 中兩項，不要將 .env 提交 Git。
python tools/import_osm.py hong-kong.json --apply
```

node/way/relation + id 為來源鍵；同名連鎖分店不會合併。重跑相同 source version 不新增餐廳。原始 cuisine/tags/版本保存在 private provenance；缺菜式不猜，缺時間保持未知。OSM 更新只更新沒有被用家改過的欄位；用家更改過的欄位持續保護。不存在的物件不自動刪除，已停用餐廳不自動重新啟用。需要人工處理來源物件消失及用家覆寫後的來源衝突。

**尚未匯入實際香港資料**：沒有提供 Supabase 專案／server key，因此不能聲稱已覆蓋香港餐廳或有任何真實數量。OSM 自身亦並非完整餐廳名冊。示範版全部為標明虛構的 fixture。

## 管理檢舉

SQL Editor 由可信管理員執行（替換實際 Auth UID）：

```sql
insert into eat_private.admins(user_id) values ('REAL_AUTH_USER_UUID') on conflict do nothing;
```

App 登入該 Google/Facebook 帳戶後顯示管理入口，可處理一頁未完成檢舉、隱藏評論、停用餐廳或 dismiss。每頁處理後重新載入，其他檢舉會進入頁面。一般用家不能設定管理員、直接改管理欄位或刪餐廳。還原會新增版本；不移除原歷史。

## 保留方式

刪除 Auth 帳戶會 cascade 刪除 profile、評分及評論；修改紀錄／資料檢舉 reporter 設 NULL。公開餐廳資料與匿名 before/after 歷史保留以確保來源和可還原性。評論相關檢舉隨評論刪除。本機清除是另一個明確操作。公開文字可能包含用家自行輸入的私人資料，營運方需處理檢舉／刪除請求；不應在評論或修改內容輸入私人資料。

## AdMob

debug / demo 的 Banner、App Open 單元永遠使用官方測試 ID；App ID 預設亦是官方示例。要測試自己的 UMP 訊息，可把專用測試 App 的公開 App ID 填入 `ADMOB_CONSENT_TEST_APP_ID`，廣告單元仍固定為官方測試單元，不產生正式廣告流量。live release 需要填三個正式 ID；缺少時停用廣告而不阻止抽選。AdMob dashboard 設定 Privacy & messaging，完成適用地區的 UMP 同意訊息，設定商店資料安全／廣告披露，並審核真機位置與誤觸風險。只有 App Open + 固定 Banner，無獎勵／插頁。開屏窗口 1.8 秒，之後無論廣告何時回來都不播；從登入／導航返回亦不會再播。首次同意表單來不及在啟動載入時，從帳戶的廣告私隱入口處理，未同意不請求廣告。

主要參考：[Supabase PKCE](https://supabase.com/docs/guides/auth/sessions/pkce-flow)、[Google](https://supabase.com/docs/guides/auth/social-sign-in/auth-google)、[Facebook](https://supabase.com/docs/guides/auth/social-sign-in/auth-facebook)、[AdMob 測試](https://developers.google.com/admob/android/test-ads)、[UMP](https://developers.google.com/admob/android/privacy)、[OSM 授權](https://www.openstreetmap.org/copyright)。
