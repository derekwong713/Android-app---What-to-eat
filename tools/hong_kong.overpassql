[out:json][timeout:240];
area["ISO3166-2"="CN-HK"]["boundary"="administrative"]->.hk;
(nwr["amenity"~"^(restaurant|fast_food|cafe|food_court)$"](area.hk);
 nwr["disused:amenity"~"^(restaurant|fast_food|cafe|food_court)$"](area.hk);
 nwr["abandoned:amenity"~"^(restaurant|fast_food|cafe|food_court)$"](area.hk););
out meta center;
