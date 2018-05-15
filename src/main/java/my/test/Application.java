package my.test;

import java.io.IOException;
import java.util.Scanner;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.ExchangeTimedOutException;

public class Application {

    public static void main(String[] args) throws Exception {

        CamelContext ctx = new DefaultCamelContext();
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
        ctx.addComponent("jms",
                JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

        ctx.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                onException(IOException.class, ExchangeTimedOutException.class)
                        .maximumRedeliveries(-1).redeliveryDelay(2000).maximumRedeliveryDelay(2000);

                from("direct:in")
                        .to("jms:queue:incomingOrders");

                from("jms:queue:incomingOrders?acknowledgementModeName=CLIENT_ACKNOWLEDGE")
                        .inOut("rabbitmq:localhost:5672/incomingOrders?username=guest&password=guest&automaticRecoveryEnabled=true")
                        .log("${body}");

            }
        });

        ctx.start();

        ProducerTemplate template = ctx.createProducerTemplate();

        try (Scanner scan = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String line = scan.nextLine();
                if (line.equals("quit")) {
                    break;
                }
                template.sendBody("direct:in", line);

            }
        }

        ctx.stop();
    }

    public static com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory() {
        com.rabbitmq.client.ConnectionFactory cf = new com.rabbitmq.client.ConnectionFactory();
        cf.setHost("localhost");
        cf.setPort(5672);
        cf.setUsername("guest");
        cf.setPassword("guest");
        return cf;
    }

}
