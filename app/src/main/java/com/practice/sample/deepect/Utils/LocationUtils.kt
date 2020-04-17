package com.practice.sample.deepect.Utils

import android.location.Location
import com.practice.sample.deepect.DirectionManager

class LocationUtils{
    fun directionBetween(src : Location , dest : Location) : Float {
        val directionManager = DirectionManager
        return directionManager.instance!!.getDirectionBetween(src, dest)
    }
    fun distanceBetween(src : Location , dest : Location) : Float {
        return src.distanceTo(dest)
    }
}