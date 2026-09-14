package kireiko.dev.anticheat.api.data;

import java.util.Map;

public record ConfigLabel(String name, Map<String, Object> parameters) {
}
