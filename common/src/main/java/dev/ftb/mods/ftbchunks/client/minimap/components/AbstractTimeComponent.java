package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;

public abstract class AbstractTimeComponent implements MinimapInfoComponent {

    protected String createTimeString(int hours, int minutes, boolean twentyFourHourClock) {
        if(twentyFourHourClock) {
            return String.format("%02d:%02d", hours, minutes);
        } else {
            String ampm = hours >= 12 ? "PM" : "AM";
            if(hours == 0) {
                hours = 12;
            } else if(hours > 12) {
                hours -= 12;
            }
            return String.format("%02d:%02d %s", hours, minutes, ampm);
        }

    }
}
