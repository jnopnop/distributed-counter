package org.jnopnop.concurrency.generator.id;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jnopnop.concurrency.generator.id.domain.Range;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class NonBlockingIdGenerator implements IdGenerator {
    @Getter
    private final String name;
    private final RangeAssigner rangeAssigner;

    private final AtomicReference<RangeCounter> rangeCounter;

    public NonBlockingIdGenerator(String name, RangeAssigner rangeAssigner) {
        this.name = name;
        this.rangeAssigner = rangeAssigner;

        Range startRange = rangeAssigner.nextRange();
        this.rangeCounter = new AtomicReference<>(new RangeCounter(startRange));
    }

    @Override
    public long generateId() {
        // Case 1: Increment within the range
        // Volatile read, fastest
        RangeCounter currState = this.rangeCounter.get();
        long nextId = currState.nextId();
        if (currState.isWithin(nextId)) {
            log.debug("[{}] WITHIN [{}]: {}", name, currState.range(), nextId);
            return nextId;
        }

        // Case 2: Increment out of range
        while (!currState.isWithin(nextId)) {
            // Case 2.1: Increment to exact range end
            // A thread who observed given condition is the only allowed to assign a new range
            if (currState.isRangeEnd(nextId)) {
                Range nextRange = this.rangeAssigner.nextRange();
                RangeCounter nextState = new RangeCounter(nextRange);
                nextId = nextState.nextId();

                // Make the new state visible to other threads
                // only after taking the first id from it
                // Otherwise it is possible that between publishing the new range
                // and generating an id from it - that id would exceed range boundaries
                // effectively forcing this thread to keep spinning
                // An effect known as Live Lock
                this.rangeCounter.set(nextState);

                log.info("[{}] UPD [{}]: {}", name, nextRange, nextId);
                return nextId;
            }

            // Case 2.2: Increment above the range
            // Busy-wait until other thread pulls a new range,
            // and we successfully generate an id withing it
            // First of all giving a hint to the scheduler that
            // given thread is ok to give its CPU usage to the other threads
            Thread.yield();

            log.info("[{}] ABOVE [{}]: {}", name, currState.range(), nextId);
            currState = this.rangeCounter.get();
            nextId = currState.nextId();
        }
        log.info("[{}] ABOVE FINISHED [{}]: {}", name, currState.range(), nextId);
        return nextId;
    }

}
