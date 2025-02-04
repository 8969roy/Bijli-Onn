package com.royllow.bijlionn.Models

class User {
    var uid: String? = null
    var name: String? = null
    var phoneNumber: String? = null
    var profileImage: String? = null
    var location: String? = null

    constructor()
    constructor(
        uid: String?,
        name: String?,
        phoneNumber: String?,
        profileImage: String?,
        location: String?
    ) {
        this.uid = uid
        this.name = name
        this.phoneNumber = phoneNumber
        this.profileImage = profileImage
        this.location = location
    }
}