package org.jnopnop.concurrency.generator.id;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jnopnop.concurrency.generator.id.domain.Range;

@Slf4j
public class SynchronizedIdGenerator implements IdGenerator {
    @Getter
    private final String name;
    private final RangeAssigner rangeAssigner;
    private RangeCounter state;

    public SynchronizedIdGenerator(String name, RangeAssigner rangeAssigner) {
        this.name = name;
        this.rangeAssigner = rangeAssigner;

        Range startRange = rangeAssigner.nextRange();
        this.state = new RangeCounter(startRange);
    }

    @Override
    public synchronized long generateId() {
        // Case 1: Increment within the range
        long nextId = state.nextId();
        if (state.isWithin(nextId)) {
            log.debug("[{}] WITHIN [{}]: {}", name, state.range(), nextId);
            return nextId;
        }

        // Case 2: Increment to exact range end
        this.state = new RangeCounter(this.rangeAssigner.nextRange());
        nextId = this.state.nextId();
        log.info("[{}] UPD [{}]: {}", this.name, this.state.range(), nextId);
        return nextId;
    }

}
