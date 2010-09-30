/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uniluebeck.itm.ui;

/**
 * @author soenke
 */
class ToolbarController implements Controller {

    private static ToolbarController _instance = null;
    private ToolbarView _view;

    private ToolbarController() {
        _view = new ToolbarView();
    }

    public static ToolbarController get() {
        if (_instance == null) {
            _instance = new ToolbarController();
        }
        return _instance;
    }

    public ToolbarView view() {
        return _view;
    }
}
