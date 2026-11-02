"""Convert gosom JSON/JSONL export to the offline Android asset; no network calls."""
import argparse,json,hashlib,math
from pathlib import Path
from urllib.parse import urlsplit, urlunsplit
from district_boundaries import district_for

DISTRICTS=json.loads((Path(__file__).parent/'hk-districts.json').read_text(encoding='utf-8'))
DISTRICT_BY_LOCALITY={name.casefold():key for key,value in DISTRICTS['districts'].items() for name in value['localities']}

LOCALITIES=json.loads((Path(__file__).parent/'hk-localities.json').read_text(encoding='utf-8'))
LOCALITY_BY_NAME={name.casefold():key for key,value in LOCALITIES.items() for name in value['localities']}

def convert(rows,date,category_map=None):
    result={}
    for r in rows:
        name=str(r.get('title') or '').strip()
        identity=r.get('cid') or r.get('link')
        if not name or not identity: raise ValueError('Each row needs title and cid or link')
        rating=r.get('review_rating')
        rating=None if rating in (None,'') else float(rating)
        if rating is not None and (not math.isfinite(rating) or not 0<=rating<=5): raise ValueError('Invalid rating')
        count=int(r.get('review_count') or 0)
        if count<0: raise ValueError('Invalid rating count')
        if count==0 or rating==0: rating=None
        status={'CLOSED':'closed','PERMANENTLY_CLOSED':'closed','TEMPORARILY_CLOSED':'temporarily_closed','OPEN':'operating','OPERATIONAL':'operating'}.get(str(r.get('status') or '').strip().upper(),'unknown')
        lat,lon=r.get('latitude'),r.get('longitude')
        if lat is not None or lon is not None:
            lat,lon=float(lat),float(lon)
            if not (-90<=lat<=90 and -180<=lon<=180): raise ValueError('Invalid coordinates')
        # Only exact source categories in a reviewed mapping may supply cuisine.
        # Never infer cuisine from the name, search query, or an unknown category.
        cuisines=r.get('confirmed_cuisines')
        if cuisines is None:
            cuisines=(category_map or {}).get(r.get('category'),[])
        if not all(isinstance(c,dict) and c.get('id') and c.get('label') for c in cuisines): raise ValueError('Invalid confirmed_cuisines')
        link=str(r.get('link') or '')
        parsed=urlsplit(link)
        maps_url=None
        if parsed.scheme=='https' and parsed.netloc in {'www.google.com','google.com','maps.google.com'} and (parsed.path=='/maps' or parsed.path.startswith('/maps/')):
            # Place identity is in the path; omit account/tracking parameters.
            maps_url=urlunsplit((parsed.scheme,parsed.netloc,parsed.path,'',''))
        address=r.get('complete_address') or {}
        locality=str(address.get('city') or '').strip().casefold()
        region=district_for(lat,lon)
        area=LOCALITY_BY_NAME.get(locality,'')
        key='bundled-'+hashlib.sha256(str(identity).encode()).hexdigest()[:24]
        row=dict(locality=area,region=region,maps_url=maps_url,id=key,name=name,address=r.get('address') or '',latitude=lat,longitude=lon,cuisines=cuisines,opening_hours=None,status=status,enabled=True,source='Google Maps / gosom 匯出',updated_at=date,version=1,average_rating=rating,rating_count=count)
        if key in result and result[key]!=row: raise ValueError('Conflicting duplicate identity')
        result[key]=row
    return sorted(result.values(),key=lambda r:r['id'])

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('input',type=Path);p.add_argument('--date',required=True);p.add_argument('--category-map',type=Path);p.add_argument('--output',type=Path,default=Path('app/src/main/assets/restaurants.json'));a=p.parse_args()
    raw=a.input.read_text(encoding='utf-8-sig')
    rows=json.loads(raw) if raw.lstrip().startswith('[') else [json.loads(x) for x in raw.splitlines() if x.strip()]
    category_map=json.loads(a.category_map.read_text(encoding='utf-8-sig')) if a.category_map else None
    output=convert(rows,a.date,category_map);a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(output,ensure_ascii=False,allow_nan=False,indent=2)+'\n',encoding='utf-8');print(f'{len(output)} restaurants; {sum(not r["cuisines"] for r in output)} unclassified (excluded from draws)')
