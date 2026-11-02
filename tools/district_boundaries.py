"""Offline point-in-polygon lookup against HAD district boundaries (WGS84 lon/lat)."""
import json
import math
from functools import lru_cache
from pathlib import Path

_DATA=Path(__file__).parent/'data/hk-district-boundaries.geojson'
_NAMES=json.loads((Path(__file__).parent/'hk-districts.json').read_text(encoding='utf-8'))['districts']
_BY_LABEL={value['label']:key for key,value in _NAMES.items()}

def ring_contains(x,y,ring):
    """Return -1 on an edge, 1 inside, 0 outside. Edges never silently pick a side."""
    inside=False
    for i,a in enumerate(ring):
        b=ring[i-1];ax,ay=a;bx,by=b
        cross=(x-ax)*(by-ay)-(y-ay)*(bx-ax)
        if abs(cross)<=1e-12 and min(ax,bx)-1e-10<=x<=max(ax,bx)+1e-10 and min(ay,by)-1e-10<=y<=max(ay,by)+1e-10:
            return -1
        if (ay>y)!=(by>y) and x<(bx-ax)*(y-ay)/(by-ay)+ax:
            inside=not inside
    return int(inside)

def polygon_contains(x,y,rings):
    outer=ring_contains(x,y,rings[0])
    if outer!=1:return outer
    for hole in rings[1:]:
        value=ring_contains(x,y,hole)
        if value==-1:return -1
        if value==1:return 0
    return 1

def compile_boundaries():
    data=json.loads(_DATA.read_text(encoding='utf-8-sig'))
    if data.get('type')!='FeatureCollection' or len(data['features'])!=18:raise ValueError('Expected 18 HAD districts')
    result=[]
    for feature in data['features']:
        key=_BY_LABEL[feature['properties']['地區']]
        geometry=feature['geometry']
        if geometry['type']=='Polygon':polygons=[geometry['coordinates']]
        elif geometry['type']=='MultiPolygon':polygons=geometry['coordinates']
        else:raise ValueError('Unsupported boundary geometry')
        compiled=[]
        for rings in polygons:
            xs=[p[0] for p in rings[0]];ys=[p[1] for p in rings[0]]
            compiled.append(((min(xs),min(ys),max(xs),max(ys)),rings))
        result.append((key,compiled))
    if len({key for key,_ in result})!=18:raise ValueError('Duplicate district')
    return result

_BOUNDARIES=compile_boundaries()
@lru_cache(maxsize=8192)
def district_for(latitude,longitude):
    if not all(isinstance(v,(int,float)) and math.isfinite(v) for v in (latitude,longitude)):return 'unknown'
    matches=[]
    for key,polygons in _BOUNDARIES:
        for (west,south,east,north),rings in polygons:
            if west<=longitude<=east and south<=latitude<=north:
                match=polygon_contains(longitude,latitude,rings)
                if match==-1:return 'unknown'
                if match==1:
                    matches.append(key);break
    return matches[0] if len(matches)==1 else 'unknown'
