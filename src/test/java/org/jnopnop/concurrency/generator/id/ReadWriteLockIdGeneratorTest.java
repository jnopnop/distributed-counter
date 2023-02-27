package org.jnopnop.concurrency.generator.id;

class ReadWriteLockIdGeneratorTest extends IdGeneratorTestBase {

    @Override
    protected IdGenerator idGeneratorFactory() {
        return new ReadWriteLockIdGenerator("G" + instanceIdCounter.getAndIncrement(), rangeAssigner);
    }

}