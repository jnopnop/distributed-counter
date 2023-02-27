package org.jnopnop.concurrency.generator.id;

class NonBlockingIdGeneratorTest extends IdGeneratorTestBase {

    @Override
    protected IdGenerator idGeneratorFactory() {
        return new NonBlockingIdGenerator("G" + instanceIdCounter.getAndIncrement(), rangeAssigner);
    }

}