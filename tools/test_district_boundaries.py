import unittest
from district_boundaries import district_for,ring_contains,polygon_contains,_BOUNDARIES

class DistrictBoundaryTest(unittest.TestCase):
    def test_all_18_official_districts_loaded(self):
        self.assertEqual(len(_BOUNDARIES),18)
    def test_nine_restored_localities_have_coordinate_districts(self):
        points=[(22.324802,114.165726,'yau_tsim_mong'),(22.2790293,114.1853345,'wan_chai'),(22.2619697,114.1295001,'southern'),(22.2812868,114.1518082,'central_western'),(22.3361527,114.1485636,'sham_shui_po'),(22.337282,114.17178,'sham_shui_po'),(22.425581,114.2123862,'sha_tin'),(22.428285,114.2004437,'tai_po'),(22.3220854,113.9448604,'islands')]
        for lat,lon,district in points:self.assertEqual(district_for(lat,lon),district)
    def test_same_locality_can_have_records_in_different_districts(self):
        self.assertEqual(district_for(22.2882304,114.1918253),'eastern')
        self.assertEqual(district_for(22.2790293,114.1853345),'wan_chai')
        self.assertEqual(district_for(22.3363741,114.1817914),'kowloon_city')
    def test_edges_holes_and_outside(self):
        outer=[[0,0],[10,0],[10,10],[0,10],[0,0]]
        hole=[[2,2],[4,2],[4,4],[2,4],[2,2]]
        self.assertEqual(polygon_contains(1,1,[outer,hole]),1)
        self.assertEqual(polygon_contains(3,3,[outer,hole]),0)
        self.assertEqual(polygon_contains(2,3,[outer,hole]),-1)
        self.assertEqual(ring_contains(0,5,outer),-1)
        self.assertEqual(ring_contains(11,1,outer),0)
    def test_missing_invalid_and_overseas_coordinates_stay_unknown(self):
        for lat,lon in [(None,None),(float('nan'),114),(35.7,139.7),(0,0)]:self.assertEqual(district_for(lat,lon),'unknown')
if __name__=='__main__':unittest.main()
