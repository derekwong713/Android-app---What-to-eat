# 架構

保留 Compose Material3 / Hilt / Room / DataStore / StateFlow。依賴方向：畫面→HomeViewModel→Repository；純 domain 模型／filter／draw 不依賴 Android。

Live 只讀寫自己 Supabase；OSM 匯入在管理員環境執行。完整候選快照在伺服器保存 10 分鐘，每種 cuisine 及同菜式內 restaurant 均等機率，與評分／廣告無關。只把候選統計及單次結果傳回 Android。catalog 更新使快照失效；不以截斷資料假稱完整。手機不直接查 Overpass。

Auth PKCE 及 pending action 加密於 noBackupFilesDir；public key 在 APK，service role 永不在 APK。所有寫入經 security-definer RPC 的固定 search_path、身份／provider／頻率／欄位 whitelist／版本檢查；RLS 拒絕直接寫表。評分 aggregate 僅由後台計算。

抽選先原子保存待處理 request UUID，再呼叫後台冪等抽選，再保存已準備結果，動畫只揭曉已儲存結果。歷史 drawId PK + transaction 保證只記錄一次。進背景或重啟直接揭曉，不換籤。Room v1 資料保留，以新增推薦狀態表作 v2。

廣告 Controller 只有冷啟動窗口，逾時／失敗即首頁，沒有 onStart 自動廣告；UMP 未同意不請求廣告。固定 banner 區與內容／底部導航分離。外部登入／導航標記抑制下一冷啟動十分鐘。

已知營運界限及部署細節見 supabase/README.md；未取得實際專案設定，未作雲端或大流量驗證。
