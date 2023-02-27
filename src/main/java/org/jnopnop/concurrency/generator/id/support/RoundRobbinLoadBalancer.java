package org.jnopnop.concurrency.generator.id.support;

import org.jnopnop.concurrency.generator.id.IdGenerator;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

public class RoundRobbinLoadBalancer implements LoadBalancer {
    private final AtomicInteger next;
    private final List<IdGenerator> pool;

    public RoundRobbinLoadBalancer(Supplier<IdGenerator> workerFactory, int poolSize) {
        this.next = new AtomicInteger();
        this.pool = range(0, poolSize)
                .mapToObj(i -> workerFactory.get())
                .collect(toList());
    }

    @Override
    public IdGenerator nextInstance() {
        int next = this.next.getAndUpdate(index -> (index + 1) % pool.size());
        return pool.get(next);
    }
}
