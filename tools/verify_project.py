"""Fast structural checks in addition to Gradle's compiler, tests and lint."""
from pathlib import Path
import re
import tomllib
import xml.etree.ElementTree as ET
import zipfile

root=Path(__file__).resolve().parents[1]
res=root/'app/src/main/res'
catalogs={p.parent.name:{e.attrib['name'] for e in ET.parse(p).getroot()} for p in res.glob('values*/strings.xml')}
base=catalogs['values']
for locale,keys in catalogs.items():
    assert keys==base, f'Translation key mismatch: {locale}'
sources=list((root/'app/src').rglob('*.kt'))
for source in sources:
    missing=set(re.findall(r'R\.string\.(\w+)',source.read_text(encoding='utf-8')))-base
    assert not missing, f'Missing resource references in {source}: {missing}'
for folder in ['core/model','domain/filter','domain/draw']:
    for source in (root/'app/src/main/java/hk/whatoeat'/folder).glob('*.kt'):
        assert not re.search(r'^import (android\.|androidx\.)',source.read_text(),re.M), source
with (root/'gradle/libs.versions.toml').open('rb') as handle:
    versions=tomllib.load(handle)['versions']
    assert all('+' not in version and not version.startswith('latest') for version in versions.values())
with zipfile.ZipFile(root/'gradle/wrapper/gradle-wrapper.jar') as wrapper:
    assert 'org/gradle/wrapper/GradleWrapperMain.class' in wrapper.namelist()
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
ns='{http://schemas.android.com/apk/res/android}'
assert manifest.find('application').get(ns+'allowBackup')=='false'
assert manifest.find('application').get(ns+'dataExtractionRules')=='@xml/data_extraction_rules'
rules=ET.parse(res/'xml/data_extraction_rules.xml').getroot()
for mode in ['cloud-backup','device-transfer']:
    excluded={e.get('domain') for e in rules.find(mode).findall('exclude') if e.get('path')=='.'}
    assert {'root','file','database','sharedpref','external','device_root','device_file','device_database','device_sharedpref'} <= excluded
permissions={e.get(ns+'name') for e in manifest.findall('uses-permission')}
assert 'android.permission.ACCESS_BACKGROUND_LOCATION' not in permissions
assert {'android.permission.ACCESS_COARSE_LOCATION','android.permission.ACCESS_FINE_LOCATION'} <= permissions
print(f'PASS: {len(sources)} Kotlin files, {len(base)} string keys × {len(catalogs)} locales, pinned catalog, native manifest, pure domain imports, real Gradle Wrapper')
