package de.uniluebeck.itm.tr.common;

import java.util.Random;

public class RandomIdProvider implements IdProvider {

	private final Random random = new Random();

	@Override
	public Long get() {
		return random.nextLong();
	}
}
