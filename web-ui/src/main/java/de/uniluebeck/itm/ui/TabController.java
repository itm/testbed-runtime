/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uniluebeck.itm.ui;

/**
 * @author soenke
 */
public class TabController {

    private static TabController _instance = null;
    private TabView _view;

    private TabController() {
        _view = new TabView();
    }

    public static TabController get() {
        if (_instance == null) {
            _instance = new TabController();
        }
        return _instance;
    }

    public TabView view() {
        return _view;
    }
}
