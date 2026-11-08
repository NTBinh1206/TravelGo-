[out:csv(::type,::id,::lat,::lon,name,"name:vi","name:en",tourism,historic,natural,leisure,amenity,religion,place,boundary,waterway,building,heritage,wikipedia,wikidata,image,wikimedia_commons,"addr:district","addr:city","addr:province";true;"\t")][timeout:300];
area["ISO3166-1"="VN"][admin_level=2]->.vn;
(
  nwr["tourism"~"^(attraction|museum|viewpoint|theme_park|zoo|aquarium|gallery)$"]["name"](area.vn);
  nwr["historic"~"^(monument|castle|ruins|archaeological_site|fort|city_gate|citadel|temple)$"]["name"](area.vn);
  nwr["natural"~"^(beach|waterfall|cave_entrance|bay|cape|hot_spring)$"]["name"](area.vn);
  nwr["natural"~"^(peak|volcano)$"]["name"]["wikidata"](area.vn);
  nwr["waterway"="waterfall"]["name"](area.vn);
  nwr["leisure"~"^(nature_reserve|water_park)$"]["name"](area.vn);
  nwr["leisure"="park"]["name"]["wikidata"](area.vn);
  nwr["boundary"="national_park"]["name"](area.vn);
  nwr["amenity"="place_of_worship"]["name"]["wikidata"](area.vn);
  nwr["amenity"="place_of_worship"]["name"]["heritage"](area.vn);
  nwr["place"~"^(island|islet)$"]["name"]["wikidata"](area.vn);
  node["place"~"^(city|town)$"]["name"](area.vn);
);
out center;
