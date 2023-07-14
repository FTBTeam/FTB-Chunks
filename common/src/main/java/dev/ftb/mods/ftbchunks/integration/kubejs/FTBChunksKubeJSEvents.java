package dev.ftb.mods.ftbchunks.integration.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.Extra;

public interface FTBChunksKubeJSEvents {
    EventGroup EVENT_GROUP = EventGroup.of("FTBChunksEvents");

    EventHandler BEFORE = EVENT_GROUP.server("before", () -> BeforeEventJS.class).extra(Extra.REQUIRES_STRING).cancelable();
    EventHandler AFTER = EVENT_GROUP.server("after", () -> AfterEventJS.class).extra(Extra.REQUIRES_STRING);
}
