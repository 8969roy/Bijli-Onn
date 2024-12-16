package com.raytechinnovators.bijlionn.models

data class UserStatus(
    var chargingDevices: Int = 0,
    var minDevices: Int = 0,
    var maxDevices: Int = 0
)
