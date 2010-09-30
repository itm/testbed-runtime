/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/
package de.uniluebeck.itm.common;

import com.vaadin.ui.Window;

/**
 * @author Soenke Nommensen
 */
public class UiUtil {

    private static Window _mainWindow = null;

    public static void setMainWindow(Window mainWindow) {
        _mainWindow = mainWindow;
    }

    /**
     * Creates a window notification that can be displayed on a Vaadin window at centered top position.
     *
     * @param caption     The notification's caption
     * @param description The notification's description text
     * @param type        Sets the notification type (choose from Window.Notification.TYPE_...)
     * @return A notification at a centered top position
     */
    public static Window.Notification createNotificationCenteredTop(final String caption, final String description, final int type) {
        Window.Notification notification = new Window.Notification(
                caption,
                description, type);
        notification.setPosition(Window.Notification.POSITION_CENTERED_TOP);
        return notification;
    }

    /**
     * Creates a window notification that can be displayed on a Vaadin window at the defined position.
     *
     * @param caption     The notification's caption
     * @param description The notification's description text
     * @param type        Sets the notification type (choose from Window.Notification.TYPE_...)
     * @param position    Sets the notification's position
     * @return A notification at a centered top position
     */
    public static Window.Notification createNotification(String caption, String description, int type, int position) {
        Window.Notification notification = new Window.Notification(
                caption,
                description, type);
        notification.setPosition(position);
        return notification;
    }

    /**
     * Creates a window notification that can be displayed on a Vaadin window at the default position.
     *
     * @param caption     The notification's caption
     * @param description The notification's description text
     * @param type        Sets the notification type (choose from Window.Notification.TYPE_...)
     * @return notification
     */
    public static Window.Notification createNotification(String caption, String description, int type) {
        return new Window.Notification(
                caption,
                description, type);
    }

    /**
     * Shows a notification on the specified window
     *
     * @param notification The notification that will be displayed on the specified window
     */
    public static void showNotification(final Window.Notification notification) {
        _mainWindow.showNotification(notification);
    }

    /**
     * Shows a notification on the specified window using the given parameters and position
     *
     * @param caption     The notification's caption
     * @param description The notification's description text
     * @param type        Sets the notification type (choose from Window.Notification.TYPE_...)
     * @param position    Sets the notification's position
     */
    public static void showNotification(final String caption, final String description, final int type, final int position) {
        _mainWindow.showNotification(createNotification(caption, description, type, position));
    }

    /**
     * Shows a notification on the specified window using the given parameters at the default position
     *
     * @param caption     The notification's caption
     * @param description The notification's description text
     * @param type        Sets the notification type (choose from Window.Notification.TYPE_...)
     */
    public static void showNotification(final String caption, final String description, int type) {
        _mainWindow.showNotification(createNotification(caption, description, type));
    }

    /**
     * Shows a notification on the specified window
     *
     * @param window       The window that is used to display the specified notification
     * @param notification The notification that will be displayed on the specified window
     */
    public static void showNotification(final Window window, final Window.Notification notification) {
        window.showNotification(notification);
    }

    /**
     * Shows a notification on the specified window using the given parameters and position
     *
     * @param window      The window that is used to display the specified notification
     * @param caption     The notification's caption
     * @param description The notification's description text
     * @param type        Sets the notification type (choose from Window.Notification.TYPE_...)
     * @param position    Sets the notification's position
     */
    public static void showNotification(
            final Window window, final String caption, final String description,
            final int type, final int position) {
        window.showNotification(createNotification(caption, description, type, position));
    }

    /**
     * Shows a notification on the specified window using the given parameters at the default position
     *
     * @param window      The window that is used to display the specified notification
     * @param caption     The notification's caption
     * @param description The notification's description text
     * @param type        Sets the notification type (choose from Window.Notification.TYPE_...)
     */
    public static void showNotification(final Window window, final String caption, final String description, final int type) {
        window.showNotification(createNotification(caption, description, type));
    }
}
