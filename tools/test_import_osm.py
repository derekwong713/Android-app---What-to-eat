import unittest
from import_osm import transform,cuisine
class ImportTests(unittest.TestCase):
 def test_alias_and_raw(self):
  self.assertEqual([x["id"] for x in cuisine("noodle;noodles")],["noodles"])
 def test_no_name_guess(self):
  r=transform({"type":"node","id":1,"lat":22,"lon":114,"tags":{"name":"拉麵","addr:mall":"商場","addr:floor":"2樓","addr:unit":"A舖"}},"2026-10-27T00:00:00Z")
  self.assertEqual(r["cuisines"],[]);self.assertIsNone(r["opening_hours"]);self.assertEqual(r["status"],"unknown");self.assertEqual(r["address"],"商場 2樓 A舖")
 def test_name_and_type(self):
  r=transform({"type":"way","id":1,"center":{"lat":22,"lon":114},"tags":{"name:zh-Hant":"繁體","name:zh":"中文","name":"English"}},"2026-10-27T00:00:00Z")
  self.assertEqual(r["name"],"繁體");self.assertEqual(r["object_type"],"way");self.assertEqual(r["object_id"],1)
 def test_no_fabricated_coordinate(self):
  r=transform({"type":"relation","id":1,"tags":{"name":"A"}},"2026-10-27T00:00:00Z");self.assertIsNone(r["latitude"]);self.assertIsNone(r["longitude"])
 def test_explicit_closed_only(self):
  base={"type":"node","id":1,"tags":{"name":"A","amenity":"restaurant","disused:amenity":"no"}}
  self.assertEqual(transform(base,"now")["status"],"unknown")
  base["tags"]={"name":"A","disused:amenity":"restaurant"}
  self.assertEqual(transform(base,"now")["status"],"closed")
 def test_idempotent(self):
  e={"type":"node","id":1,"tags":{"name":"A","cuisine":"noodle"}}
  self.assertEqual(transform(e,"2026-10-27T00:00:00Z"),transform(e,"2026-10-27T00:00:00Z"))
if __name__=="__main__": unittest.main()
