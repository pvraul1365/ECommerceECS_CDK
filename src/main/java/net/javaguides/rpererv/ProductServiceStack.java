package net.javaguides.rpererv;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddApplicationTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.LogGroupProps;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

/**
 * ProductServiceStack
 * <p>
 * Created by IntelliJ, Spring Framework Guru.
 *
 * @author architecture - pvraul
 * @version 15/02/2026 - 15:24
 * @since 1.17
 */
public class ProductServiceStack extends Stack {

    public ProductServiceStack(final Construct scope, final String id, final StackProps props,
                               ProductServiceProps productServiceProps) {
        super(scope, id, props);

        // 1. Fargate Task Definition
        FargateTaskDefinition fargateTaskDefinition = new FargateTaskDefinition(this, "TaskDefinition",
                FargateTaskDefinitionProps.builder()
                        .family("products-service") // name of the task definition, same as the family name in the ECS service
                        .cpu(512) // 0.5 vCPU
                        .memoryLimitMiB(1024) // 1 GB RAM
                        .build());

        // 2. Log Driver for Container Definition
        AwsLogDriver logDriver = new AwsLogDriver(AwsLogDriverProps.builder()
                .logGroup(new LogGroup(this, "logGroup",
                        LogGroupProps.builder()
                                .logGroupName("ProductsService") // name of the log group, which will be used in CloudWatch Logs
                                .removalPolicy(RemovalPolicy.DESTROY) // to delete the log group when the stack is deleted, for cleanup purposes
                                .retention(RetentionDays.ONE_MONTH)
                                .build()
                        ))
                .streamPrefix("ProductsService") // prefix for the log stream, which will be used in CloudWatch Logs
                .build());

        // 3. Adding a container to the Fargate Task Definition (using the ECR repository created in the RepositoryStack)
        Map<String, String> envVariables = new HashMap<>();
        envVariables.put("SERVER_PORT", "8080"); // environment variable for the application inside the container, which will be used to configure the port where the application listens

        fargateTaskDefinition.addContainer("ProductsServiceContainer", ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromEcrRepository(productServiceProps.repository(), "1.0.0")) // to use the ECR repository created in the RepositoryStack
                        .containerName("productsService")
                .logging(logDriver) // to use the log driver created above for logging
                        .portMappings(Collections.singletonList(PortMapping.builder()
                                        .containerPort(8080) // port where the application inside the container listens, which will be used in the target group of the ALB
                                        .protocol(Protocol.TCP)
                                .build()))
                        .environment(envVariables)
                .build());

        // 4. Create the Fargate Service and associate it with the ALB created in the NlbStack, using private subnets for security
        FargateService fargateService = FargateService.Builder.create(this, "ProductsService")
                .serviceName("ProductsService") // name of the ECS service, same as the service
                .cluster(productServiceProps.cluster())
                .taskDefinition(fargateTaskDefinition)
                .desiredCount(2)
                .assignPublicIp(false) // to not assign public IPs to the Fargate tasks, which is recommended for security reasons, since the ALB will be in charge of routing the traffic to the Fargate service and we don't want to expose the tasks directly to the internet
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_WITH_EGRESS) // <--- Seguridad con salida a internet
                        .build())
                .build();
        productServiceProps.repository().grantPull(fargateTaskDefinition.getExecutionRole());
        fargateService.getConnections().allowFrom(productServiceProps.nlbStack().getApplicationLoadBalancer(), Port.tcp(8080)); // to allow incoming traffic from the ALB to the Fargate service on the port where the application listens (8080)

        // 4. Associate the Fargate Service with the ALB created in the NlbStack, using an Application Target Group with health checks and a short deregistration delay for faster recovery in case of failures
        ApplicationTargetGroup targetGroup = productServiceProps.nlbStack().getApplicationListener()
                .addTargets("ProdcutsServiceAlbTarget",
                        AddApplicationTargetsProps.builder()
                                .targetGroupName("productsServiceAlb")
                                .port(8080)
                                .protocol(ApplicationProtocol.HTTP)
                                .targets(Collections.singletonList(fargateService))
                                .deregistrationDelay(Duration.seconds(30)) // to reduce the time it takes for the ALB to stop routing traffic to unhealthy instances, which is important for faster recovery in case of failures
                                .healthCheck(HealthCheck.builder()
                                        .enabled(true)
                                        .interval(Duration.seconds(30)) // to set the interval between health checks, which is important for faster detection of unhealthy instances
                                        .timeout(Duration.seconds(10)) // to set the timeout for health checks, which is important for faster detection of unhealthy instances
                                        .path("/actuator/health") // to set the path for health checks, which should be an endpoint in your application that returns a 200 status code when the application is healthy, and a 500 status code when the application is unhealthy
                                        //.healthyHttpCodes("200") // to set the HTTP status codes that indicate a healthy instance, which should match the response from your health check endpoint
                                        .port("8080") // to set the port for health checks, which should match the container port where your application listens
                                        .build())
                                .build());
    }

}

record ProductServiceProps(
        Vpc vpc,
        Cluster cluster,
        NlbStack nlbStack,
        Repository repository
) {
}
