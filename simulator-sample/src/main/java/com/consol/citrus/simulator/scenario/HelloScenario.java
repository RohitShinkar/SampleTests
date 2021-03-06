/*
 * Copyright 2006-2016 the original author or authors.
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

package com.consol.citrus.simulator.scenario;

import com.consol.citrus.simulator.AbstractSimulatorScenario;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Christoph Deppisch
 */
@Component("Hello")
@Scope("prototype")
public class HelloScenario extends AbstractSimulatorScenario {

    @Override
    protected void configure() {
        receiveSOAPRequest()
            .payload("<Hello xmlns=\"http://citrusframework.org/schemas/hello\">" +
                        "Say Hello!" +
                     "</Hello>")
            .header("citrus_soap_action", "Hello");

        sendSOAPResponse()
            .payload("<HelloResponse xmlns=\"http://citrusframework.org/schemas/hello\">" +
                        "Hi there!" +
                     "</HelloResponse>");
    }
}
