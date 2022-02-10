package com.routesms.util.slack

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface SlackApi {
    @POST
    fun sendWebHook(@Url url: String, @Body slackWebHook: SlackWebHook): Call<String>
}