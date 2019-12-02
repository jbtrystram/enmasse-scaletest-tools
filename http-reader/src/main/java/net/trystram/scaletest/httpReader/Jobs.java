package net.trystram.scaletest.httpReader;

import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class Jobs {

    public static List<String> findJobs(final String namespace) {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            return client
                    .pods()
                    .inNamespace(namespace)
                    .withLabel("job-name", "http-inserter")
                    .list()
                    .getItems()
                    .stream()
                    .map(pod -> pod.getMetadata().getName())
                    .collect(Collectors.toList());
        }
    }

}
