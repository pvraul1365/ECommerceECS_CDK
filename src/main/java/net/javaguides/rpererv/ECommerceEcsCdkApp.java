package net.javaguides.rpererv;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class ECommerceEcsCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        final Environment environment = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv("CDK_DEFAULT_REGION"))
                .build();

        Map<String, String> infraestructureTags = new HashMap<>();
        infraestructureTags.put("team", "RperezvCode");
        infraestructureTags.put("cost", "ECommerceInfraestructure");

        // ------------------------------------------------------------------------------------------------------------
        // 1. Create the ECR (Respository for the Docker image of the products' microservice)
        EcrStack ecrStack = new EcrStack(app, "EComerceEcr", StackProps.builder()
                .env(environment)
                .tags(infraestructureTags)
                .build());

        // ------------------------------------------------------------------------------------------------------------
        // 2. Red. Create the VPC
        VpcStack vpcStack = new VpcStack(app, "EComerceVpc", StackProps.builder()
                .env(environment)
                .tags(infraestructureTags)
                .build());

        // ------------------------------------------------------------------------------------------------------------
        // 3. Cluster Create the ECS cluster
        ClusterStack clusterStack = new ClusterStack(app, "EComerceCluster",
                StackProps.builder()
                    .env(environment)
                    .tags(infraestructureTags)
                    .build(),
                new ClusterStackProps(vpcStack.getVpc()));
        clusterStack.addDependency(ecrStack);

        // -----------------------------------------------------------------------------------------------------------
        // 4. Infraestructura de Red Interna (NLB + ALB) Create the Network Load Balancer, Vpc Link and Application Load Balancer
        NlbStack nlbStack = new NlbStack(app, "EComerceNlb",
                StackProps.builder()
                    .env(environment)
                    .tags(infraestructureTags)
                    .build(),
                new NlbStackProps(vpcStack.getVpc()));
        nlbStack.addDependency(clusterStack);

        // -----------------------------------------------------------------------------------------------------------
        // 5. Create the Product Service (Fargate Service)
        Map<String, String> productsServiceTags = new HashMap<>();
        infraestructureTags.put("team", "RperezvCode");
        infraestructureTags.put("cost", "ProductsService");
        ProductServiceStack productsServiceStack = new ProductServiceStack(app, "EComerceProductService",
                StackProps.builder()
                        .env(environment)
                        .tags(productsServiceTags)
                        .build(),
                new ProductServiceProps(
                        vpcStack.getVpc(),
                        clusterStack.getCluster(),
                        nlbStack, // Pasamos el stack completo para acceder al listener
                        ecrStack.getProductsServiceRepository()
                ));
        productsServiceStack.addDependency(vpcStack);
        productsServiceStack.addDependency(clusterStack);
        productsServiceStack.addDependency(nlbStack);
        productsServiceStack.addDependency(ecrStack);

        // -----------------------------------------------------------------------------------------------------------
        // 6. Create the API Gateway (Prueba de infraestructura)
        // Pasamos el nlbStack como argumento para extraer la integración
        ApiStack apiStack = new ApiStack(app, "EComerceApi",
                StackProps.builder()
                        .env(environment)
                        .tags(infraestructureTags)
                        .build(),
                nlbStack);
        // La API no puede existir sin el VpcLink y el NLB definidos en NlbStack
        apiStack.addDependency(nlbStack);

        app.synth();
    }
}

