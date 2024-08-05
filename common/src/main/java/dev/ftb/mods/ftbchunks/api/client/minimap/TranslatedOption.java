package dev.ftb.mods.ftbchunks.api.client.minimap;

public record TranslatedOption(
   String optionName,
   String translationKey
) {

    public static TranslatedOption of(String optionName) {
        String translatedKey = optionName.toLowerCase().replaceAll("[^a-z0-9]", "_");
        return new TranslatedOption(optionName, "minimap.option." + translatedKey);
    }
}
