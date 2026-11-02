"""Review and combine local gosom exports. No network requests or name-based cuisine inference."""
import argparse
import collections
import json
import math
from pathlib import Path
from prepare_bundled_catalog import convert

FOOD_CATEGORIES={'Cafe','Coffee shop','Animal cafe','Art cafe','Cafeteria','Chinese tea house','Traditional teahouse','Snack bar','Chop bar','Deli','Bistro','Brasserie','Steak house','Bar & grill','Kebab shop','Noodle shop','Pasta shop','Sushi takeaway','Vegetarian cafe and deli','Dessert shop'}

def load_rows(path):
    raw=Path(path).read_text(encoding='utf-8-sig')
    result=json.loads(raw) if raw.lstrip().startswith('[') else [json.loads(line) for line in raw.splitlines() if line.strip()]
    if not isinstance(result,list) or not all(isinstance(row,dict) for row in result):
        raise ValueError('Expected JSON array or JSONL objects')
    return result

def review(exports,date,mapping):
    unique={};duplicates=[];excluded=[];accepted=[]
    total=0
    for source,rows in exports:
        for index,row in enumerate(rows):
            total+=1
            identity=str(row.get('cid') or row.get('link') or '')
            if not identity:
                excluded.append({'name':str(row.get('title','')),'reason':'missing_identity','source':source});continue
            if identity in unique:
                previous=unique[identity]
                # Later input is the explicitly newer snapshot. Never merge same-name branches.
                duplicates.append({'identity':identity,'name':row.get('title'),'previous_source':previous[0],'selected_source':source,'changed':previous[1]!=row})
            unique[identity]=(source,row)
    for identity,(source,row) in unique.items():
        category=str(row.get('category') or '')
        lat,lon=row.get('latitude'),row.get('longitude')
        reason=None
        if not all(isinstance(v,(int,float)) and not isinstance(v,bool) and math.isfinite(v) for v in (lat,lon)):
            reason='invalid_or_missing_coordinates'
        elif not (-90<=lat<=90 and -180<=lon<=180):reason='invalid_coordinates'
        elif not (22.15<=lat<=22.57 and 113.8<=lon<=114.45):reason='outside_hk_bounding_box'
        elif (row.get('complete_address') or {}).get('country')!='HK':reason='country_not_hk_or_unconfirmed'
        elif not (category in mapping or 'restaurant' in category.lower() or category in FOOD_CATEGORIES):reason='non_restaurant_or_unclear_category'
        if not reason:
            try:
                restaurant=convert([row],date,mapping)[0]
                if not restaurant['name'] or len(restaurant['name'])>120 or len(restaurant['address'])>500:raise ValueError('Name/address length')
            except (ValueError,TypeError,KeyError) as error:
                reason='invalid_record: '+str(error)
        if reason:
            excluded.append({'name':row.get('title'),'category':category,'reason':reason,'source':source,'latitude':lat,'longitude':lon});continue
        accepted.append(restaurant)
    accepted.sort(key=lambda row:row['id'])
    unclassified=[{'name':r['name'],'id':r['id']} for r in accepted if not r['cuisines']]
    counts=collections.Counter(c['label'] for r in accepted if r['enabled'] and r['status'] not in {'closed','temporarily_closed'} for c in r['cuisines'])
    report={'date':date,'input_records':total,'unique_identities':len(unique),'duplicate_count':len(duplicates),'duplicates':duplicates,'excluded_count':len(excluded),'exclusion_reasons':dict(collections.Counter(r['reason'] for r in excluded)),'excluded':excluded,'imported':len(accepted),'closed':sum(r['status'] in {'closed','temporarily_closed'} for r in accepted),'unclassified_count':len(unclassified),'unclassified':unclassified,'recommendable':sum(bool(r['cuisines']) and r['enabled'] and r['status'] not in {'closed','temporarily_closed'} for r in accepted),'cuisine_counts':dict(sorted(counts.items()))}
    return accepted,report

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('inputs',nargs='+',type=Path,help='Oldest first; later files win for identical source IDs')
    p.add_argument('--date',required=True)
    p.add_argument('--sync-localities',action='store_true',help='Update locality memberships and generated Kotlin options')
    p.add_argument('--map',type=Path,default=Path('tools/gosom-cuisine-map.json'))
    p.add_argument('--output',type=Path,default=Path('app/src/main/assets/restaurants.json'))
    p.add_argument('--report',type=Path,default=Path('gosom-data/import-report.json'))
    a=p.parse_args();mapping=json.loads(a.map.read_text(encoding='utf-8'))
    rows,report=review([(str(path),load_rows(path)) for path in a.inputs],a.date,mapping)
    if not rows or not report['recommendable']:raise ValueError('Refusing to replace catalogue with no drawable restaurants')
    for path,value in [(a.output,rows),(a.report,report)]:
        path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,ensure_ascii=False,allow_nan=False,indent=2)+'\n',encoding='utf-8')
    if a.sync_localities:
        from sync_locality_options import sync
        sync(rows)
    print(json.dumps({key:value for key,value in report.items() if key not in {'duplicates','excluded','unclassified'}},ensure_ascii=False,indent=2))
