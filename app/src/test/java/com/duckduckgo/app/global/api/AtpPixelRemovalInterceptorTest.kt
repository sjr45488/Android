/*
 * Copyright (c) 2021 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.global.api

import com.duckduckgo.mobile.android.vpn.pixels.DeviceShieldPixelNames
import com.duckduckgo.networkprotection.impl.pixels.NetworkProtectionPixelNames
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class AtpPixelRemovalInterceptorTest {
    private lateinit var atpPixelRemovalInterceptor: AtpPixelRemovalInterceptor

    @Before
    fun setup() {
        atpPixelRemovalInterceptor = AtpPixelRemovalInterceptor()
    }

    @Test
    fun whenSendPixelTheRedactAtbInfoFromPixels() {
        DeviceShieldPixelNames.values().map { it.pixelName }.forEach { pixelName ->
            val pixelUrl = String.format(PIXEL_TEMPLATE, pixelName)

            val interceptedUrl = atpPixelRemovalInterceptor.intercept(FakeChain(pixelUrl)).request.url
            assertEquals(!PIXELS_WITH_ATB_INFO.contains(pixelName), interceptedUrl.queryParameter("atb") == null)
            Assert.assertFalse(interceptedUrl.queryParameter("appVersion") == null)
        }
    }

    @Test
    fun whenSendPixelThenRedactAtbInfoFromNetPPixels() {
        val url = "https://improving.duckduckgo.com/t/%s_android_phone?atb=v255-7zu&appVersion=5.74.0&test=1"
        NetworkProtectionPixelNames.values().map { it.pixelName }.forEach { pixelName ->
            val pixelUrl = String.format(url, pixelName)

            val interceptedUrl = atpPixelRemovalInterceptor.intercept(FakeChain(pixelUrl)).request.url
            assertEquals(!PIXELS_WITH_ATB_INFO.contains(pixelName), interceptedUrl.queryParameter("atb") == null)
            assertNotNull(interceptedUrl.queryParameter("appVersion"))
        }
    }

    @Test
    fun whenSendNonNetpAndAppTPPixelThenDoNothingToPixel() {
        val url = "https://improving.duckduckgo.com/t/m_voice_search_available_android_phone?atb=v255-7zu&appVersion=5.74.0&test=1"

        val interceptedUrl = atpPixelRemovalInterceptor.intercept(FakeChain(url)).request.url
        assertNotNull(interceptedUrl.queryParameter("atb"))
        assertNotNull(interceptedUrl.queryParameter("appVersion"))
    }

    companion object {
        private const val PIXEL_TEMPLATE = "https://improving.duckduckgo.com/t/%s_android_phone?atb=v255-7zu&appVersion=5.74.0&test=1"

        private val PIXELS_WITH_ATB_INFO = listOf(
            "m_atp_ev_enabled_tracker_activity_d",
            "m_atp_ev_enabled_tracker_activity_c",
            "m_atp_ev_enabled_tracker_activity_u",
            "m_atp_imp_exclusion_list_activity_d",
            "m_atp_imp_exclusion_list_activity_c",
            "m_atp_imp_exclusion_list_activity_u",
            "m_atp_imp_company_trackers_activity_d",
            "m_atp_imp_company_trackers_activity_c",
            "m_atp_imp_company_trackers_activity_u",
            "m_atp_imp_tracker_activity_detail_d",
            "m_atp_imp_tracker_activity_detail_c",
            "m_atp_imp_tracker_activity_detail_u",
            "m_atp_ev_selected_cancel_app_protection_c",
            "m_atp_ev_selected_disable_app_protection_c",
            "m_atp_ev_selected_disable_protection_c",
            "m_atp_ev_disabled_tracker_activity_c",
            "m_atp_ev_enabled_on_search_d",
            "m_atp_ev_disabled_on_search_d",
            "m_atp_ev_enabled_on_launch_d",
            "m_atp_ev_enabled_on_launch_c",
            "m_atp_ev_disabled_on_launch_d",
            "m_atp_ev_disabled_on_launch_c",
            "m_atp_ev_enabled_on_search_c",
            "m_atp_ev_disabled_on_search_c",
            "m_atp_ev_enabled_onboarding_u",
            "m_atp_ev_enabled_onboarding_c",
            "m_atp_ev_enabled_onboarding_d",
            "m_atp_ev_apptp_enabled_cta_button_press",
            "m_atp_imp_disable_protection_dialog_c",
            "m_atp_ev_disabled_tracker_activity_d",
            "m_atp_imp_manage_recent_app_settings_activity_d",
            "m_atp_imp_manage_recent_app_settings_activity_c",
            "m_atp_imp_manage_recent_app_settings_activity_u",
            "m_atp_imp_disable_app_protection_all_c",
            "m_atp_ev_submit_disable_app_protection_dialog_c",
            "m_atp_ev_submit_disable_app_protection_dialog_d",
            "m_atp_ev_skip_disable_app_protection_dialog_d",
            "m_atp_ev_skip_disable_app_protection_dialog_c",
            "m_atp_imp_disable_app_protection_detail_c",
        )
    }
}
