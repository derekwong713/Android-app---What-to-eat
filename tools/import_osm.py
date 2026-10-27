"""Server-only OSM importer. Dry run by default; never writes to OSM."""
import argparse, hashlib, json, os, re, urllib.request
from datetime import datetime, timezone
ALIASES={"noodle":"noodles","ramen":"japanese","chinese":"chinese","cantonese":"cantonese","japanese":"japanese","coffee_shop":"cafe","coffee":"cafe"}
FOOD_TYPES={"restaurant","fast_food","cafe","food_court"}
LABELS={"noodles":"粉麵","chinese":"中式","cantonese":"粵菜","japanese":"日式","cafe":"咖啡店","korean":"韓式","thai":"泰式","western":"西式"}
def cuisine(raw):
    values={}
    for value in raw.split(";"):
        value=value.strip().lower()
        if not value: continue
        key=ALIASES.get(value,value)
        if not re.fullmatch(r"[a-z0-9_][a-z0-9_-]{0,47}",key): key="osm_"+hashlib.sha256(value.encode()).hexdigest()[:20]
        values[key]={"id":key,"label":LABELS.get(key,value)[:80]}
    return list(values.values())
def transform(e,observed):
    t=e.get("tags",{});name=next((t[k].strip() for k in ("name:zh-Hant","name:zh","name","name:en") if t.get(k,"").strip()),None)
    if not name or e.get("type") not in ("node","way","relation"): return None
    coord=e if e["type"]=="node" else e.get("center",{})
    lat,lon=coord.get("lat"),coord.get("lon")
    if lat is None or lon is None or not (-90<=lat<=90 and -180<=lon<=180): lat=lon=None
    address=t.get("addr:full") or " ".join(str(t[k]) for k in ("addr:city","addr:district","addr:subdistrict","addr:street","addr:housenumber","addr:place","addr:mall","addr:building","addr:floor","addr:unit","addr:shop") if t.get(k))
    inactive=t.get("disused")=="yes" or t.get("abandoned")=="yes" or (t.get("amenity") not in FOOD_TYPES and (t.get("disused:amenity") in FOOD_TYPES or t.get("abandoned:amenity") in FOOD_TYPES))
    status="closed" if inactive else "unknown"
    digest=hashlib.sha256(json.dumps(e,sort_keys=True,ensure_ascii=False).encode()).hexdigest()
    return dict(object_type=e["type"],object_id=e["id"],source_version=str(e.get("version","unknown"))+":"+digest,observed_at=e.get("timestamp") or observed,name=name[:120],address=address[:500],latitude=lat,longitude=lon,opening_hours=t.get("opening_hours","")[:1000] or None,status=status,cuisines=cuisine(t.get("cuisine","")),raw_tags=t)
def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument("input");p.add_argument("--apply",action="store_true");p.add_argument("--observed-at",help="Fallback timestamp when export lacks OSM metadata");a=p.parse_args()
    data=json.load(open(a.input,encoding="utf-8"));observed=a.observed_at or datetime.now(timezone.utc).isoformat()
    rows=[r for e in data.get("elements",[]) if (r:=transform(e,observed))]
    print(f"Prepared {len(rows)} OSM objects; {len(data.get('elements',[]))-len(rows)} skipped. This is not a complete HK inventory.")
    print("Gaps: unclassified={}, missing coordinates={}, missing address={}, unknown hours={}".format(sum(not r["cuisines"] for r in rows),sum(r["latitude"] is None for r in rows),sum(not r["address"] for r in rows),sum(not r["opening_hours"] for r in rows)))
    if not a.apply:
        print(json.dumps(rows,ensure_ascii=False,indent=2));return
    base=os.environ["SUPABASE_URL"].rstrip("/");key=os.environ["SUPABASE_SERVICE_ROLE_KEY"]
    if not base.startswith("https://"): raise ValueError("Use HTTPS hosted Supabase")
    for start in range(0,len(rows),500):
        req=urllib.request.Request(base+"/rest/v1/rpc/eat_import_osm",data=json.dumps({"p_records":rows[start:start+500]}).encode(),headers={"Content-Type":"application/json","apikey":key,"Authorization":"Bearer "+key},method="POST")
        with urllib.request.urlopen(req,timeout=120) as response: print(response.read().decode())
if __name__=="__main__": main()
