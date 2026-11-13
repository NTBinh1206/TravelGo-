[out:csv(::type,::id,::lat,::lon,name,"name:vi",amenity,cuisine,"addr:housenumber","addr:street","addr:district","addr:subdistrict","addr:city",opening_hours,end_date;true;"\t")][timeout:120];
nwr["amenity"~"^(restaurant|fast_food|food_court|cafe|ice_cream)$"]["name"]{{bbox}};
out center;
