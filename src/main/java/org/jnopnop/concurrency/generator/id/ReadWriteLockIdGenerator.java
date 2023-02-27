package org.jnopnop.concurrency.generator.id;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jnopnop.concurrency.generator.id.domain.Range;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class ReadWriteLockIdGenerator implements IdGenerator {
    @Getter
    private final String name;
    private final RangeAssigner rangeAssigner;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();
    // In given implementation primitive long type would also work fine
    // however Atomic has more streamlined interface when used as counter
    private final AtomicLong next;
    private Range range;

    public ReadWriteLockIdGenerator(String name, RangeAssigner rangeAssigner) {
        this.name = name;
        this.rangeAssigner = rangeAssigner;
        this.range = rangeAssigner.nextRange();
        this.next = new AtomicLong(this.range.start());
    }

    @Override
    public long generateId() {
        try {
            readLock.lock();
            // Note, even though this is an AtomicLong
            // if you moved it outside the read lock
            // it would've interleaved with the write lock logic
            long nextId = this.next.getAndIncrement();
            if (this.range.isWithin(nextId)) {
                return nextId;
            }
        } finally {
            readLock.unlock();
        }

        try {
            writeLock.lock();
            long nextId = this.next.getAndIncrement();

            // Double check counter against the range to prevent empty ranges
            // Following situation is possible:
            // [T1] increment counter, counter is not withing the range, acquire the WriteLock
            // [T2] increment counter, counter is not withing the range, wait for the WriteLock
            // [T1] update range, update counter
            // [T2] update range, update counter
            if (this.range.isWithin(nextId)) {
                // Safely return current counter value since we're inside the WriteLock
                // e.g. readers couldn't have updated its value
                // as all of them are blocked waiting to get access to the counter
                log.info("[{}] Aborted range update due to the race condition nextId:{}", name, nextId);
                return nextId;
            }

            this.range = rangeAssigner.nextRange();
            this.next.set(this.range.start());

            nextId = this.next.getAndIncrement();
            log.info("[{}] Assigned new range [{}], nextId:{}", this.name, this.range, nextId);
            return nextId;
        } finally {
            writeLock.unlock();
        }
    }
}
