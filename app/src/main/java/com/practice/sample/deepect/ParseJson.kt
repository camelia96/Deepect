package com.practice.sample.deepect

import android.util.Log
import com.skt.Tmap.TMapPoint
import com.skt.Tmap.TMapPolyLine
import org.json.JSONObject

class ParseJson {
    companion object{
        fun parseJSON (root : JSONObject) : TMapPolyLine {
            val polyLine = TMapPolyLine()
            //popupListItems.clear()

            try {
                val features = root.getJSONArray("features")

                for(i in 0 until features.length()) {
                    var obj = features.getJSONObject(i)
                    val type = obj.getJSONObject("geometry").getString("type")

                    val coord = obj.getJSONObject("geometry").getJSONArray("coordinates")
                    val props = obj.getJSONObject("properties")

                    if(type.equals("Point")){
                        //Point인 경우
                        var points = arrayOfNulls<TMapPoint>(1)
                        points[0] = TMapPoint(coord.getDouble(1),coord.getDouble(0))
                        polyLine.addLinePoint(points[0])

                        var name = props.getString("name")
                        var description = props.getString("description")

                        if(name.equals("")){
                            name = description
                            description = ""
                        }

                        //popupListItems.add(PopupListItem(name, description, false, points))
                    } else if (type.equals("LineString")) {
                        //Line String인 경우

                        var points = arrayOfNulls<TMapPoint>(coord.length())
                        for(k in 0 until coord.length()) {
                            var innerCoord = coord.getJSONArray(k)

                            points[k] = TMapPoint(innerCoord.getDouble(1), innerCoord.getDouble(0))
                            polyLine.addLinePoint(points[k])
                        }

                        var name = props.getString("name")
                        var description = props.getString("description")

                        if(name.equals("")){
                            name = description
                            description = ""
                        }
                        //popupListItems.add(new PopupListItem(name, description, false, points));
                    }

                    //의미없는 정보 제거
                    //if (popupListItems.get(popupListItems.size()-1).mainText.startsWith(","))
                    //                    popupListItems.remove(popupListItems.size()-1);
                }
            } catch (ex : Exception){
                Log.d("EXC:", "an error occur while parsing json / " + ex.toString());
            }
            return polyLine
        }
    }
}