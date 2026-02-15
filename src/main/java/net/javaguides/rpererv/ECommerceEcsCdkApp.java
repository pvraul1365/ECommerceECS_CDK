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
                .account("")
                .region("us-east-1")
                .build();

        Map<String, String> infraestructureTags = new HashMap<>();
        infraestructureTags.put("team", "RperezvCode");
        infraestructureTags.put("cost", "ECommerceInfraestructure");

        // ------------------------------------------------------------------------------------------------------------
        // Create the ECR
        EcrStack ecrStack = new EcrStack(app, "EComerceEcr", StackProps.builder()
                .env(environment)
                .tags(infraestructureTags)
                .build());

        // ------------------------------------------------------------------------------------------------------------
        // Create the VPC
        VpcStack vpcStack = new VpcStack(app, "EComerceVpc", StackProps.builder()
                .env(environment)
                .tags(infraestructureTags)
                .build());

        // ------------------------------------------------------------------------------------------------------------
        // Create the ECS cluster
        ClusterStack clusterStack = new ClusterStack(app, "EComerceCluster",
                StackProps.builder()
                    .env(environment)
                    .tags(infraestructureTags)
                    .build(),
                new ClusterStackProps(vpcStack.getVpc()));
        clusterStack.addDependency(ecrStack);

        // -----------------------------------------------------------------------------------------------------------
        // Create the Network Load Balancer, Vpc Link and Application Load Balancer
        NlbStack nlbStack = new NlbStack(app, "EComerceNlb",
                StackProps.builder()
                    .env(environment)
                    .tags(infraestructureTags)
                    .build(),
                new NlbStackProps(vpcStack.getVpc()));
        nlbStack.addDependency(clusterStack);

        // -----------------------------------------------------------------------------------------------------------
        // 5. Create the API Gateway (Prueba de infraestructura)
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

