package hk.whatoeat.core.designsystem
import hk.whatoeat.core.model.*
fun Failure.message(): String=when(this) {
 Failure.Configuration->"尚未設定 Supabase，請參閱設定文件。"
 Failure.Network->"連線失敗，請檢查網絡後重試。"
 Failure.Permission,Failure.PermissionPermanent->"未獲定位權限。可重試、前往系統設定，或選擇不限距離。"
 Failure.LocationDisabled->"裝置定位已關閉。請開啟定位，或選擇不限距離。"
 Failure.LocationUnavailable,Failure.LocationTimeout,Failure.StaleLocation->"未能取得目前位置。請重試或選擇不限距離；不會使用舊位置。"
 Failure.Conflict->"資料已被其他用家更新。你的草稿仍保留；請關閉草稿、重新載入資料後再修改。"
 Failure.SnapshotExpired->"候選資料已更新或過期。請捨棄待處理抽選，再重新載入候選。"
 Failure.InvalidInput->"資料格式或長度不正確，請檢查輸入。"
 Failure.Login->"登入未完成或已過期，請重新登入。"
 Failure.Cancelled->"已取消登入。"
 Failure.Forbidden->"沒有執行此操作的權限。"
 Failure.RateLimited->"操作太頻密，請稍後再試。"
 Failure.Navigation->"未能開啟導航連結。"
 Failure.DetailsUnavailable->"餐廳資料暫未能讀取。舊版收藏仍保留，但不能以舊供應商編號查詢新後台。"
 Failure.Persistence->"未能儲存或讀取資料，請重試。"
}
fun Business.label(): String=when(this) {Business.Unknown->"營業狀態未知";Business.Operating->"營業中（資料紀錄，並非即時營業判斷）";Business.TemporarilyClosed->"暫停營業";Business.Closed->"已結業"}
fun filterSummary(f: Filters)=listOf(f.radius?.let {"${it} 米內"} ?: "不限距離",if(f.cuisines.isEmpty()) "所有已分類菜式" else "已選 ${f.cuisines.size} 種菜式").joinToString(" · ")
