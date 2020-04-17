package com.practice.sample.deepect

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PointF
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.practice.sample.deepect.ParseJson.Companion.parseJSON
import com.practice.sample.deepect.Utils.LocationUtils
import com.skt.Tmap.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(),  NavigationView.OnNavigationItemSelectedListener {

    private var selectionMode = false
    private var navigationMode = false

    private var oldMarker: TMapMarkerItem? = null
    private var destination : TMapPoint? = null

    private lateinit var mapView : TMapView

    private lateinit var gpsManager: GpsManager
    private lateinit var pathManager: PathManager
    private lateinit var directionManager: DirectionManager

    private lateinit var timer : Timer
    private lateinit var timerTask: TimerTask

    var Start_Point : TMapPoint? = null
    var Destination_Point : TMapPoint? = null
    var Current_Location : Location? = null

    var start  = false


    private lateinit var Address : String
    private var m_Latitude = 0.0
    private var m_Longitude = 0.0

    private val REQUEST_SEARCH = 0x0001
    private val REQUEST_HISTORY = 0x0002




    //Marker
    private var markerIdList = ArrayList<String>()

    //PopupList Item List
    //private var popupListItems = ArrayList<PopupListItem>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        mapView = TMapView(this)

        try {
            //gps = TMapGpsManager(this)
            initView()

            GpsManager.init(this)
            gpsManager = GpsManager.getInstance()
            gpsManager.setOnLocationListener(locationListener)


            pathManager = PathManager.getInstance()

            directionManager = DirectionManager().getInstance()


            checkPermission()


            btnPath.setOnClickListener {
                setNavigationMode(true)
            }

            moveToCurrentLocation()
        } catch (e: Exception){
            Log.d("Exception : ", "cant initialize ${e.message}")
        }
    }


    private fun checkPermission() {
        if(ActivityCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            return
        }
    }

    private fun initView() {
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this,
            drawer_layout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        setMapIcon()
        mapView.setSKTMapApiKey("l7xxe5f30ecdf6da43cba53ee7260d1e8675")
        //mapView.setLocationPoint(gps.location.longitude, gps.location.latitude)
        frameMap.addView(mapView)
        setSelectionMode(!selectionMode)


        mapView.setOnApiKeyListener(object : TMapView.OnApiKeyListenerCallback{
            override fun SKTMapApikeySucceed() {
                Log.d("SKTMapApikeySucceed", "ApiSucceed")
            }

            override fun SKTMapApikeyFailed(errorMsg: String?) {
                Log.d("SKTMapApikeyFailed " ,errorMsg)
            }
        })
    }

    fun setMapIcon() {
        val currentMarker = TMapMarkerItem()

        val bitmap = BitmapFactory.decodeResource(this.resources, R.drawable.poi_here)

        mapView.setZoomLevel(16)
        mapView.setIcon(bitmap)
        mapView.addMarkerItem("CurrentMarker", currentMarker)
        mapView.setIconVisibility(true)

    }

    private fun setSelectionMode(isSelectionMode : Boolean) {
        this.selectionMode = isSelectionMode

        if (isSelectionMode) {
            toolbar.setTitle("도착지를 설정하세요")

            mapView.setOnLongClickListenerCallback(object : TMapView.OnLongClickListenerCallback {
                override fun onLongPressEvent(
                    p0: ArrayList<TMapMarkerItem>?,
                    p1: ArrayList<TMapPOIItem>?,
                    p2: TMapPoint
                ) {
                    val builder = AlertDialog.Builder(this@MainActivity)
                        .setTitle("안내")
                        .setMessage("도착지로 설정하시겠습니까?")
                        .setPositiveButton("예", object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                setDestination(p2)

                                // turn off selection mode
                                setSelectionMode(false);

                                getRoute(p2)

                            }
                        })
                        .setNegativeButton("아니오", null).show()
                }
            })
        }
    }


    private fun setNavigationMode(isNavigationMode : Boolean){
        this.navigationMode = isNavigationMode
        if(isNavigationMode) {
            try {
                if (destination == null) {
                    Toast.makeText(this, "먼저 도착지를 선택하세요.", Toast.LENGTH_SHORT).show()
                    setNavigationMode(false)
                    setSelectionMode(true)
                    return
                }
                val mapData = TMapData()

                //이부분 어케할꺼야
                val currentLocation = Current_Location
                val startPoint =  TMapPoint(currentLocation!!.latitude, currentLocation.longitude)
                //

                if(destination != null) {
                    val endpoint = destination
                    //getRoute(endpoint!!)
                    val currentLocation : Location? = gpsManager.getCurrentLocation()
                    val startPoint = TMapPoint(currentLocation!!.latitude , currentLocation!!.longitude)

                    if(startPoint != null) {
                        val routeApi = RouteApi(this, startPoint, endpoint!!, object : RouteApi.EventListener {
                            override fun onApiResult(jsonString : String) {
                                Toast.makeText(this@MainActivity, "요청성공", Toast.LENGTH_SHORT).show()
                                try {
                                    val objects : JSONObject = JSONObject(jsonString)
                                    Log.d("result:", objects.toString())

                                    val polyLine = parseJSON(objects)

                                    runOnUiThread(object : Runnable {
                                        override fun run() {
                                            val linePoints : ArrayList<TMapPoint> = polyLine.getLinePoint()
                                            markerIdList.clear()

                                            var i = 0
                                            mapView.removeAllMarkerItem()

                                            for (p : TMapPoint in linePoints) {
                                                val markerItem = TMapMarkerItem()
                                                markerItem.tMapPoint = p

                                                val id = "i" + i++
                                                Log.d("id", id)
                                                markerItem.id = id

                                                mapView.addMarkerItem(id, markerItem)

                                                markerIdList.add(id)
                                            }

                                            mapView.addTMapPath(polyLine)


                                            pathManager.setPolyLine(polyLine)

                                            start = true

                                        }
                                    })
                                } catch (ex : Exception) {
                                    Log.d("EXC", ex.message)
                                }

                            }

                            override fun onApiFailed() {
                                Toast.makeText(this@MainActivity, "요청실패", Toast.LENGTH_SHORT).show()
                            }
                        })
                        routeApi.start()
                    }
                    setTrackingMode()
                    setSelectionMode(false)
                }



                Toast.makeText(this, "길 안내를 시작합니다.", Toast.LENGTH_SHORT).show()
                // always compass mode
                mapView.setOnTouchListener { v, event ->
                    mapView.setCompassMode(true)
                    true
                }

                moveToCurrentLocation()

                Log.d("Start", "$start")

                timer = Timer(true)
                timerTask = object : TimerTask() {
                    override fun run() {
                        updateDirection()
                    }
                }
                timer.schedule(timerTask, 300, 1)



            } catch (ex : Exception) {
                Log.d("EXC", ex.message)
                setNavigationMode(false)
            }
        } else {
            try {
                timer.cancel()
            } catch (e : Exception){
                Log.d("error", "${e.message}")
            }
        }
    }

    private fun moveToCurrentLocation() {
        try{
            val currentLocation = gpsManager.getCurrentLocation()
            Current_Location = currentLocation
            if (currentLocation != null) {
                mapView.setLocationPoint(currentLocation.longitude, currentLocation.latitude)
                mapView.setCenterPoint(currentLocation.longitude, currentLocation.latitude)
            }
        } catch (ex : Exception) {
            Toast.makeText(this, "현재 위치를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateDirection() {
        val distThreshold = 20.0f // 20 meter

        var nearestPoint: TMapPoint? = null
        try {

            nearestPoint = pathManager.nearestPoint
            Log.d("nearest:", "" + pathManager.getNearestIndex() + pathManager.nearestPoint);
//            double distance = nearestVector.getDistance();
            val currentLocation = gpsManager.getCurrentLocation()
            val nearestLocation =
                Location(LocationManager.GPS_PROVIDER)
            nearestLocation.longitude = nearestPoint!!.longitude
            nearestLocation.latitude = nearestPoint.latitude
            val distance: Float =
                LocationUtils().distanceBetween(currentLocation!!, nearestLocation)

                       Log.d("distance:", "" + distance);
            if (distance < distThreshold) {
                val nearestIndex : Int? = pathManager.nearestIndex
                // remove nearest marker and marker id
                val targetMarkerId = markerIdList[nearestIndex!!]
                mapView.removeMarkerItem(targetMarkerId)
                markerIdList.removeAt(nearestIndex)
                if (pathManager.hasNext()) { // Path has next point

                    nearestPoint = pathManager.nearestPoint
                } else { // Navigation Complete !!!

                    nearestPoint = null
                    Toast.makeText(this, "목적지에 도착하였습니다.", Toast.LENGTH_SHORT).show()
                    setNavigationMode(false)
                    return
                }
            } else { // out of 20.0 meters

            }
        } catch (ex: java.lang.Exception) {
            Log.d("Exception:", ex.message)
        }
    }


    private fun getRoute(endPoint: TMapPoint){

        val currentLocation : Location? = gpsManager.getCurrentLocation()
        val startPoint = TMapPoint(currentLocation!!.latitude , currentLocation!!.longitude)

        if(startPoint != null) {
            val routeApi = RouteApi(this, startPoint, endPoint, object : RouteApi.EventListener {
                override fun onApiResult(jsonString : String) {
                    Toast.makeText(this@MainActivity, "요청성공", Toast.LENGTH_SHORT).show()
                    try {
                        val objects : JSONObject = JSONObject(jsonString)
                        Log.d("result:", objects.toString())

                        val polyLine = parseJSON(objects)

                        runOnUiThread(object : Runnable {
                            override fun run() {
                                val linePoints : ArrayList<TMapPoint> = polyLine.getLinePoint()
                                markerIdList.clear()

                                var i = 0
                                mapView.removeAllMarkerItem()

                                for (p : TMapPoint in linePoints) {
                                    val markerItem = TMapMarkerItem()
                                    markerItem.tMapPoint = p

                                    val id = "i" + i++
                                    Log.d("id", id)
                                    markerItem.id = id

                                    mapView.addMarkerItem(id, markerItem)

                                    markerIdList.add(id)
                                }

                                mapView.addTMapPath(polyLine)


                                pathManager.setPolyLine(polyLine)

                            }
                        })
                    } catch (ex : Exception) {
                        Log.d("EXC", ex.message)
                    }

                }

                override fun onApiFailed() {
                    Toast.makeText(this@MainActivity, "요청실패", Toast.LENGTH_SHORT).show()
                }
            })
            routeApi.start()
        }
    }

    private fun setDestination(destination : TMapPoint) {
        //clear previous destination
        try{
            mapView.removeMarkerItem("도착지")
        } catch (ex : Exception){

        }

        this.destination = destination
        Destination_Point = destination


        //add destination marker on TMap View
        val marker = TMapMarkerItem()
        marker.id = "도착지"
        marker.tMapPoint = destination
        oldMarker = marker;

        mapView.addMarkerItem("도착지", marker)
        Log.d("Info::", "도착지 설정 완료")

    }

    /////////////////////////////////////////////////////////////////////////////////////////////

    fun ClickDestination() {
        Toast.makeText(this@MainActivity, "원하시는 도착 지점을 터치한 후 길안내 시작버튼을 눌러주세요", Toast.LENGTH_SHORT).show()

        mapView.setOnClickListenerCallBack(object : TMapView.OnClickListenerCallback {
            override fun onPressEvent(
                p0: java.util.ArrayList<TMapMarkerItem>?,
                p1: java.util.ArrayList<TMapPOIItem>?,
                tMapPoint: TMapPoint,
                p3: PointF?
            ): Boolean {
                val tMapData = TMapData()
                tMapData.convertGpsToAddress(tMapPoint.latitude , tMapPoint.longitude, object : TMapData.ConvertGPSToAddressListenerCallback {
                    override fun onConvertToGPSToAddress(strAddress: String) {
                        Address = strAddress
                        LogManager.printLog("선택한 위치의 주소는 $strAddress")
                    }
                })
                Toast.makeText(this@MainActivity, "선택하신 위치의 주소는 $Address 입니다", Toast.LENGTH_SHORT).show()

                return false
            }

            override fun onPressUpEvent(
                p0: java.util.ArrayList<TMapMarkerItem>?,
                p1: java.util.ArrayList<TMapPOIItem>?,
                tMapPoint: TMapPoint?,
                p3: PointF?
            ): Boolean {
                Destination_Point = tMapPoint

                return false
            }
        })
    }

    fun SearchDestination() {
        val builder =  AlertDialog.Builder(this)
        builder.setTitle("POI 통합 검색")

        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("확인", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                val strData = input.text.toString()
                val tMapData = TMapData()

                tMapData.findAllPOI(strData, object : TMapData.FindAllPOIListenerCallback {
                    override fun onFindAllPOI(poiItem: java.util.ArrayList<TMapPOIItem>) {
                        for(i in 0 until poiItem.size) {
                            val item : TMapPOIItem = poiItem.get(i)
                            LogManager.printLog("POI NAME : ${item.poiName.toString()} , " +
                                    "Address : ${item.poiAddress.replace("null","")} , " +
                                    "Point ${item.poiPoint.toString()}")
                            Address = item.poiAddress
                            Destination_Point = item.poiPoint

                        }
                    }
                })
            }
        })
        Toast.makeText(this, "입력하신 주소는 $Address 입니다.", Toast.LENGTH_SHORT).show()
        builder.setNegativeButton("취소", object : DialogInterface.OnClickListener{
            override fun onClick(dialog: DialogInterface?, which: Int) {
                dialog?.cancel()
            }
        })
        builder.show()
    }

    fun StartGuidance() {
        mapView.removeTMapPath()

        //setTrackingMode()

        val point1 = mapView.locationPoint
        val point2 = Destination_Point

        val tMapData = TMapData()

        tMapData.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, point1, point2, object : TMapData.FindPathDataListenerCallback{
            override fun onFindPathData(polyLine: TMapPolyLine) {
                polyLine.lineColor = Color.RED
                mapView.addTMapPath(polyLine)
            }
        })

        val start : Bitmap = BitmapFactory.decodeResource(this.resources , R.drawable.poi_start)
        val end : Bitmap = BitmapFactory.decodeResource(this.resources , R.drawable.poi_end)
        mapView.setTMapPathIcon(start, end)
        mapView.zoomToTMapPoint(point1, point2)
    }

    fun getLocationPoint() {
        val point = mapView.locationPoint

        val Latitude = point.latitude
        val Longitude = point.longitude

        m_Latitude = Latitude
        m_Longitude = Longitude

        LogManager.printLog("Latitude : $Latitude Longitude : $Longitude")
        //val strResult = String.format("Latitude = %f Longitude = %f", Latitude, Longitude)
        //Common.showAlertDialog(this, "", strResult)
    }

    fun setTrackingMode() {mapView.setTrackingMode(mapView.getIsTracking())}


    override fun onBackPressed() {
        if(drawer_layout.isDrawerOpen(GravityCompat.START)){
            drawer_layout.closeDrawer(GravityCompat.START)
        }
        else{
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == Activity.RESULT_OK){
            when(requestCode){
                REQUEST_SEARCH -> {
                    setNavigationMode(false)

                    val name = data?.getStringExtra("POI")
                    val longitude = data?.getDoubleExtra("LON", 0.0)
                    val latitude = data?.getDoubleExtra("LAT", 0.0)

                    if(latitude != null && longitude != null) {
                        val mapPoint = TMapPoint(latitude, longitude)
                        setDestination(mapPoint)
                        mapView.setCenterPoint(longitude, latitude)
                    }

                    appendToHistoryFile(name!!, latitude!!, longitude!!)
                }
                REQUEST_HISTORY ->{
                    setNavigationMode(false)

                    val name = data?.getStringExtra("POI")
                    val longitude = data?.getDoubleExtra("LON", 0.0)
                    val latitude = data?.getDoubleExtra("LAT", 0.0)

                    if(latitude != null && longitude != null) {
                        val mapPoint = TMapPoint(latitude, longitude)
                        setDestination(mapPoint)
                        mapView.setCenterPoint(longitude, latitude)
                    }

                }
                else -> {
                    super.onActivityResult(requestCode, resultCode, data)
                }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        try {
            when(id) {
                R.id.nav_search -> {
                    val intent = Intent(this , SearchActivity::class.java)
                    startActivityForResult(intent, REQUEST_SEARCH)
                }
                R.id.nav_history -> {
                    val intent = Intent(this, HistoryActivity::class.java)
                    startActivityForResult(intent, REQUEST_HISTORY)
                }
            }
        } catch( e : Exception){
            Log.d("Exception:", e.message)
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun appendToHistoryFile(name : String, latitude : Double , longitude : Double) {
        try {
            val bw = BufferedWriter(FileWriter(File(filesDir, "history.txt"), true))
            bw.append(String.format("%s %f %f", name, longitude, latitude))
            bw.newLine()
            bw.close()
        } catch (e : Exception){
            Log.d("FileWirteException", e.message)
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    val locationListener = object : LocationListener{
        override fun onLocationChanged(location: Location?) {
            try {
                if(location != null) {
                    val distanceFromPrev: Float = location.distanceTo(gpsManager.getLastLocation())

                    if ((distanceFromPrev < 20.0f) || (distanceFromPrev > 100.0f)) {
                        gpsManager.setLastLocation(location)

                        mapView.setLocationPoint(location.longitude , location.latitude)
                        if(navigationMode){
                            moveToCurrentLocation()
                            //updateDirection()
                        }
                    }
                }
            } catch (e : Exception){ }
        }

        override fun onProviderDisabled(provider: String?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onProviderEnabled(provider: String?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    override fun onResume() {
        super.onResume()
        //gpsManager.start()
    }

    override fun onPause() {
        super.onPause()
        //gpsManager.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        //gpsManager.stop()
    }

}



