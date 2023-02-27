package org.jnopnop.concurrency.generator.id;

class SynchronizedIdGeneratorTest extends IdGeneratorTestBase {

    @Override
    protected IdGenerator idGeneratorFactory() {
        return new SynchronizedIdGenerator("G" + instanceIdCounter.getAndIncrement(), rangeAssigner);
    }
}