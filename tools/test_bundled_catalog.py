import json
import unittest
from pathlib import Path
from prepare_bundled_catalog import convert

MAP=json.loads((Path(__file__).parent/'gosom-cuisine-map.json').read_text(encoding='utf-8'))
class BundledImportTest(unittest.TestCase):
    def row(self,**values):
        return dict(title='測試店',cid='123',category='Ramen restaurant',review_rating=4.2,review_count=10,latitude=22.3,longitude=114.1,**values)
    def test_exact_source_mapping(self):
        self.assertEqual(convert([self.row()],'2026-11-02',MAP)[0]['cuisines'],[{'id':'ramen','label':'拉麵'}])
    def test_name_and_query_do_not_supply_cuisine(self):
        r=self.row();r.update(title='拉麵店',category='Chop bar',input_id='旺角 拉麵')
        self.assertEqual(convert([r],'2026-11-02',MAP)[0]['cuisines'],[])
    def test_explicit_empty_classification_is_preserved(self):
        self.assertEqual(convert([self.row(confirmed_cuisines=[])],'2026-11-02',MAP)[0]['cuisines'],[])
    def test_private_and_review_fields_are_not_packaged(self):
        result=convert([self.row(user_reviews=['private'],emails=['private'])],'2026-11-02',MAP)[0]
        self.assertNotIn('emails',result);self.assertNotIn('user_reviews',result)
        self.assertEqual(result['average_rating'],4.2)
    def test_duplicate_identity_not_name(self):
        r=self.row();other=dict(r,cid='456')
        self.assertEqual(len(convert([r,r,other],'2026-11-02',MAP)),2)
    def test_place_link_preserved_without_tracking(self):
        r=self.row(link='https://www.google.com/maps/place/Ramen/data=!test?authuser=0&hl=en')
        self.assertEqual(convert([r],'2026-11-02',MAP)[0]['maps_url'],'https://www.google.com/maps/place/Ramen/data=!test')
    def test_external_link_rejected(self):
        self.assertIsNone(convert([self.row(link='https://www.google.com.evil.example/maps/place/a')],'2026-11-02',MAP)[0]['maps_url'])
    def test_district_uses_coordinates_instead_of_source_city(self):
        r=self.row(complete_address={'city':'uncertain','state':'unknown'})
        r.update(latitude=22.3193,longitude=114.1694)
        self.assertEqual(convert([r],'2026-11-03',MAP)[0]['region'],'yau_tsim_mong')
        r['complete_address']['city']='Causeway Bay'
        self.assertEqual(convert([r],'2026-11-03',MAP)[0]['region'],'yau_tsim_mong')
    def test_missing_coordinates_do_not_guess_district(self):
        r=self.row(complete_address={'city':'Mong Kok'})
        r.update(latitude=None,longitude=None)
        self.assertEqual(convert([r],'2026-11-03',MAP)[0]['region'],'unknown')
    def test_locality_kept_separate_from_ambiguous_district(self):
        r=self.row(complete_address={'city':'Causeway Bay'})
        r.update(latitude=22.2790293,longitude=114.1853345)
        out=convert([r],'2026-11-03',MAP)[0]
        self.assertEqual(out['region'],'wan_chai')
        self.assertEqual(out['locality'],'area_causeway_bay')
        r['complete_address']['city']='Mong Kok'
        self.assertEqual(convert([r],'2026-11-03',MAP)[0]['locality'],'area_mong_kok')
    def test_all_18_districts_have_unique_localities(self):
        from prepare_bundled_catalog import DISTRICTS
        districts=DISTRICTS['districts']
        self.assertEqual(len(districts),18)
        names=[n.casefold() for d in districts.values() for n in d['localities']]
        self.assertEqual(len(names),len(set(names)))
    def test_invalid_rating_rejected(self):
        r=self.row();r['review_rating']=6
        with self.assertRaises(ValueError):convert([r],'2026-11-02',MAP)
if __name__=='__main__':unittest.main()
