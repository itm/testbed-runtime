package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.base.Function;
import de.uniluebeck.itm.netty.handlerstack.dlestxetx.DleStxEtxFramingDecoder;
import de.uniluebeck.itm.netty.handlerstack.dlestxetx.DleStxEtxFramingEncoder;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder;
import org.jboss.netty.handler.codec.embedder.EncoderEmbedder;

import javax.annotation.Nullable;

public class BenchmarkHelper {

	public static class MinFunction implements Function<Iterable<Float>, Float> {

		@Override
		public Float apply(@Nullable final Iterable<Float> values) {

			if (values == null) {
				throw new RuntimeException("Null values are not allowed");
			}

			float min = Float.MAX_VALUE;

			for (float value : values) {
				if (value < min) {
					min = value;
				}
			}

			return min;
		}
	}

	public static class MaxFunction implements Function<Iterable<Float>, Float> {

		@Override
		public Float apply(@Nullable final Iterable<Float> values) {

			if (values == null) {
				throw new RuntimeException("Null values are not allowed");
			}

			float max = Float.MIN_VALUE;

			for (float value : values) {
				if (value > max) {
					max = value;
				}
			}

			return max;
		}
	}

	public static class MeanFunction implements Function<Iterable<Float>, Float> {

		@Override
		public Float apply(@Nullable final Iterable<Float> values) {

			if (values == null) {
				throw new RuntimeException("Null values are not allowed");
			}

			int valueCnt = 0;
			float sum = 0f;

			for (float value : values) {
				valueCnt++;
				sum += value;
			}

			return sum / valueCnt;
		}
	}

	public static final MinFunction MIN = new MinFunction();

	public static final MaxFunction MAX = new MaxFunction();

	public static final MeanFunction MEAN = new MeanFunction();

	private EncoderEmbedder<ChannelBuffer> encoder = new EncoderEmbedder<ChannelBuffer>(new DleStxEtxFramingEncoder());

	private DecoderEmbedder<ChannelBuffer> decoder = new DecoderEmbedder<ChannelBuffer>(new DleStxEtxFramingDecoder());

	public ChannelBuffer encode(final ChannelBuffer decodedBuffer) {
		encoder.offer(decodedBuffer);
		return encoder.poll();
	}

	public ChannelBuffer decode(final ChannelBuffer encodedBuffer) {
		decoder.offer(encodedBuffer);
		return decoder.poll();
	}

	public static byte[] toByteArray(final ChannelBuffer buffer) {
		byte[] arr = new byte[buffer.readableBytes()];
		buffer.getBytes(0, arr);
		return arr;
	}

}
