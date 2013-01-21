package de.uniluebeck.itm.tr.iwsn.gateway;

import java.util.Random;

public class RandomEventIdProvider implements EventIdProvider {

	private final Random random = new Random();

	@Override
	public Long get() {
		return random.nextLong();
	}
}
