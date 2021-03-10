package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.client.FTBChunksClient;
import com.feed_the_beast.mods.ftbchunks.client.map.RegionSyncKey;
import com.feed_the_beast.mods.ftbchunks.client.map.SyncRXTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class PartialPackets<Key, Packet> {
	public static final PartialPackets<RegionSyncKey, SyncTXPacket> REGION = new PartialPackets<>(SyncTXPacket::new, (key, data) -> FTBChunksClient.queue(new SyncRXTask(key, data)));

	public static class PartialData {
		public int remaining;
		public final byte[] data;

		public PartialData(int s) {
			remaining = s;
			data = new byte[s];
		}
	}

	@FunctionalInterface
	public interface PacketFactory<K, P> {
		P create(K key, int offset, int total, byte[] data);
	}

	@FunctionalInterface
	public interface Callback<K> {
		void accept(K key, byte[] data);
	}

	public final Map<Key, PartialData> map;
	public final PacketFactory<Key, Packet> packetFactory;
	public final Callback<Key> callback;

	public PartialPackets(PacketFactory<Key, Packet> pf, Callback<Key> c) {
		map = new HashMap<>();
		packetFactory = pf;
		callback = c;
	}

	public List<Packet> write(Key key, byte[] data) {
		if (data.length <= 30000) {
			return Collections.singletonList(packetFactory.create(key, 0, data.length, data));
		}

		List<Packet> list = new ArrayList<>();

		int r = data.length;
		int i = 0;

		while (r >= 30000) {
			byte[] data1 = new byte[30000];
			System.arraycopy(data, i, data1, 0, 30000);
			list.add(packetFactory.create(key, i, data.length, data1));
			r -= 30000;
			i += 30000;
		}

		if (r > 0) {
			byte[] data1 = new byte[r];
			System.arraycopy(data, i, data1, 0, r);
			list.add(packetFactory.create(key, i, data.length, data1));
		}

		return list;
	}

	public void read(Key key, int offset, final int total, byte[] data) {
		PartialData partialData = map.computeIfAbsent(key, s -> new PartialData(total));

		System.arraycopy(data, 0, partialData.data, offset, data.length);
		partialData.remaining -= data.length;

		if (partialData.remaining == 0) {
			callback.accept(key, partialData.data);
			map.remove(key);
		} else if (partialData.remaining < 0) {
			throw new RuntimeException("Read more data than required");
		}
	}
}
