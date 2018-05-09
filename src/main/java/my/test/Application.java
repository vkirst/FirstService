package my.test;

import java.io.IOException;
import java.net.SocketException;
import java.util.Scanner;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;

public class Application {

    public static void main(String[] args) throws Exception {

        CamelContext ctx = new DefaultCamelContext();
        ConnectionFactory connectionFactory =
                new ActiveMQConnectionFactory("vm://localhost");
        ctx.addComponent("jms",
                JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

        ctx.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                onException(IOException.class, SocketException.class)
                        .maximumRedeliveries(-1).redeliveryDelay(2000);

                restConfiguration().producerComponent("http4")
                    .host("localhost").port(8080);

                from("direct:in")
                    .to("jms:queue:incomingOrders");

                from("jms:queue:incomingOrders?acknowledgementModeName=CLIENT_ACKNOWLEDGE")
                    .setHeader("name", simple("${in.body}"))
                    .to("rest:get:greeting?name={name}")
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

    public void print(String st) {
        System.err.println(st);
    }

}
