package org.jnopnop.concurrency.generator.id;


import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.jnopnop.concurrency.generator.id.support.LoadBalancer;
import org.jnopnop.concurrency.generator.id.support.RoundRobbinLoadBalancer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;

abstract class IdGeneratorTestBase {
    protected AtomicLong instanceIdCounter;
    protected RangeAssigner rangeAssigner;
    protected abstract IdGenerator idGeneratorFactory();

    public static Stream<Arguments> concurrencyCases() {
        return Stream.of(
                Arguments.of(TestSettings.builder()
                        .clientCount(1)
                        .idsPerClient(128)
                        .instanceCount(1)
                        .rangeSize(10).build()),
                Arguments.of(TestSettings.builder()
                        .clientCount(2)
                        .idsPerClient(128)
                        .instanceCount(1)
                        .rangeSize(10).build()),
                Arguments.of(TestSettings.builder()
                        .clientCount(100)
                        .idsPerClient(10000)
                        .instanceCount(10)
                        .rangeSize(100).build())
        );
    }

    @ParameterizedTest
    @MethodSource("concurrencyCases")
    protected void verifyIdGenerator(TestSettings settings) throws Exception {
        this.instanceIdCounter = new AtomicLong();
        this.rangeAssigner = new MonotonicRangeAssigner(settings.rangeSize());

        LoadBalancer lb = new RoundRobbinLoadBalancer(this::idGeneratorFactory, settings.instanceCount);
        ExecutorService clientPool = newFixedThreadPool(settings.clientCount);

        List<Future<List<Id>>> futures = range(0, settings.clientCount)
                .mapToObj(i -> clientPool.submit(new ClientTask(lb, settings.idsPerClient, "C" + i)))
                .toList();

        clientPool.shutdown();
        clientPool.awaitTermination(10, TimeUnit.SECONDS);

        final List<Id> generatedIds = futures.stream()
                .map(this::safeGet)
                .flatMap(Collection::stream)
                .sorted(comparing(id -> id.value))
                .toList();

        // Check #1: Completeness
        // Verify each client received an id
        assertThat(generatedIds)
                .hasSize(settings.clientCount * settings.idsPerClient);

        // Check #2: Correctness
        // Verify all ids are unique
        Set<Long> uniqueIds = new HashSet<>();
        List<Id> duplicates = generatedIds.stream()
                .filter(x -> !uniqueIds.add(x.value))
                .collect(Collectors.toList());
        assertThat(duplicates)
                .describedAs("Should not generate duplicate ids")
                .isEmpty();

        // Check #3: Reliability
        // Verify ranges are depleted evenly without holes
        // At any point in time we can have at most:
        // (NUM_WORKERS - 1) * (RANGE_SIZE - 1)
        // not filled ids
        // Explanation: NUM_WORKERS:3, RANGE_SIZE:10, each instance generated 1 id:
        // [ W1:0, W2:10, W3:20 ] -> 18 ids missing [1-9], [11-19]
        // TODO: Rephrase proof
        List<String> unfinishedRanges = range(0, generatedIds.size() - 1)
                .mapToObj(i -> Pair.of(generatedIds.get(i), generatedIds.get(i + 1)))
                .filter(pair -> pair.getLeft().value + 1 < pair.getRight().value)
                .map(Pair::getLeft)
                .map(id -> {
                    long rangeStart = ((id.value + 1) / settings.rangeSize) * settings.rangeSize;
                    long rangeEnd = rangeStart + settings.rangeSize;
                    if (id.value >= rangeStart) {
                        // Unfinished range
                        return format("Unfinished range [%s, %s], Last id:%s, Instance:%s",
                                rangeStart, rangeEnd, id.value, id.instance);
                    } else {
                        // Lost range
                        return format("Unfinished range [%s, %s]", rangeStart, rangeEnd);
                    }
                }).toList();
        assertThat(unfinishedRanges)
                .describedAs("Unevenly depleted id ranges")
                .hasSizeLessThanOrEqualTo(settings.instanceCount);
    }

    @SneakyThrows
    protected <T> T safeGet(Future<T> future) {
        return future.get();
    }

    protected record Id(long value, String client, String instance) {
    }

    @Builder
    protected record TestSettings(int clientCount,
                                  int idsPerClient,
                                  int instanceCount,
                                  long rangeSize) {
    }

    @RequiredArgsConstructor
    private static class ClientTask implements Callable<List<Id>> {
        private final LoadBalancer lb;
        private final long totalCalls;
        private final String id;

        @Override
        public List<Id> call() {
            return LongStream.range(0, totalCalls)
                    .mapToObj(i -> {
                        IdGenerator idGenerator = lb.nextInstance();
                        long nextNumber = idGenerator.generateId();
                        return new Id(nextNumber, id, idGenerator.getName());
                    })
                    .collect(toList());
        }
    }
}