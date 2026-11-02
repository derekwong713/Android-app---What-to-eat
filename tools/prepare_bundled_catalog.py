"""Convert gosom JSON/JSONL export to the offline Android asset; no network calls."""
import argparse,json,hashlib,math
from pathlib import Path

def convert(rows,date):
    result={}
    for r in rows:
        name=str(r.get('title') or '').strip()
        identity=r.get('cid') or r.get('link')
        if not name or not identity: raise ValueError('Each row needs title and cid or link')
        rating=r.get('review_rating')
        rating=None if rating in (None,'') else float(rating)
        if rating is not None and (not math.isfinite(rating) or not 0<=rating<=5): raise ValueError('Invalid rating')
        lat,lon=r.get('latitude'),r.get('longitude')
        if lat is not None or lon is not None:
            lat,lon=float(lat),float(lon)
            if not (-90<=lat<=90 and -180<=lon<=180): raise ValueError('Invalid coordinates')
        # Cuisine requires an explicit curated mapping, never infer it from names/categories.
        cuisines=r.get('confirmed_cuisines',[])
        if not all(isinstance(c,dict) and c.get('id') and c.get('label') for c in cuisines): raise ValueError('Invalid confirmed_cuisines')
        key='bundled-'+hashlib.sha256(str(identity).encode()).hexdigest()[:24]
        row=dict(id=key,name=name,address=r.get('address') or '',latitude=lat,longitude=lon,cuisines=cuisines,opening_hours=None,status='unknown',enabled=True,source='Google Maps / gosom 匯出',updated_at=date,version=1,average_rating=rating,rating_count=int(r.get('review_count') or 0))
        if key in result and result[key]!=row: raise ValueError('Conflicting duplicate identity')
        result[key]=row
    return sorted(result.values(),key=lambda r:r['id'])

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('input',type=Path);p.add_argument('--date',required=True);p.add_argument('--output',type=Path,default=Path('app/src/main/assets/restaurants.json'));a=p.parse_args()
    raw=a.input.read_text(encoding='utf-8-sig')
    rows=json.loads(raw) if raw.lstrip().startswith('[') else [json.loads(x) for x in raw.splitlines() if x.strip()]
    output=convert(rows,a.date);a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(output,ensure_ascii=False,allow_nan=False,indent=2)+'\n',encoding='utf-8');print(f'{len(output)} restaurants; {sum(not r["cuisines"] for r in output)} unclassified (excluded from draws)')
