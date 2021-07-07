package dev.ftb.mods.ftbchunks.client.map;

public interface ColorsFromRegion {
	ColorsFromRegion FOLIAGE = new ColorsFromRegion() {
		@Override
		public int[] getColors(MapRegionData regionData) {
			return regionData.foliage;
		}

		@Override
		public String getName() {
			return "foliage";
		}
	};

	ColorsFromRegion GRASS = new ColorsFromRegion() {
		@Override
		public int[] getColors(MapRegionData regionData) {
			return regionData.grass;
		}

		@Override
		public String getName() {
			return "grass";
		}
	};

	ColorsFromRegion WATER = new ColorsFromRegion() {
		@Override
		public int[] getColors(MapRegionData regionData) {
			return regionData.water;
		}

		@Override
		public String getName() {
			return "water";
		}
	};

	int[] getColors(MapRegionData regionData);

	String getName();
}