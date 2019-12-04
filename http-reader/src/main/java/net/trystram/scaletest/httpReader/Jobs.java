package net.trystram.scaletest.httpReader;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
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

    public static OptionalLong createdPerPod(final String namespace) {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            return Optional.ofNullable(client
                    .batch()
                    .jobs()
                    .inNamespace(namespace)
                    .withName("http-inserter")
                    .get())
                    .flatMap(job -> {
                        return job
                                .getSpec()
                                .getTemplate()
                                .getSpec()
                                .getContainers()
                                .stream()
                                .filter(container -> container.getName().equals("main"))
                                .flatMap(container -> container.getEnv().stream())
                                .filter(env -> env.getName().equals("MAX_DEVICES"))
                                .map(env -> env.getValue())
                                .map(Long::parseLong)
                                .findAny();
                    })

                    .stream().mapToLong(l -> l).findAny();
        }
    }

}
