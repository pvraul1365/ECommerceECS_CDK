package net.javaguides.rpererv;

import java.util.Collections;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigatewayv2.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.HttpApiProps;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.constructs.Construct;

/**
 * ApiStack
 * <p>
 * Created by IntelliJ, Spring Framework Guru.
 *
 * @author architecture - pvraul
 * @version 15/02/2026 - 10:24
 * @since 1.17
 */
public class ApiStack extends Stack {

    public ApiStack(final Construct scope, final String id, final StackProps props, NlbStack nlbStack) {
        super(scope, id, props);

        // 1. Crear la HTTP API (v2)
        HttpApi httpApi = new HttpApi(this, "ECommerceHttpApi", HttpApiProps.builder()
                .apiName("ECommerceApi")
                .build());

        // 2. Añadir una ruta de prueba que use la integración de tu NlbStack
        // Esta ruta enviará el tráfico a través del VpcLink -> NLB -> ALB
        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/test")
                .methods(Collections.singletonList(HttpMethod.GET))
                .integration(nlbStack.getNlbIntegration())
                .build());

        // ESTO MOSTRARÁ LA URL EN TU CONSOLA AL FINALIZAR
        CfnOutput.Builder.create(this, "ApiEndpoint")
                .value(httpApi.getApiEndpoint() + "/test")
                .description("URL para probar el encadenamiento NLB -> ALB")
                .build();

        // RUTA NUEVA: Para el microservicio de productos
        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/api/products") // Coincide con tu Controller de Spring
                .methods(Collections.singletonList(HttpMethod.GET))
                .integration(nlbStack.getNlbIntegration())
                .build());

        CfnOutput.Builder.create(this, "ProductsEndpoint")
                .value(httpApi.getApiEndpoint() + "/api/products")
                .description("URL para ver tus productos de Spring Boot")
                .build();
    }
}
