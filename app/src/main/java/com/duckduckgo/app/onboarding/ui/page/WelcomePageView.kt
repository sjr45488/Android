/*
 * Copyright (c) 2020 DuckDuckGo
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

package com.duckduckgo.app.onboarding.ui.page

import android.content.Intent
import com.duckduckgo.app.onboarding.ui.customisationexperiment.DDGFeatureOnboardingOption

object WelcomePageView {
    sealed class Event {
        object OnPrimaryCtaClicked : Event()
        object OnDefaultBrowserSet : Event()
        object OnDefaultBrowserNotSet : Event()
        object OnSkipOptions : Event()
        data class OnContinueOptions(val options: Map<DDGFeatureOnboardingOption, Boolean>) : Event()
        object ShowFirstDaxOnboardingDialog : Event()
    }

    sealed class State {
        object Idle : State()
        data class ShowDefaultBrowserDialog(val intent: Intent) : State()
        object Finish : State()
        object ShowFeatureOptionsCta : State()
        object ShowControlDaxCta : State()
    }
}
