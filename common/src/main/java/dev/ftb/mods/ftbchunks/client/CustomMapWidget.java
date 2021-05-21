package dev.ftb.mods.ftbchunks.client;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Widget;

public abstract class CustomMapWidget extends Widget {

    public CustomMapWidget(Panel panel) {
        super(panel);
    }

    public abstract int getX();

    public abstract int getZ();

}
