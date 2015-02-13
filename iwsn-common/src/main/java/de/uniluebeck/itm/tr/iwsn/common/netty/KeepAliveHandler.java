package de.uniluebeck.itm.tr.iwsn.common.netty;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import de.uniluebeck.itm.tr.iwsn.messages.Message;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;
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
    private Stopwatch stopwatch = Stopwatch.createUnstarted();

    private Runnable checkLivelinessRunnable = new Runnable() {
        @Override
        public void run() {

            final long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);

            try {

                if (elapsed > (KEEP_ALIVE_INTERVAL_MS + KEEP_ALIVE_TOLERANCE_MS)) {

                    if (chc.getChannel() == null) {
                        log.trace("KeepAliveHandler.checkLivelinessRunnable detected dead TCP channel (already null). Stopping work.");
                        stopSchedule();
                        return;
                    }

                    log.warn(
                            "KeepAliveHandler[remote={},msElapsed={}] detected dead TCP channel. Closing it...",
                            chc.getChannel().getRemoteAddress(),
                            elapsed
                    );

                    lock.lock();
                    try {
                        chc.getChannel().close();
                        stopSchedule();
                    } finally {
                        lock.unlock();
                    }

                } else {
                    sendKeepAlive();
                }

            } catch (Exception e) {

                log.warn(
                        "KeepAliveHandler[remote={},msElapsed={}]: Exception during execution of checkLivelinessRunnable: {}, StackTrace: {}",
                        chc == null ? "unknown" : chc.getChannel() == null ? "unknown" : chc.getChannel().getRemoteAddress(),
                        elapsed,
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

            chc = ctx;
            stopwatch.reset();
            stopwatch.start();

            log.trace(
                    "KeepAliveHandler[remote={},msElapsed={}] received connection event. Starting work.",
                    ctx.getChannel().getRemoteAddress(),
                    stopwatch.elapsed(TimeUnit.MILLISECONDS)
            );

            sendKeepAlive();

            schedule = schedulerService.scheduleAtFixedRate(
                    checkLivelinessRunnable,
                    KEEP_ALIVE_TOLERANCE_MS, KEEP_ALIVE_INTERVAL_MS, TimeUnit.MILLISECONDS
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

			/*
            log.trace(
					"KeepAliveHandler[remote={},msElapsed={}] received disconnection event. Stopping work.",
					ctx.getChannel().getRemoteAddress(),
					stopwatch.elapsed(TimeUnit.MILLISECONDS)
			);
			*/

            stopSchedule();
            stopwatch.reset();
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
                log.trace(
                        "KeepAliveHandler[remote={},msElapsed={}] cancelling schedule",
                        chc.getChannel().getRemoteAddress(),
                        stopwatch.elapsed(TimeUnit.MILLISECONDS)
                );
                schedule.cancel(true);
                schedule = null;
            } else if (log.isWarnEnabled()) {
                log.warn(
                        "KeepAliveHandler[remote={},msElapsed={}] schedule already null",
                        chc.getChannel().getRemoteAddress(),
                        stopwatch.elapsed(TimeUnit.MILLISECONDS)
                );
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

        lock.lock();

        try {

			/*
			// should receive one KEEP_ALIVE every 5 seconds from remote (and send an KEEP_ALIVE_ACK back) as well as
			// one KEEP_ALIVE_ACK as response to the KEEP_ALIVE sent to remote every 5 seconds ourselves
			log.trace(
					"KeepAliveHandler[remote={},msElapsed={}] received message {}. Updating msElapsed.",
					chc.getChannel().getRemoteAddress(),
					stopwatch.elapsed(TimeUnit.MILLISECONDS),
					e.getMessage()
			);
			*/

            stopwatch.reset();
            stopwatch.start();

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
						"KeepAliveHandler[remote={},msElapsed={}] received KEEP_ALIVE, sending KEEP_ALIVE_ACK",
						chc.getChannel().getRemoteAddress(),
						stopwatch.elapsed(TimeUnit.MILLISECONDS)
				);
			}
			*/

            final Channel channel = chc.getChannel();

            if (channel == null) {
                log.warn("Channel is null, stopping work");
                stopSchedule();
                return;
            }

            final Message message = Message.newBuilder().setType(Message.Type.KEEP_ALIVE_ACK).build();

            Channels.write(channel, message);

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
						"KeepAliveHandler[remote={},msElapsed={}] sending KEEP_ALIVE",
						chc.getChannel().getRemoteAddress(),
						stopwatch.elapsed(TimeUnit.MILLISECONDS)
				);
			}
			*/

            try {

                final Channel channel = chc.getChannel();

                if (channel == null) {
                    log.warn("Channel is null, stopping work");
                    stopSchedule();
                    return;
                }

                final Message message = Message.newBuilder().setType(Message.Type.KEEP_ALIVE).build();
                final ChannelFutureListener listener = new ChannelFutureListener() {
                    @Override
                    public void operationComplete(final ChannelFuture future) throws Exception {
                        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                        final boolean channelClosed = !future.isSuccess() &&
                                future.getCause() instanceof ClosedChannelException;
                        if (channelClosed) {
                            log.trace("KeepAliveHandler.operationComplete(success={},cause={})",
                                    future.isSuccess(), future.getCause()
                            );
                            future.getChannel().close();
                        }
                    }
                };

                Channels.write(channel, message).addListener(listener);

            } catch (Exception e) {
                log.warn("Exception while sending keep alive: ", e);
            }

        } finally {
            lock.unlock();
        }
    }
}
