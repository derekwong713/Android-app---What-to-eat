# 本次驗收範圍

自動化規格包括：

- 純 domain：距離+菜式交集、未知狀態／時間、停業排除、去重、多菜式、未分類、零結果不放寬、定位缺失／過時。
- 抽選：distinct cuisine 與 restaurant 各階段無權重、重抽保留／避重、單一候選、零候選、輪盤指針對齊（1–64 菜式）。
- Auth：PKCE RFC vector、nonce/scheme 驗證、取消／錯誤、HTTP mock 回跳成功、pending action、登出、server key 拒絕。
- Ad：逾時／home／外部返回後 gate 永久關閉、重複回呼不能再展示。
- Android：Room v1→v2 保留舊資料、原子 reveal 唯一歷史、最多 200 筆、導航座標／地址編碼／名稱／無 handler；Compose 遊客抽選、重抽、登入入口；實際系統定位拒絕與不限距離；ViewModel 連點、保存、登入續操作、有限距離失敗。
- PostgreSQL：migration 可執行、RLS／grants 拒絕遊客與越權、社交 provider 限制、評分唯一／衍生平均、評論冪等、版本必填／衝突、修改者不可偽造、能力 token 必須匹配、快照過期、頻率限制、檢舉管理、OSM 更新保護及刪帳戶匿名保留。
- OSM 工具：名稱 fallback、noodle/noodles 正規化、type+id、冪等、缺菜式不猜、地址細節及缺座標不虛構。

上列是測試定義；實際執行結果、數量及阻礙以 verification.md 為準。外部 provider、雲端部署、實際 HK 匯入、正式 AdMob 收益及生產壓力測試尚需外部設定，不能以本機 mock 當成已驗證。
