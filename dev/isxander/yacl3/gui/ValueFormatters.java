package dev.isxander.yacl3.gui;

import dev.isxander.yacl3.api.controller.ValueFormatter;
import net.minecraft.class_2561;

public final class ValueFormatters {
    public static ValueFormatter<Float> percent(int decimalPlaces) {
        return new PercentFormatter(decimalPlaces);
    }

    public record PercentFormatter(int decimalPlaces) implements ValueFormatter<Float> {
        public PercentFormatter() {
            this(1);
        }

        @Override
        public class_2561 format(Float value) {
            return class_2561.method_43470(String.format("%." + decimalPlaces + "f%%", value * 100));
        }
    }
}
