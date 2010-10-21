/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uniluebeck.itm.ui.components;

import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.ui.LoginForm;

/**
 *
 * @author soenke
 */
public class UiLoginForm extends LoginForm {

    static final String FIELD_SIZE = "35";
    static final String SPACING = "5px";
    private String usernameCaption;
    private String passwordCaption;
    private String submitCaption;

    public UiLoginForm() {
        usernameCaption = "Username";
        passwordCaption = "Password";
        submitCaption = "Login";
    }

    public UiLoginForm(String usernameCaption,
            String passwordCaption, String submitCaption) {
        this.usernameCaption = usernameCaption;
        this.passwordCaption = passwordCaption;
        this.submitCaption = submitCaption;
    }

    @Override
    protected byte[] getLoginHTML() {
        // Application URI needed for submitting form
        String appUri = getApplication().getURL().toString()
                + getWindow().getName() + "/";
        String x, h, b; // XML header, HTML head and body

        // The XML header is needed for the validity of the XHTML page:
        x = "<!DOCTYPE html PUBLIC \"-//W3C//DTD "
                + "XHTML 1.0 Transitional//EN\" "
                + "\"http://www.w3.org/TR/xhtml1/"
                + "DTD/xhtml1-transitional.dtd\">\n";

        // The HTML header part contains JavaScript definitions that handle
        // submitting the form data. It also copies the style sheet references
        // from the parent window.
        h = "<head><script type='text/javascript'>"
                + "var setTarget = function() {"
                + "  var uri = '" + appUri + "loginHandler';"
                + "  var f = document.getElementById('loginf');"
                + "  document.forms[0].action = uri;"
                + "  document.forms[0].username.focus();"
                + "};"
                + ""
                + "var styles = window.parent.document.styleSheets;"
                + "for(var j = 0; j < styles.length; j++) {\n"
                + "  if(styles[j].href) {"
                + "    var stylesheet = document.createElement('link');\n"
                + "    stylesheet.setAttribute('rel', 'stylesheet');\n"
                + "    stylesheet.setAttribute('type', 'text/css');\n"
                + "    stylesheet.setAttribute('href', styles[j].href);\n"
                + "    document.getElementsByTagName('head')[0]"
                + "                .appendChild(stylesheet);\n"
                + "  }"
                + "}\n"
                + "function submitOnEnter(e) {"
                + "  var keycode = e.keyCode || e.which;"
                + "  if (keycode == 13) {document.forms[0].submit();}"
                + "}\n"
                + "</script>"
                + "</head>";

        // The HTML body element contains the actual form. Notice that it is
        // contained within an inner iframe. The form and the button must have
        // JavaScript calls to submit the form content.
        b = "<body onload='setTarget();'"
                + "  style='margin:0;padding:0; background:transparent;'"
                + "  class='"
                + ApplicationConnection.GENERATED_BODY_CLASSNAME + "'>"
                + "<div class='v-app v-app-loginpage'"
                + "     style='background:transparent;'>"
                + "<iframe name='logintarget' style='width:0;height:0;"
                + "border:0;margin:0;padding:0;'></iframe>"
                + "<form id='loginf' target='logintarget'"
                + "      onkeypress='submitOnEnter(event)'"
                + "      method='post'>"
                + "<table>"
                + "<tr><td style='padding:" + SPACING + ";'>" + usernameCaption + "</td>"
                + "<td style='padding:" + SPACING + "';><input class='v-textfield' style='display:block;'"
                + "           type='text' name='username' size='" + FIELD_SIZE + "'></td></tr>"
                + "<tr><td style='padding:" + SPACING + ";'>" + passwordCaption + "</td>"
                + "    <td style='padding:" + SPACING + ";'><input class='v-textfield'"
                + "          style='display:block;' type='password'"
                + "          name='password' size='" + FIELD_SIZE + "'></td></tr>"
                + "</table>"
                + "<div>"
                + "<div onclick='document.forms[0].submit();'"
                + "     tabindex='0' class='v-button' role='button' style='text-alignment:right;'>"
                + "<span class='v-button-wrap'>"
                + "<span class='v-button-caption'>"
                + submitCaption + "</span>"
                + "</span></div></div></form></div></body>";

        // Then combine and return the page as a byte array.
        return (x + "<html>" + h + b + "</html>").getBytes();
    }
}
