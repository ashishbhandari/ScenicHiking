Scenic Hiking:


The new application should include the following features: 

- To enable a user  to walk  along a  route (see below), show a  map (use Mapbox, free tier) with your current position and bearing. - DONE

- A new custom route can be created by adding markers to the map via a long click.  Those markers may  stand  for  special spots  with a beautiful 
scenic view or other points of interest along the route which you do not want to miss. These markers are automatically connected with each other in  the  order  of  their  creation  via  a  straight  polyline  to  represent  the 
actual route. 

- https://docs.mapbox.com/android/maps/examples/recycler-view-directions/ connecting dots between points [markers are automatically connected with each other in  the  order  of  their  creation]
- https://docs.mapbox.com/android/maps/examples/marker-following-route/ [user  to walk  along a  route ] 
- https://docs.mapbox.com/android/maps/examples/use-an-image-source/
- https://docs.mapbox.com/android/maps/examples/value-animator-icon-animation/
- https://docs.mapbox.com/android/maps/examples/animate-the-map-camera/
- https://docs.mapbox.com/android/maps/examples/show-a-users-location/
- https://docs.mapbox.com/android/maps/examples/recyclerview-interaction/ for getting a focus of the selected section in a map
- https://docs.mapbox.com/android/maps/examples/snaking-directions-route/
- https://docs.mapbox.com/android/maps/examples/fit-camera-in-bounding-box/ : [button is needed which centers the map section so that all markers are visible.]
- https://docs.mapbox.com/android/maps/examples/location-component-camera-options/
- https://docs.mapbox.com/android/maps/examples/location-picker/
- https://docs.mapbox.com/android/maps/examples/icon-size-change-on-click/ [visible within the list as well as in the map view ] / all need to create bottom loading window holding list with icons
- https://docs.mapbox.com/android/maps/examples/support-map-fragment/ [Map Fragment support]
- https://docs.mapbox.com/android/maps/examples/symbollayer-mapillary/ [Scenic images]

§  The user should also be able to get an overview of the created route. So, a button is needed which centers the map section so that all markers are visible. 

§  For planning purposes, provide a list of all set markers in a sperate view. Each list entry can be toggled as a favorite. This should be visible within 
the list as well as in the map view. 

§  Please also write two test cases. No UI testing needed. 
 
 
Information 
 
§  Consider only portrait mode. 
§  Open Source frameworks/libraries can be used. 
§  It is not necessary to persist the custom route or allow deletion of routes. 
§  Create  a  Git  repository  for  your  project  and  include  this  along  with  a runnable APK file into your result set. 




