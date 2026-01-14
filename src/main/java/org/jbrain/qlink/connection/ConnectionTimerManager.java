/*
Copyright Jim Brain and Brain Innovations, 2005.

This file is part of QLinkServer.

QLinkServer is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

QLinkServer is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with QLinkServer; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package org.jbrain.qlink.connection;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.jbrain.qlink.QConfig;

/**
 * Manages all timers for a QConnection: ping, keepalive, and suspend watchdog.
 */
public class ConnectionTimerManager {
    private static final Logger _log = Logger.getLogger(ConnectionTimerManager.class);
    private static final Configuration _config = QConfig.getInstance();

    // Timer intervals (in milliseconds)
    private static final long PING_INTERVAL = _config.getLong("qlink.ping.interval", 2000);
    private static final long KEEPALIVE_INTERVAL = _config.getLong("qlink.keepalive.interval", 90000);
    private static final long SUSPEND_TIMEOUT = _config.getLong("qlink.suspend.timeout", 450000); // 5 * 90000

    private final Timer _timer = new Timer(true); // daemon timer
    private final ConnectionTimerCallback _callback;
    private final boolean _keepaliveEnabled;

    private TimerTask _pingTask;
    private KeepAliveTask _keepAliveTask;
    private TimerTask _suspendWatchdog;

    public interface ConnectionTimerCallback {
        void onPing();
        void onKeepaliveTimeout();
        void onSuspendTimeout();
    }

    public ConnectionTimerManager(ConnectionTimerCallback callback) {
        this._callback = callback;
        Boolean enabled = _config.getBoolean("qlink.keepalive.enabled", true);
        if (System.getenv("QLINK_SHOULD_PING") != null) {
            enabled = Boolean.parseBoolean(System.getenv("QLINK_SHOULD_PING"));
        }
        this._keepaliveEnabled = enabled;
    }

    /**
     * Starts the ping timer for flow control.
     */
    public synchronized void startPingTimer() {
        if (_pingTask == null) {
            _log.debug("Starting PING timer");
            _pingTask = new TimerTask() {
                @Override
                public void run() {
                    _callback.onPing();
                }
            };
            _timer.scheduleAtFixedRate(_pingTask, PING_INTERVAL, PING_INTERVAL);
        }
    }

    /**
     * Stops the ping timer.
     */
    public synchronized void stopPingTimer() {
        if (_pingTask != null) {
            _log.debug("Stopping PING timer");
            _pingTask.cancel();
            _pingTask = null;
        }
    }

    /**
     * Starts the keepalive timer for connection health monitoring.
     */
    public synchronized void startKeepaliveTimer() {
        if (_keepAliveTask == null && _keepaliveEnabled) {
            _log.debug("Creating keep alive timer");
            _keepAliveTask = new KeepAliveTask();
            _log.debug("Scheduling keep alive timer for " + KEEPALIVE_INTERVAL + "ms intervals");
            _timer.scheduleAtFixedRate(_keepAliveTask, KEEPALIVE_INTERVAL, KEEPALIVE_INTERVAL);
        } else if (_keepaliveEnabled && _keepAliveTask != null) {
            _log.warn("Resuming, but KeepAliveTask already active");
        }
    }

    /**
     * Stops the keepalive timer.
     */
    public synchronized void stopKeepaliveTimer() {
        if (_keepAliveTask != null) {
            _keepAliveTask.cancel();
            _keepAliveTask = null;
        } else if (_keepaliveEnabled) {
            _log.error("Suspending, but KeepAliveTask is null!");
        }
    }

    /**
     * Resets the keepalive timer (called when data is received).
     */
    public void resetKeepalive() {
        if (_keepAliveTask != null) {
            _keepAliveTask.reset();
        }
    }

    /**
     * Starts the suspend watchdog timer.
     */
    public synchronized void startSuspendWatchdog() {
        _suspendWatchdog = new TimerTask() {
            @Override
            public void run() {
                _callback.onSuspendTimeout();
            }
        };
        _log.debug("Scheduling suspend watchdog for " + SUSPEND_TIMEOUT + "ms");
        _timer.schedule(_suspendWatchdog, SUSPEND_TIMEOUT);
    }

    /**
     * Stops the suspend watchdog timer.
     */
    public synchronized void stopSuspendWatchdog() {
        if (_suspendWatchdog != null) {
            _suspendWatchdog.cancel();
            _suspendWatchdog = null;
        }
    }

    /**
     * Stops all timers and cancels the timer thread.
     */
    public synchronized void shutdown() {
        stopPingTimer();
        stopKeepaliveTimer();
        stopSuspendWatchdog();
        _timer.cancel();
    }

    /**
     * Inner class for keepalive with outstanding ping tracking.
     */
    private class KeepAliveTask extends TimerTask {
        private boolean _outstandingPing = false;

        @Override
        public void run() {
            if (_outstandingPing) {
                _log.debug("KeepAlive ping went unanswered, closing link");
                _callback.onKeepaliveTimeout();
            } else {
                _callback.onPing();
                _outstandingPing = true;
            }
        }

        public void reset() {
            _outstandingPing = false;
        }
    }
}
