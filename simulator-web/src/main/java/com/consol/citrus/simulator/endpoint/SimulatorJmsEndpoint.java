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

package com.consol.citrus.simulator.endpoint;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.context.TestContextFactory;
import com.consol.citrus.endpoint.EndpointAdapter;
import com.consol.citrus.exceptions.ActionTimeoutException;
import com.consol.citrus.jms.endpoint.JmsEndpoint;
import com.consol.citrus.jms.endpoint.JmsSyncEndpoint;
import com.consol.citrus.message.Message;
import com.consol.citrus.simulator.exception.SimulatorException;
import com.consol.citrus.simulator.util.SoapMessageHelper;
import com.consol.citrus.ws.message.SoapMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * @author Christoph Deppisch
 */
public class SimulatorJmsEndpoint  implements InitializingBean, Runnable, DisposableBean {

    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(SoapMessageHelper.class);

    @Autowired
    private SoapMessageHelper soapMessageHelper;

    @Autowired
    private TestContextFactory testContextFactory;

    /** Jms destination that is constantly listened on */
    private JmsEndpoint jmsEndpoint;

    /** Thread running the server */
    private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

    /** Message handler for incoming simulator request messages */
    private EndpointAdapter endpointAdapter;

    /** Running flag */
    private boolean running = false;

    /** Should automatically start on system load */
    private boolean autoStart = true;

    @Override
    public void run() {
        LOG.info(String.format("Simulator endpoint waiting for requests on JMS destination '%s'", jmsEndpoint.getEndpointConfiguration().getDestinationName()));

        while (running) {
            try {
                TestContext context = testContextFactory.getObject();
                Message message = jmsEndpoint.createConsumer().receive(context, jmsEndpoint.getEndpointConfiguration().getTimeout());
                if (message != null) {
                    Message request = new SoapMessage(soapMessageHelper.getSoapBody(message), message.getHeaders());
                    Message response = endpointAdapter.handleMessage(request);

                    if (response != null && jmsEndpoint instanceof JmsSyncEndpoint) {
                        jmsEndpoint.createProducer().send(soapMessageHelper.createSoapMessage(response), context);
                    }
                }
            } catch (ActionTimeoutException e) {
                // ignore timeout and continue listening for request messages.
                continue;
            } catch (SimulatorException e) {
                LOG.error("Failed to process message", e);
            } catch (Exception e) {
                LOG.error("Unexpected error while processing", e);
            }
        }
    }

    /**
     * Start up runnable in separate thread.
     */
    public void start() {
        running = true;
        taskExecutor.execute(this);
    }

    /**
     * Stop runnable execution.
     */
    public void stop() {
        running = false;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (autoStart) {
            start();
        }
    }

    @Override
    public void destroy() throws Exception {
        stop();
    }

    /**
     * Sets the jms endpoint to read messages from.
     * @param jmsEndpoint
     */
    public void setJmsEndpoint(JmsEndpoint jmsEndpoint) {
        this.jmsEndpoint = jmsEndpoint;
    }

    /**
     * Sets the endpoint adapter to delegate messages to.
     * @param endpointAdapter
     */
    public void setEndpointAdapter(EndpointAdapter endpointAdapter) {
        this.endpointAdapter = endpointAdapter;
    }

    /**
     * Enable/Disable auto start.
     * @param autoStart
     */
    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }
}
