package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.client.ClientTaskQueue;
import dev.ftb.mods.ftbchunks.client.map.RegionSyncKey;
import dev.ftb.mods.ftbchunks.client.map.SyncRXTask;

import java.util.*;

public class PartialPackets<Key, Packet> {
	public static final PartialPackets<RegionSyncKey, SyncRXPacket> REGION
			= new PartialPackets<>(SyncRXPacket::new, (key, data) -> ClientTaskQueue.queue(new SyncRXTask(key, data)));

	public static class PartialData {
		private int remaining;
		private final byte[] data;

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

	private final Map<Key, PartialData> map;
	private final PacketFactory<Key, Packet> packetFactory;
	private final Callback<Key> callback;

	public PartialPackets(PacketFactory<Key, Packet> packetFactory, Callback<Key> callback) {
		map = new HashMap<>();
		this.packetFactory = packetFactory;
		this.callback = callback;
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
