package com.raytechinnovators.bijlionn.models

data class AdsModel(
    val adIdAppOpen: String = "",
    val showAppOpenAd: Boolean = false,

    val adIdBanner: String = "",
    val showBannerAd: Boolean = false,

    val adIdCollapsibleBanner: String = "",
    val showCollapsibleBannerAd: Boolean = false,

    val adIdInterstitial: String = "",
    val showInterstitialAd: Boolean = false,

    val adIdExitNative: String = "",
    val showExitNativeAd: Boolean = false,

    val adItemIdNative: String = "",
    val showItemNativeAd: Boolean = false,

    val adIdBillNative: String = "",
    val showBillNativeAd: Boolean = false,

    val adIdSuccessNative: String = "",
    val showSuccessNativeAd: Boolean = false
)
