/*
 * Copyright 2013 Dirk Vranckaert
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

package eu.vranckaert.worktime.utils.view.validation;

import android.view.View;
import com.mobsandgeeks.saripaar.Rule;

/**
 * User: DIRK VRANCKAERT
 * Date: 24/05/13
 * Time: 13:30
 */
public class ObjectRequiredRule extends Rule<View> {
    private Object object;

    public ObjectRequiredRule(String failureMessage, Object object) {
        super(failureMessage);
        this.object = object;
    }

    @Override
    public boolean isValid(View view) {
        return object == null ? false : true;
    }
}
