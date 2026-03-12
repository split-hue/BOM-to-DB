package com.ksenija;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Configures the Vaadin application shell.
 * <p>
 * Sets the UI theme to "ksenija" (extends Lumo) and loads
 * the Lumo utility stylesheet for use across all views.
 *
 * @see AppShellConfigurator
 */
@StyleSheet(Lumo.STYLESHEET)
@Theme("ksenija")
public class AppShell implements AppShellConfigurator {
}
