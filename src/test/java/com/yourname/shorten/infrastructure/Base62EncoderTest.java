package com.yourname.shorten.infrastructure;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62EncoderTest {

    @Test
    void encode_zero_returnsZero() {
        assertThat(Base62Encoder.encode(0L)).isEqualTo("0");
    }

    @Test
    void encode_one_returnsOne() {
        assertThat(Base62Encoder.encode(1L)).isEqualTo("1");
    }

    @Test
    void encode_ten_returnsA() {
        assertThat(Base62Encoder.encode(10L)).isEqualTo("a");
    }

    @Test
    void encode_thirtySix_returnsCapitalA() {
        assertThat(Base62Encoder.encode(36L)).isEqualTo("A");
    }

    @Test
    void encode_sixtyOne_returnsCapitalZ() {
        assertThat(Base62Encoder.encode(61L)).isEqualTo("Z");
    }

    @Test
    void encode_sixtyTwo_returnsTen() {
        assertThat(Base62Encoder.encode(62L)).isEqualTo("10");
    }

    @Test
    void encode_largeNumber_returnsCorrectString() {
        assertThat(Base62Encoder.encode(12345L)).isEqualTo("3d7");
    }

    @Test
    void encode_negative_throws() {
        assertThatThrownBy(() -> Base62Encoder.encode(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
