# 正式發佈前（本工作沒有發佈）

- 提供實際 Supabase 專案 URL / public key；review 並執行兩份 additive migration，部署 delete-account Edge Function。
- 在 Supabase 設定 Google / Facebook provider、各自 OAuth redirect URI、Android callback allowlist；只啟用這兩種 provider。
- 真實帳戶端到端測試：成功、取消、失敗、token refresh、登出、刪帳戶、登入後返回原操作。
- 管理員電腦匯入 HK OSM export，review 未分類／缺座標／缺地址／OSM object 消失等資料缺口；不宣稱完整收錄。
- 填妥營運者、聯絡及保留政策，review `web/` 範本並上載免費靜態託管，設定 policy/terms/deletion URLs。
- 填正式 AdMob App ID、Banner ID、App Open ID；設定 UMP messaging，以官方測試模式真機驗證同意／拒絕／失敗／離線／外部返回不補播。
- 真機檢查定位拒絕／永久拒絕／服務關閉／新鮮定位／約略定位、導航、多種尺寸、字體放大及 TalkBack。
- review Supabase RLS/grants、管理員 UID、頻率限制、匿名 snapshot 防濫用及容量，監察免費額度；沒有建立付費資源。
- 自備 release signing key，在 CI 以安全 secret 注入。debug/demo 測試 ID 不能用作正式收益驗證。此版本並未簽署或發布正式 APK。

最新本機實測及未驗證項目見 verification.md；較早 log／deliverables 是舊方案歷史資料。
