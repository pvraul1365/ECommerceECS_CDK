package net.javaguides.rpererv;

import java.util.Arrays;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcProps;
import software.constructs.Construct;

/**
 * VpcStack
 * <p>
 * Created by IntelliJ, Spring Framework Guru.
 *
 * @author architecture - pvraul
 * @version 14/02/2026 - 11:54
 * @since 1.17
 */
public class VpcStack extends Stack {

    private final Vpc vpc;

    public VpcStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.vpc = new Vpc(this, "Vpc", VpcProps.builder()
                .vpcName("ECommerceVPC") // name of the VPC, same as the VPC name in the ECS cluster
                .maxAzs(2) // to create subnets in 2 availability zones for high availability
                // DO NOT DO THIS IN PRODUCTION, since it will create public subnets and expose the ECS cluster to the internet, but for this example we will use public subnets to avoid creating NAT gateways and keep the cost low
                // .natGateways(0)
                .natGateways(2) // to create NAT gateways in each availability zone for private subnets, which is the recommended configuration for production environments to keep the ECS cluster secure and not exposed to the internet
                .subnetConfiguration(Arrays.asList(
                        SubnetConfiguration.builder()
                                .name("Public")
                                .subnetType(SubnetType.PUBLIC)
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("Private")
                                .subnetType(SubnetType.PRIVATE_WITH_EGRESS) // Para los contenedores
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("Isolated")
                                .subnetType(SubnetType.PRIVATE_ISOLATED) // Para los balanceadores/VpcLink
                                .cidrMask(24)
                                .build()
                ))
                .build());
    }

    public Vpc getVpc() {
        return vpc;
    }
}
