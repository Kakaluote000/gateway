/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.gateway.server.config.parse.translate;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.kaazing.gateway.server.config.parse.GatewayConfigNamespace;

public class GatewayConfigTranslatorFactoryTest {

    @Test
    public void getTranslatorCurrentNSTest() {
        try {
            GatewayConfigTranslatorPipeline  translator = (GatewayConfigTranslatorPipeline) GatewayConfigTranslatorFactory.newInstance().getTranslator(
                    GatewayConfigNamespace.CURRENT_NS);
             assertTrue(translator.getTranslators().get(0) instanceof September2014Validator);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

}