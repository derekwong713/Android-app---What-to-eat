"""Generate the buildless /docs website from the currently bundled catalogue."""
import argparse
import collections
from html import escape
import json
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parent.parent
REPO='https://github.com/derekwong713/Android-app---What-to-eat'
def generate(date):
    rows=json.loads((ROOT/'app/src/main/assets/restaurants.json').read_text(encoding='utf-8'))
    areas=json.loads((ROOT/'tools/hk-localities.json').read_text(encoding='utf-8'))
    districts=json.loads((ROOT/'tools/hk-districts.json').read_text(encoding='utf-8'))
    eligible=[r for r in rows if r['enabled'] and r['status'] not in {'closed','temporarily_closed'} and r['cuisines']]
    cuisines={c['id'] for r in eligible for c in r['cuisines']}
    region_counts=collections.Counter(r['region'] for r in rows)
    themes=json.loads((ROOT/'art/mascot/cuisine-themes.json').read_text(encoding='utf-8'))
    cuisine_labels={c['id']:c['label'] for r in rows for c in r['cuisines']}
    theme_labels={'Chinese':'中菜及點心','Ramen':'日式及拉麵','Sushi':'壽司','Korean':'韓式','Southeast':'東南亞','Curry':'咖喱','Western':'西式','Grill':'燒烤','Vegetarian':'素食','Seafood':'海鮮','Hotpot':'火鍋','Dessert':'甜品','Wrap':'薄餅及卷餅'}
    assets=ROOT/'docs/assets'
    assets.mkdir(exist_ok=True)
    for key in ('base','sushi','hotpot','dessert'):
        shutil.copyfile(ROOT/f'app/src/main/res/drawable-nodpi/mascot_{key}.png',assets/f'mascot-{key}.png')
    motion=json.loads((ROOT/'art/mascot/motion/manifest.json').read_text(encoding='utf-8'))
    clips=[dict(id=t['id'],title=t['title'],description=t['description'],times=t['times'],cuisines=[c['label'] for c in t['cuisines']]) for t in motion['themes']]
    clips += [
        dict(id='welcome',title='首頁：眨眼揮手',description='首頁先播放迎賓動作，再播放流口水動作，最後停留。',times=[600,120,100,140,140,140,140,500],cuisines=[]),
        dict(id='wok',title='輪盤：拋鑊炒菜',description='輪盤中央循環播放拋鑊，直至已保存的抽選結果揭曉。',times=[180,140,120,120,150,140,140,180],cuisines=[]),
        dict(id='drooling',title='肚餓：流口水抹嘴',description='聞到香味、嘴角一小滴口水，再可愛地抹嘴。',times=[500,240,350,250,350,230,450,450],cuisines=[]),
    ]
    for clip in clips:
        clip['src']=f"assets/motion-{clip['id']}.png"
        shutil.copyfile(ROOT/f"app/src/main/res/drawable-nodpi/mascot_motion_{clip['id']}.png",ROOT/'docs'/clip['src'])
    (assets/'motion-clips.json').write_text(json.dumps(clips,ensure_ascii=False,indent=2),encoding='utf-8')
    def page(title,content):
        return f'''<!doctype html>
<html lang="zh-HK"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="color-scheme" content="light dark"><meta name="description" content="食咩好？Android 餐廳抽選 App：篩選、地區選擇、資料來源與更新指南。"><title>{escape(title)}｜食咩好？</title><link rel="stylesheet" href="style.css?v=0.3.1"></head>
<body><a class="skip" href="#main">跳至內容</a><header><a class="brand" href="index.html">食咩好？<small>WHAT TO EAT</small></a><nav aria-label="主選單"><a href="index.html#guide">使用方法</a><a href="index.html#filters">篩選條件</a><a href="mascot.html">豬仔角色</a><a href="data.html">資料與維護</a><a href="index.html#faq">常見問題</a></nav></header><main id="main">{content}</main>
<footer><div><strong>食咩好？</strong><p>少一點猶豫，多一個選擇。</p></div><p>開發版文件 · {escape(date)}<br><a href="{REPO}">GitHub 原始碼 ↗</a></p></footer></body></html>\n'''
    metrics=f'''<div class="metrics" aria-label="目前內置資料統計"><div><strong>{len(rows):,}</strong><span>已匯入餐廳</span></div><div><strong>{len(eligible):,}</strong><span>可抽選餐廳</span></div><div><strong>{len(cuisines)}</strong><span>可抽選菜式</span></div><div><strong>{len(areas)}</strong><span>細地點選項</span></div></div>'''
    index='''<section class="hero"><div><p class="eyebrow">原生 ANDROID · 香港繁體中文</p><h1>今日食咩？<br><span>交畀輪盤決定。</span></h1><p class="lead">揀地點、揀菜式，設定心水評分。先抽菜式，再抽餐廳，毋須註冊。</p><a class="button" href="#guide">開始了解 <span aria-hidden="true">↗</span></a></div><aside class="illustration"><img class="hero-mascot" src="assets/mascot-base.png" width="1254" height="1254" alt="大眼豬仔披着灰綠披肩，拿着鑊和鑊鏟"><p>豬仔陪你揀今餐</p><small>App 內置角色 · <a href="mascot.html">認識菜式造型 ↗</a></small></aside></section>'''+metrics+'''
<div class="notice" role="note"><strong>目前是 0.3.1 開發版</strong><p>餐廳資料隨 App 打包，以香港為目標範圍，未完整收錄全港餐廳。新版 debug APK 已建置；本網站不提供正式下載或商店版本，亦不代表已正式發布。</p></div>
<section id="guide"><p class="eyebrow">01 / 使用方法</p><h2>兩次抽選，一個去處。</h2><ol class="steps"><li><h3>設定條件</h3><p>選擇距離、菜式、最低評分及地區。按「套用篩選」，再開始抽選。</p></li><li><h3>先菜式，後餐廳</h3><p>先從符合條件的不同菜式中等機率抽選，再在該菜式候選內等機率抽餐廳。評分只作篩選，廣告及評分不會增加中選機率。</p></li><li><h3>查看，或者重抽</h3><p>「重抽餐廳」保留菜式；「重抽菜式」重新執行兩階段抽選。「在 Google Maps 查看餐廳資料」開啟外部餐廳頁，之後可在 Google Maps 選擇路線。</p></li></ol><p class="muted">多個候選時避免立即重複；只有一個時會提示。零候選不會自動放寬條件。同一餐廳在同一菜式候選中只計一次。</p></section>
<section id="filters"><p class="eyebrow">02 / 四種篩選</p><h2>按你想去的地方，<br>縮窄今日的選擇。</h2><div class="cards"><article><span class="tag">距離</span><h3>附近，或者不限距離</h3><p>不限、500、1,000、2,000、5,000 米內。距離是直線距離，不代表步行距離。「取得／重試目前位置」放在篩選面板的距離區內，按下會清除已選地區及細地點；只顯示簡單定位狀態。定位失敗可重試或不限距離繼續。</p></article><article><span class="tag">菜式</span><h3>可多選，不選即全部</h3><p>只從符合所有條件的餐廳實際包含的菜式中抽選。未分類餐廳不參與菜式抽選，不會按店名猜測菜式。</p></article><article><span class="tag">最低評分</span><h3>選擇你的評分門檻</h3><p>不限、3.0、3.5、4.0、4.5、5.0 分或以上。選了門檻後，暫無評分的餐廳不參與抽選；評分是匯入時的快照，並非即時更新。</p></article><article><span class="tag">地區及地點</span><h3>18 區，93 個細地點</h3><p>可搜尋太子、銅鑼灣、旺角、東涌等地點，支援多選或全選大區。行政區及細地點的選擇取聯集，再與菜式及評分條件一同篩選。</p></article></div><div class="callout"><strong>選了地區，就自動不限距離。</strong><p>選取地區／地點會取消定位請求、清除已取得位置，並自動不限距離；反過來，按取得目前位置會清除所有已選地區／細地點。菜式與評分保留，較遲返回的舊定位結果會被忽略。清除地區後不會自動恢復舊位置，需要時重新取得。</p></div><p>「多區資料」表示該地點的現有餐廳分屬不同行政區，會在相關區下顯示。選地點會包含該地點全部候選。</p><a href="data.html#districts">查看地區對應與資料限制 ↗</a></section>
<section class="split"><div><p class="eyebrow">03 / 設計及設定</p><h2>柔和配色，<br>清楚作選擇。</h2><p>莫蘭迪灰綠、藕粉、霧藍及暖灰，配合圓角元件、日夜模式和低飽和輪盤。首頁、輪盤及結果頁都有大眼豬仔陪你揀食。抽中菜式後會換上相關主題造型；配色及角色不影響實際抽選結果。</p><a href="mascot.html">看看豬仔的菜式造型 ↗</a><div class="swatches" aria-label="灰綠、藕粉、霧藍、暖灰配色"><span></span><span></span><span></span><span></span></div></div><dl><dt>毋須註冊</dt><dd>沒有帳戶、評論或評分提交入口；來源評分只供參考。</dd><dt>設定</dt><dd>提供廣告同意／私隱選項及清除本機資料。篩選、收藏及歷史保存在手機。</dd><dt>內置資料</dt><dd>抽選可在本機執行。廣告、外部 Google Maps 及定位服務各有網絡或裝置要求。</dd><dt>更新資料</dt><dd>重新整理餐廳資料並發布新版 APK；需重新安裝，手機資料才會更新。</dd></dl></section>
<section id="faq"><p class="eyebrow">04 / 常見問題</p><h2>使用前，你可能想知。</h2><details><summary>為甚麼揀了地區就不能揀 500 米？</summary><p>地區選擇代表你想在指定地點找餐廳，未必在目前位置附近。因此 App 自動不限距離；清除所有地區後，可重新選擇有限距離。</p></details><details><summary>銅鑼灣怎樣同時出現在兩個區？</summary><p>行政區按每間餐廳的來源座標及官方邊界分類，地點名稱則保留來源標記。本批銅鑼灣資料在灣仔區及東區均有記錄。選「銅鑼灣」包含兩區中標為該地點的候選；來源座標及標記仍可能有誤。</p></details><details><summary>為甚麼沒有符合條件的餐廳？</summary><p>可調整菜式、評分或地區；沒有選地區時也可調整距離。未分類、明確停業及不符合條件的餐廳不參與抽選，App 不會自行放寬條件。</p></details><details><summary>Google 評分和營業時間準確嗎？</summary><p>評分是來源資料的快照，並非即時評分。營業時間目前未整理，顯示「未知」；未知不等於已關門。明確停業的餐廳不參與抽選。</p></details><details><summary>App 有收集位置或需要登入嗎？</summary><p>毋須登入。有限距離使用裝置目前位置在本機篩選，不傳送到 Supabase。AdMob 及外部 Google Maps 有其自身私隱流程；正式發布前仍需完善營運者、聯絡資料及私隱政策。</p></details></section>'''
    restored=['area_prince_edward','area_causeway_bay','area_pok_fu_lam','area_mid_levels','area_lai_chi_kok','area_kowloon_tong','area_science_park','area_pak_shek_kok','area_lantau_island']
    place_rows=''.join('<tr><th scope="row">'+escape(areas[key]['label'])+'</th><td>'+escape('、'.join(districts['districts'][d]['label'] for d in areas[key]['districts']))+'</td></tr>' for key in restored)
    district_list=''
    for group in districts['groups'].values():
        district_list+='<details><summary>'+escape(group['label'])+'：區域及細地點</summary>'
        for key in group['districts']:
            labels=[v['label'] for v in areas.values() if key in v['districts']]
            district_list+='<h3>'+escape(districts['districts'][key]['label'])+' · '+str(region_counts[key])+' 間</h3><p>'+escape('、'.join(labels))+'</p>'
        district_list+='</details>'
    data=f'''<section class="hero compact"><div><p class="eyebrow">資料與維護指南</p><h1>知道資料從哪裏來，<br><span>再放心作選擇。</span></h1><p class="lead">以下數字來自目前內置清單；記錄資料範圍、分類方法、更新方式及驗證限制。</p></div></section>{metrics}
<section id="districts"><p class="eyebrow">01 / 地區對應</p><h2>行政區看座標，地點保留原名。</h2><p>匯入工具使用民政事務總署的 <a href="https://data.gov.hk/en-data/dataset/hk-had-json1-hong-kong-administrative-boundaries">行政區邊界資料</a>，在電腦上以餐廳座標作離線配對；App 不需要呼叫 Google Geocoding 或 Places API。現有 {len(rows):,} 間餐廳均匹配到行政區，準確度仍取決於來源座標及邊界版本。</p><div class="table-wrap"><table><caption>9 個地點在本批資料的所屬區域</caption><thead><tr><th scope="col">地點</th><th scope="col">本批餐廳資料所屬行政區</th></tr></thead><tbody>{place_rows}</tbody></table></div><p class="muted">這是現有餐廳記錄的對應結果，不代表每個地名在所有情況下只屬於這些區。同一地點可能在多區顯示；選某一行政區，只納入該區餐廳。選地點則納入該地點的全部候選，不重複計算。</p>{district_list}</section>
<section id="catalogue"><p class="eyebrow">02 / 資料清理</p><h2>保留可追溯的來源，<br>排除不適合抽選的記錄。</h2><ul><li>合併原有 16 筆及新一批 2,755 筆資料，按來源識別碼合併 2 筆重複；不按同名合併分店。</li><li>排除 42 筆境外資料，以及 62 筆非餐廳／類型不明記錄。</li><li>匯入 2,665 間；277 間未分類、15 間明確停業，不進入抽選，兩者有重疊。最後可抽選 2,376 間。</li><li>菜式只按經整理的來源分類對照；不按店名或搜尋字眼猜測。保留名稱、地址、座標、來源評分、Google Maps 餐廳連結、分類及更新日期。</li><li>不打包文字評論、評論者資料、電話或電郵。評分與資料日期是匯入快照，並非即時驗證。</li></ul><p>資料來自 gosom 匯出與本機整理。工具開源不等於來源資料的再發布授權；正式發布前仍須確認相關資料使用條款。行政區邊界資料來源：香港特別行政區政府民政事務總署。</p><a href="{REPO}/blob/main/docs/catalog-import-report.md">閱讀完整匯入報告 ↗</a></section>
<section id="maintain"><p class="eyebrow">03 / 維護者工作流程</p><h2>匯出、整理、驗證，再打包。</h2><ol class="workflow"><li><strong>在電腦執行 gosom</strong><p>Windows 可透過 Docker Desktop 的 Linux containers 執行，毋須更換作業系統。依 <a href="https://github.com/gosom/google-maps-scraper">gosom 官方說明</a> 匯出 JSON／JSONL，不收集額外文字評論。原始檔放在忽略 Git 的 <code>gosom-data/</code>。</p></li><li><strong>更新確認菜式對照及檢查資料</strong><p>菜式對照在 <code>tools/gosom-cuisine-map.json</code>。沒有對照的餐廳保持未分類。明確 CLOSED 等狀態才視為停業；描述文字及空白狀態保持未知。</p></li><li><strong>合併及產生內置清單</strong><pre><code>python tools/review_gosom_catalog.py gosom-data/results.json gosom-data/results-hk-20261103.json --date 2026-11-03 --sync-localities</code></pre><p>由專案根目錄執行，日期換成實際整理日期。輸入檔由舊至新排列，相同來源識別碼採後一快照；執行前保留原始檔。輸出 <code>app/src/main/assets/restaurants.json</code>，同時更新地點顯示分組；完整排除清單在 <code>gosom-data/import-report.json</code>。上述原始輸入檔不在 GitHub，須由維護者自行提供。</p></li><li><strong>執行檢查，再重新建置</strong><pre><code>python -m unittest discover -s tools -p 'test_*catalog.py' -v
python -m unittest discover -s tools -p test_district_boundaries.py -v</code></pre><p>以 Android Studio 建置 <code>demoDebug</code> 或 <code>liveDebug</code>；兩者目前均使用本機資料，debug 廣告使用官方測試單元。<code>tools/verify-community.ps1</code> 保留建置及測試流程。選與手機既有安裝相同的版本更新，資料才會生效。</p></li></ol><p class="muted">不需要 Supabase 專案或長期開機的私人電腦。更新邊界時，從官方來源另行下載並核對版本，重新測試及產生資料；App 不會在執行時下載行政區邊界。</p></section>
<section id="verification"><p class="eyebrow">04 / 目前驗證狀態</p><h2>已驗證的，與尚待確認的。</h2><div class="cards"><article><h3>已完成</h3><p>demoDebug／liveDebug 建置成功；36 項程式測試通過；動畫整合階段另有 31 項資料及匯入測試通過。保留 Android Studio 更新的 AGP 9.5.0／KSP 2.3.6／Gradle 9.8.2 重新驗證；Android lint 無錯誤，另有 175 項警告。0.3.1 兩個 APK 已確認包含 16 張逐格動畫圖集及 2,665 間餐廳；保留原本靜態角色作停用動畫時的回退。</p></article><article><h3>仍有範圍限制</h3><p>0.3.1 的 9 項畫面／定位取消競態測試通過；動畫整合亦完成逐格、靜態回退、抽選重抽及日夜模式驗證。首次原生定位提示測試受模擬器 System UI 無回應阻擋，實體手機定位及全面回歸仍待確認。</p></article></div><div class="links"><a href="{REPO}/blob/main/LOCATION-FILTERS.md">位置與地區互斥驗證 ↗</a><a href="{REPO}/blob/main/MOTION-INTEGRATION.md">動畫整合驗證紀錄 ↗</a><a href="{REPO}/blob/main/OFFLINE-CATALOG.md">完整匯入設定 ↗</a></div></section>'''
    demo='''<section id="animation-demo"><p class="eyebrow">原生 App 的動畫素材</p><h2>揀一款，看看豬仔怎樣開餐。</h2><p>這裡播放與 App 相同的八格圖集作示例；網站可循環播放，App 結果則播兩次後停留。只在選擇時載入該款素材。</p><div class="motion-controls"><label for="motion-choice">動畫造型</label><select id="motion-choice" aria-label="動畫造型"></select><button id="motion-play" type="button">播放動畫</button></div><canvas id="motion-canvas" width="640" height="640" role="img" aria-label="豬仔動畫示例"></canvas><p id="motion-description"></p><p id="motion-status" class="muted" role="status">動畫播放器需要 JavaScript；下方仍可查看原圖及菜式對照。</p><noscript><p>此瀏覽器未啟用 JavaScript，請參閱下方靜態造型。</p></noscript></section><script src="motion-preview.js" defer></script>'''
    theme_rows=''.join('<tr><th scope="row">'+escape(theme_labels[key])+'</th><td>'+escape('、'.join(cuisine_labels[c] for c in ids))+'</td></tr>' for key,ids in themes.items())
    mascot=f'''<section class="hero compact"><div><p class="eyebrow">豬仔角色 / MORANDI MASCOT</p><h1>拿起鑊鏟，<br><span>陪你揀好今餐。</span></h1><p class="lead">大眼睛、灰綠披肩、柔和莫蘭迪配色。首頁陪你揀條件，輪盤陪你等揭曉，結果就換上菜式主題造型。</p></div></section>
<section class="mascot-intro"><img src="assets/mascot-base.png" width="1254" height="1254" alt="豬仔原造型，手拿鑊及鑊鏟，穿輪盤圖案圍裙"><div><p class="eyebrow">一個角色，三個出場位置</p><h2>從揀條件，到揭曉結果。</h2><ol class="workflow"><li><strong>首頁：準備開餐</strong><p>先揮手迎賓，再播放流口水動作，陪你設定距離、菜式、評分及地區。</p></li><li><strong>輪盤：拋鑊炒菜</strong><p>豬仔在輪盤中央循環拋鑊、接回食材；是逐格手部及表情動作，不再只旋轉整張圖片。</p></li><li><strong>結果：換上相關造型</strong><p>按本輪抽中的菜式播放吃東西、咀嚼或其他相應動作，兩個循環後停留。「重抽餐廳」保留菜式主題，「重抽菜式」跟隨新菜式轉換。</p></li></ol></div></section>
<section><p class="eyebrow">造型示例</p><h2>抽到壽司、火鍋，還是甜品？</h2><div class="mascot-gallery"><figure><img src="assets/mascot-sushi.png" width="1254" height="1254" loading="lazy" decoding="async" alt="壽司主題豬仔拿着壽司盤及筷子"><figcaption><strong>壽司造型</strong><span>壽司盤與筷子，準備開動。</span></figcaption></figure><figure><img src="assets/mascot-hotpot.png" width="1254" height="1254" loading="lazy" decoding="async" alt="火鍋主題豬仔拿着火鍋及漏勺"><figcaption><strong>火鍋造型</strong><span>捧着熱鍋，拿好漏勺。</span></figcaption></figure><figure><img src="assets/mascot-dessert.png" width="1254" height="1254" loading="lazy" decoding="async" alt="甜品主題豬仔披着藕粉披肩，拿蛋糕及打蛋器"><figcaption><strong>甜品造型</strong><span>藕粉披肩，蛋糕與打蛋器。</span></figcaption></figure></div><p class="muted">角色插圖示例，並非餐廳食物照片，也不代表個別餐廳一定供應圖中食物。</p></section>
<section><p class="eyebrow">{len(themes)} 組主題 / {sum(map(len,themes.values()))} 種菜式</p><h2>相關菜式，共用主題。</h2><p>App 內置 13 組菜式動畫（104 格主題動作），對應 48 種菜式；同組菜式共用造型動畫。另有迎賓、拋鑊及流口水三款，共 16 張透明圖集，並保留原本靜態圖。以下為原圖示例及完整菜式對照。</p><div class="table-wrap"><table><caption>現有菜式與角色主題對照</caption><thead><tr><th scope="col">造型主題</th><th scope="col">對應菜式</th></tr></thead><tbody>{theme_rows}</tbody></table></div><div class="callout"><strong>跟隨抽中菜式，不猜餐廳風格。</strong><p>多菜式餐廳使用本輪實際抽中的菜式配圖，並非取餐廳第一個分類。新菜式未有對照時，顯示拿鑊原造型；角色不會改變任何篩選或抽選機率。</p></div></section>
<section><p class="eyebrow">無障礙與離線使用</p><h2>動作可以停，造型仍然在。</h2><p>App 在背景時暫停角色播放；停用 Android 系統動畫後，顯示原本靜態造型，抽選仍會顯示相同結果。圖片隨 App 打包，顯示角色毋須額外網絡、帳戶或金鑰，亦支援日夜模式。</p><p>本輪同時修正已完成抽選在重新啟動後仍沿用失效候選快取的問題：保留上次結果供查看，下一輪重新查詢符合現有條件的候選。</p><div class="links"><a href="{REPO}/blob/main/art/mascot/motion/README.md">角色素材與生成提示詞 ↗</a><a href="{REPO}/blob/main/MOTION-INTEGRATION.md">建置及畫面測試紀錄 ↗</a><a href="index.html#guide">返回使用方法 ↗</a></div></section>'''
    (ROOT/'docs/mascot.html').write_text(page('豬仔角色與菜式造型',mascot.replace('<section class="mascot-intro">',demo+'<section class="mascot-intro">')),encoding='utf-8')
    (ROOT/'docs/index.html').write_text(page('App 使用說明',index),encoding='utf-8')
    (ROOT/'docs/data.html').write_text(page('資料與維護指南',data),encoding='utf-8')
    print(f'Generated 3 documentation pages: {len(rows)} restaurants / {len(eligible)} drawable / {len(cuisines)} cuisines / {len(areas)} localities / {len(themes)} mascot themes')
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--date',required=True)
    generate(parser.parse_args().date)
