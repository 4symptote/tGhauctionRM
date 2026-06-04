package com.app.client.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeUtilTest {

    @Test
    void formatExactDateUsesReadablePattern() {
        String formatted = TimeUtil.formatExactDate(0L);

        assertThat(formatted).contains("1970");
        assertThat(formatted).matches("[A-Z][a-z]{2} \\d{2}, \\d{4} \\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    void formatCountdownReturnsEndedForPastTarget() {
        assertThat(TimeUtil.formatCountdown(System.currentTimeMillis() - 1_000L)).isEqualTo("Ended");
    }

    @Test
    void formatCountdownUsesDayPrefixWhenTargetIsFarInFuture() {
        long target = System.currentTimeMillis() + (2L * 24 * 60 * 60 * 1000) + 5_000L;

        String countdown = TimeUtil.formatCountdown(target);

        assertThat(countdown).startsWith("2d ");
    }

    @Test
    void formatCountdownUsesClockFormatWhenLessThanOneDayRemains() {
        long target = System.currentTimeMillis() + (3L * 60 * 60 * 1000) + (4L * 60 * 1000) + 5_000L;

        String countdown = TimeUtil.formatCountdown(target);

        assertThat(countdown).matches("\\d{2}:\\d{2}:\\d{2}");
    }
}
