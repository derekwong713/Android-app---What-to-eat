import unittest
from review_gosom_catalog import review

MAP={'Ramen restaurant':[{'id':'ramen','label':'拉麵'}]}
def row(**values):
    r=dict(cid='1',title='測試拉麵',category='Ramen restaurant',latitude=22.3,longitude=114.1,complete_address={'country':'HK'},review_rating=4.2,review_count=10)
    r.update(values);return r
class ReviewCatalogTest(unittest.TestCase):
    def run_review(self,rows):return review([('test',rows)],'2026-11-03',MAP)
    def test_country_and_coordinates_both_required(self):
        rs,report=self.run_review([row(cid='1',complete_address={'country':'CN'}),row(cid='2',latitude=35.7,longitude=139.7),row(cid='3',latitude=None)])
        self.assertEqual(rs,[]);self.assertEqual(report['excluded_count'],3)
    def test_nonfood_is_excluded_despite_restaurant_name(self):
        rs,report=self.run_review([row(category='Shopping mall')]);self.assertEqual(rs,[])
    def test_generic_restaurant_is_retained_unclassified(self):
        rs,report=self.run_review([row(category='Restaurant')]);self.assertEqual(len(rs),1);self.assertEqual(report['recommendable'],0)
    def test_closed_is_retained_but_not_drawable(self):
        rs,report=self.run_review([row(status='CLOSED')]);self.assertEqual(rs[0]['status'],'closed');self.assertEqual(report['recommendable'],0)
    def test_description_is_not_operating_status(self):
        rs,report=self.run_review([row(status='Brunch')]);self.assertEqual(rs[0]['status'],'unknown');self.assertEqual(report['recommendable'],1)
    def test_newer_snapshot_wins_without_merging_branches(self):
        rs,report=review([('old',[row()]),('new',[row(review_rating=4.5),row(cid='2')])],'2026-11-03',MAP)
        self.assertEqual(len(rs),2);self.assertEqual(report['duplicate_count'],1);self.assertEqual(sum(r['average_rating']==4.5 for r in rs),1)
    def test_no_rating_is_not_zero_stars(self):
        rs,_=self.run_review([row(review_rating=0,review_count=0)]);self.assertIsNone(rs[0]['average_rating'])
    def test_invalid_rating_is_quarantined(self):
        rs,report=self.run_review([row(review_rating=8)]);self.assertEqual(rs,[]);self.assertEqual(report['excluded_count'],1)
if __name__=='__main__':unittest.main()
