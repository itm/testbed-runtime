package de.uniluebeck.itm.tr.iwsn.common.netty;

import com.google.common.base.Throwables;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A ChannelHandler that uses KEEP_ALIVE and KEEP_ALIVE_ACK messages to detect TCP connection outages at application
 * layer. Every KEEP_ALIVE_INTERVAL_MS milliseconds it will send a KEEP_ALIVE message to the other TCP endpoint. If the
 * other endpoint does not answer within a certain time frame this handler will assume a dead TCP connection and close
 * the channel. Closing the Channel will then create a corresponding event so that other components can detect the
 * closed Channel and try to create a new one.
 */
public class KeepAliveHandler extends SimpleChannelUpstreamHandler {

	private static final Logger log = LoggerFactory.getLogger(KeepAliveHandler.class);

	private static final int KEEP_ALIVE_INTERVAL_MS = 5000;

	private static final int KEEP_ALIVE_TOLERANCE_MS = 2500;

	private final SchedulerService schedulerService;

	// lock for thread-shared vars
	private final Lock lock = new ReentrantLock();

	// thread-shared var
	private ChannelHandlerContext chc;

	// thread-shared var
	private long lastLifeSign;

	private Runnable checkLivelinessRunnable = new Runnable() {
		@Override
		public void run() {

			try {

				if ((System.currentTimeMillis() - lastLifeSign) > (KEEP_ALIVE_INTERVAL_MS + KEEP_ALIVE_TOLERANCE_MS)) {

					log.warn(
							"KeepAliveHandler[remote={},lastLifeSign={}] detected dead TCP channel. Closing it...",
							chc.getChannel().getRemoteAddress(),
							lastLifeSign
					);

					lock.lock();
					try {
						stopSchedule();
						chc.getChannel().close();
					} finally {
						lock.unlock();
					}
				}

				sendKeepAlive();

			} catch (Exception e) {

				log.warn(
						"KeepAliveHandler[remote={},lastLifeSign={}]: Exception during execution of checkLivelinessRunnable: {}, StackTrace: {}",
						chc.getChannel().getRemoteAddress(),
						lastLifeSign,
						e.getMessage(),
						Throwables.getStackTraceAsString(e)
				);
			}
		}
	};

	private ScheduledFuture<?> schedule;

	public KeepAliveHandler(final SchedulerService schedulerService) {
		this.schedulerService = schedulerService;
	}

	@Override
	public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {

		lock.lock();
		try {

			log.trace(
					"KeepAliveHandler[remote={},lastLifeSign={}] received connection event. Starting work.",
					ctx.getChannel().getRemoteAddress(),
					lastLifeSign
			);

			chc = ctx;
			lastLifeSign = System.currentTimeMillis();
			schedule = schedulerService.scheduleAtFixedRate(
					checkLivelinessRunnable,
					1, KEEP_ALIVE_INTERVAL_MS, TimeUnit.MILLISECONDS
			);

		} catch (Exception e1) {
			log.error("Exception in KeepAliveHandler.channelConnected: ", e1);
		} finally {
			lock.unlock();
		}

		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {

		lock.lock();
		try {

			log.trace(
					"KeepAliveHandler[remote={},lastLifeSign={}] received disconnection event. Stopping work.",
					ctx.getChannel().getRemoteAddress(),
					lastLifeSign
			);

			stopSchedule();
			chc = null;

		} catch (Exception e1) {
			log.error("Exception in KeepAliveHandler.channelDisconnected: ", e1);
		} finally {
			lock.unlock();
		}

		super.channelDisconnected(ctx, e);
	}

	private void stopSchedule() {
		lock.lock();
		try {
			if (schedule != null) {
				/*
				log.trace(
						"KeepAliveHandler[remote={},lastLifeSign={}] cancelling schedule",
						chc.getChannel().getRemoteAddress(),
						lastLifeSign
				);
				*/
				schedule.cancel(true);
				schedule = null;
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

		lock.lock();

		try {

			@SuppressWarnings("UnnecessaryLocalVariable")
			final long now = System.currentTimeMillis();

			/*
			log.trace(
					"KeepAliveHandler[remote={},lastLifeSign={}] received message. Updating lastLifeSign <= {}.",
					chc.getChannel().getRemoteAddress(),
					lastLifeSign,
					now
			);
			*/

			lastLifeSign = now;

			if (e.getMessage() instanceof Message && ((Message) e.getMessage()).getType() == Message.Type.KEEP_ALIVE) {
				sendKeepAliveAck();
			}

		} catch (Exception e1) {
			log.error("Exception in KeepAliveHandler.messageReceived: ", e1);
		} finally {
			lock.unlock();
		}

		super.messageReceived(ctx, e);
	}

	private void sendKeepAliveAck() {

		lock.lock();
		try {

			/*
			if (log.isTraceEnabled()) {
				log.trace(
						"KeepAliveHandler[remote={},lastLifeSign={}] received KEEP_ALIVE, sending KEEP_ALIVE_ACK",
						chc.getChannel().getRemoteAddress(),
						lastLifeSign
				);
			}
			*/

			Channels.write(chc.getChannel(), Message.newBuilder().setType(Message.Type.KEEP_ALIVE_ACK).build());

		} finally {
			lock.unlock();
		}
	}

	private void sendKeepAlive() {

		lock.lock();
		try {

			/*
			if (log.isTraceEnabled()) {
				log.trace(
						"KeepAliveHandler[remote={},lastLifeSign={}] sending KEEP_ALIVE",
						chc.getChannel().getRemoteAddress(),
						lastLifeSign
				);
			}
			*/

			Channels.write(chc.getChannel(), Message.newBuilder().setType(Message.Type.KEEP_ALIVE).build());

		} finally {
			lock.unlock();
		}
	}
}
