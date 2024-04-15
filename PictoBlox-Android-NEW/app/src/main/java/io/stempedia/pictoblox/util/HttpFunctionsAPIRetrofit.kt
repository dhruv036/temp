package io.stempedia.pictoblox.util

import io.reactivex.rxjava3.core.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*


val pictoBloxFirebaseFunctionAPI by lazy {
    Retrofit.Builder()
        .client(OkHttpClient.Builder().let {
            val logging = HttpLoggingInterceptor()
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
            it.addInterceptor(logging)
        }
            .build())
        .baseUrl("https://asia-east2-pictobloxdev.cloudfunctions.net/")
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
        .build()
        .create(PictoBloxFunctions::class.java)
}

interface PictoBloxFunctions {
    @POST("pictobloxRedeemCoupon/check-coupon")
    fun checkCoupon(
        @Query("userId") userId: String,
        @Query("coupon") coupon: String
    ): Single<CheckCouponPOJO>

    @FormUrlEncoded
    @POST("pictobloxRedeemCouponV1/check-coupon")
    fun checkCouponV1(
        @Field("coupon") coupon: String,
        @Header("Authorization") authHeader: String
    ): Call<CheckCouponPOJO>

}

class CheckCouponPOJO(var credits: Long?, var text: String?, var error: String?)
class CheckCouponErrorPOJO(var text: String?, var error: String?)
