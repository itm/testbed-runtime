/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uniluebeck.itm.services;

/**
 *
 * @author soenke
 */
public class ReservationServiceAdapter {
    private String rsEndpointUrl;

    public ReservationServiceAdapter(String rsEndpointUrl) {
        this.rsEndpointUrl = rsEndpointUrl;
    }

    public String getRsEndpointUrl() {
        return rsEndpointUrl;
    }
}
