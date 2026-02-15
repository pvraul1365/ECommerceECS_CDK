package net.javaguides.rpererv;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.RepositoryProps;
import software.amazon.awscdk.services.ecr.TagMutability;
import software.constructs.Construct;

/**
 * EcrStack
 * <p>
 * Created by IntelliJ, Spring Framework Guru.
 *
 * @author architecture - pvraul
 * @version 14/02/2026 - 09:25
 * @since 1.17
 */
public class EcrStack extends Stack {

    private final Repository productsServiceRepository;

    public EcrStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.productsServiceRepository = new Repository(this, "ProductsService", RepositoryProps.builder()
                .repositoryName("productsservice") // name of the repository, same as the container name in the task definition
                .removalPolicy(RemovalPolicy.DESTROY) // to automatically delete the repository when the stack is deleted
                .imageTagMutability(TagMutability.IMMUTABLE) // to prevent overwriting existing images with the same tag
                .emptyOnDelete(true) // to automatically delete images when the repository is deleted
                .build());
    }

    public Repository getProductsServiceRepository() {
        return productsServiceRepository;
    }
}
