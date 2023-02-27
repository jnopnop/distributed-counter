package org.jnopnop.concurrency.generator.id;

import org.jnopnop.concurrency.generator.id.domain.Range;

public interface RangeAssigner {

    Range nextRange();
}
