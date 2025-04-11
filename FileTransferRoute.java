package com.taller.demo;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class FileTransferRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("file:input?include=.*\\.csv&noop=true")
            .log("📂 Archivo detectado: ${file:name}")
            .process(exchange -> {
                String tipoCliente = "otros"; // Valor por defecto

                try {
                    String body = exchange.getIn().getBody(String.class);
                    String[] lines = body.split("\n");

                    if (lines.length >= 2) {
                        String header = lines[0].toLowerCase().trim();
                        String firstDataLine = lines[1].toLowerCase().trim();

                        if (header.contains("tipo_cliente")) {
                            String[] headerColumns = header.split(",");
                            int tipoClienteIndex = -1;

                            for (int i = 0; i < headerColumns.length; i++) {
                                if (headerColumns[i].trim().equals("tipo_cliente")) {
                                    tipoClienteIndex = i;
                                    break;
                                }
                            }

                            if (tipoClienteIndex != -1) {
                                String[] dataValues = firstDataLine.split(",");
                                if (dataValues.length > tipoClienteIndex) {
                                    tipoCliente = dataValues[tipoClienteIndex].trim();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Si hay error, tipoCliente seguirá siendo "otros"
                }

                exchange.setProperty("tipoCliente", tipoCliente.toLowerCase());
            })
            .choice()
                .when(simple("${exchangeProperty.tipoCliente} == 'vip'"))
                    .log("🏷️ Clasificado como cliente VIP")
                    .to("file:output/VIP")
                .when(simple("${exchangeProperty.tipoCliente} == 'regular'"))
                    .log("🏷️ Clasificado como cliente REGULAR")
                    .to("file:output/regular")
                .otherwise()
                    .log("🏷️ Cliente sin tipo claro → enviado a 'otros'")
                    .to("file:output/otros")
            .end()
            .log("✅ Transferencia completa: ${file:name}");
    }
}
