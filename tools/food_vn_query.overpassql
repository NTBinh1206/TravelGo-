[out:csv(::type,::id,::lat,::lon,name,"name:vi",amenity,cuisine,"addr:housenumber","addr:street","addr:district","addr:subdistrict","addr:city";true;"\t")][timeout:600][maxsize:1073741824];
area["ISO3166-1"="VN"][admin_level=2]->.vn;
nwr["amenity"~"^(restaurant|fast_food|food_court|cafe|ice_cream)$"]["name"](area.vn){{bbox}};
out center;
