"""Fetch a pinned PostgreSQL WASM test runtime from npm with integrity verification. No cloud account."""
from pathlib import Path
import urllib.request,json,hashlib,base64,tarfile,io
root=Path(__file__).resolve().parents[1]/'.tools'/'pglite'
version='0.3.14'
metadata=json.load(urllib.request.urlopen(f'https://registry.npmjs.org/@electric-sql/pglite/{version}',timeout=30))
archive=urllib.request.urlopen(metadata['dist']['tarball'],timeout=120).read()
algorithm,expected=metadata['dist']['integrity'].split('-',1)
assert algorithm=='sha512' and base64.b64encode(hashlib.sha512(archive).digest()).decode()==expected
root.mkdir(parents=True,exist_ok=True)
with tarfile.open(fileobj=io.BytesIO(archive),mode='r:gz') as package:
 for entry in package.getmembers():
  target=(root/entry.name).resolve()
  assert target.is_relative_to(root.resolve()) and not entry.issym() and not entry.islnk()
 package.extractall(root,filter='data')
print('Verified PGlite',version,'in',root)
