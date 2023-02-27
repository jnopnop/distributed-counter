package org.jnopnop.concurrency.generator.id;

import lombok.RequiredArgsConstructor;
import org.jnopnop.concurrency.generator.id.domain.Range;

import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public class MonotonicRangeAssigner implements RangeAssigner {
    private final AtomicLong nextRangeStart = new AtomicLong();
    private final long rangeSize;

    public Range nextRange() {
        long rangeStart = nextRangeStart.getAndUpdate(curr -> curr + rangeSize);
        return new Range(rangeStart, rangeStart + rangeSize);
    }
}
