package org.camelcookbook.examples.testing.notifybuilder;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelSpringJUnit4ClassRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class that demonstrates the use of the NotifyBuilder DSL to verify the interactions
 * on a route with fixed endpoints without modifying it in any way.
 */
@RunWith(CamelSpringJUnit4ClassRunner.class)
@ContextConfiguration({"/META-INF/spring/fixedEndpoints-context.xml",
        "/spring/notifybuilder/test-jms-context.xml"})
public class FixedEndpointsNotifyBuilderSpringTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Test
    public void testSingleMessageDone() throws InterruptedException {
        final String messageText = "testMessage";

        NotifyBuilder notify = new NotifyBuilder(camelContext)
                .from("activemq:in")
                .whenDone(1)
                .create();

        sendMessageBody(messageText);
        assertTrue(notify.matches(10, TimeUnit.SECONDS));
    }

    @Test
    public void testSingleMessageDoneByRouteId() throws InterruptedException {
        final String messageText = "testMessage";

        NotifyBuilder notify = new NotifyBuilder(camelContext)
                .fromRoute("modifyPayloadBetweenQueues")
                .whenDone(1)
                .create();

        sendMessageBody(messageText);
        assertTrue(notify.matches(10, TimeUnit.SECONDS));
    }

    @Test
    public void testSingleMessageDoneSentToOut() throws InterruptedException {
        final String messageText = "testMessage";

        NotifyBuilder notify = new NotifyBuilder(camelContext)
                .from("activemq:in")
                .whenDone(1)
                .whenBodiesDone("Modified: testMessage")
                .wereSentTo("activemq:out")
                .create();

        sendMessageBody(messageText);
        assertTrue(notify.matches(10, TimeUnit.SECONDS));
    }

    @Test
    public void testSingleMessageDoneSentToOutAndMatched() throws InterruptedException {
        final String messageText = "testMessage";

        MockEndpoint mock = camelContext.getEndpoint("mock:nameDoesNotMatter", MockEndpoint.class);
        mock.message(0).inMessage().contains(messageText);
        mock.message(0).header("count").isNull();

        NotifyBuilder notify = new NotifyBuilder(camelContext)
                .from("activemq:in")
                .whenDone(1).wereSentTo("activemq:out")
                .whenDoneSatisfied(mock)
                .create();

        sendMessageBody(messageText);
        assertTrue(notify.matches(10, TimeUnit.SECONDS));
    }

    @Test
    public void testMessageFiltering() throws InterruptedException {
        NotifyBuilder notify = new NotifyBuilder(camelContext)
                .from("activemq:in")
                .whenExactlyDone(1).filter().simple("${body} contains 'test'")
                .create();

        sendMessageBody("testMessage");
        sendMessageBody("realMessage");

        assertTrue(notify.matches(10, TimeUnit.SECONDS));
    }

    private void sendMessageBody(final String messageText) {
        jmsTemplate.send("in", new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                TextMessage textMessage = session.createTextMessage(messageText);
                return textMessage;
            }
        });
    }

}
