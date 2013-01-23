package de.uniluebeck.itm.tr.iwsn.portal;

import java.util.Random;

public class RandomRequestIdProvider implements RequestIdProvider {

	private final Random random = new Random();

	@Override
	public Long get() {
		return random.nextLong();
	}
}
