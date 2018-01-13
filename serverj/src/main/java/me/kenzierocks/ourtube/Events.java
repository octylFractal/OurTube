package me.kenzierocks.ourtube;

import java.util.HashMap;
import java.util.Map;

import com.google.common.eventbus.EventBus;

public class Events {

	private final String id;
	private final Map<String, EventBus> guildEventBuses = new HashMap<>();

	public Events(String id) {
		this.id = id;
	}

	public void subscribe(String guildId, Object subscription) {
		guildEventBuses.computeIfAbsent(guildId, k -> new EventBus(id + "-" + guildId)).register(subscription);
	}

	public void unsubscribe(String guildId, Object subscription) {
		EventBus bus = guildEventBuses.get(guildId);
		if (bus != null) {
			bus.unregister(subscription);
		}
	}

	public void post(String guildId, Object event) {
		EventBus bus = guildEventBuses.get(guildId);
		if (bus != null) {
			bus.post(event);
		}
	}

}
