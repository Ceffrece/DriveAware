package com.google.mediapipe.examples.facelandmarker

data class UserData(
    val id: String? = null,
    val username: String? = null,
    val email: String? = null,
    val password: String? = null,
    val driveSessions: Map<String, DriveData>? = null,
    val sharedReports: Map<String, Map<String, DriveData>>? = null
)

data class DriveData(
    var sessionId: String? = null,
    val date: String? = null,
    val distractedDrivingPercentage: Double? = null,
    val endTime: String? = null,
    val startTime: String? = null,
    val totalDistractedDistance: Double? = null,
    val totalDistractedTime: Int? = null
)