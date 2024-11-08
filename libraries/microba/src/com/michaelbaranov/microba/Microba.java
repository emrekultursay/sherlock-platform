package com.michaelbaranov.microba;

import com.michaelbaranov.microba.calendar.ui.basic.BasicCalendarPaneUI;
import com.michaelbaranov.microba.calendar.ui.basic.BasicDatePickerUI;
import com.michaelbaranov.microba.common.MicrobaComponent;
import com.michaelbaranov.microba.gradient.ui.basic.BasicGradientUI;
import com.michaelbaranov.microba.gradienteditor.ui.basic.BasicGradientEditorUI;
import com.michaelbaranov.microba.marker.ui.basic.BasicMarkerBarUI;
import com.michaelbaranov.microba.marker.ui.metal.MetalMarkerBarUI;
import com.michaelbaranov.microba.marker.ui.motif.MotifMarkerBarUI;
import com.michaelbaranov.microba.marker.ui.windows.WindowsMarkerBarUI;

import javax.swing.*;
import java.applet.Applet;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to initialize Microba library.
 * 
 * @author Michael Baranov
 * 
 */
public class Microba {

  private static UIChangeListener changeListener = new UIChangeListener();

  /**
   * Initializes the library: installs L&F properties, sets up a L&F change
   * listener.
   * <p>
   * No need to call this method explicitly for desktop applications. You
   * should only call it in {@link Applet#init()}. This will handle browser
   * refresh button correctly.
   * 
   */
  public static synchronized void init() {
    setLookAndFeelProperties(UIManager.getLookAndFeel());

    UIManager.removePropertyChangeListener(changeListener);
    UIManager.addPropertyChangeListener(changeListener);
  }

  private static synchronized void setLookAndFeelProperties(LookAndFeel lookAndFeel) {
    if (lookAndFeel == null) {
      return;
    }

    String packagePrefix = "com.michaelbaranov.microba.";

    // all L&F
    UIManager.put("microba.CalendarPaneUI", packagePrefix + "calendar.ui.basic.BasicCalendarPaneUI");
    UIManager.put(packagePrefix + "calendar.ui.basic.BasicCalendarPaneUI", BasicCalendarPaneUI.class);
    UIManager.put("microba.DatePickerUI", packagePrefix + "calendar.ui.basic.BasicDatePickerUI");
    UIManager.put(packagePrefix + "calendar.ui.basic.BasicDatePickerUI", BasicDatePickerUI.class);
    UIManager.put("microba.GradientUI", packagePrefix + "gradient.ui.basic.BasicGradientUI");
    UIManager.put(packagePrefix + "gradient.ui.basic.BasicGradientUI", BasicGradientUI.class);
    UIManager.put("microba.GradientEditorUI", packagePrefix + "gradienteditor.ui.basic.BasicGradientEditorUI");
    UIManager.put(packagePrefix + "gradienteditor.ui.basic.BasicGradientEditorUI", BasicGradientEditorUI.class);
    UIManager.put("microba.MarkerBarUI", packagePrefix + "marker.ui.basic.BasicMarkerBarUI");
    UIManager.put(packagePrefix + "marker.ui.basic.BasicMarkerBarUI", BasicMarkerBarUI.class);

    // particular L&F
    if (lookAndFeel.getID().equals("Windows")) {
      UIManager.put("microba.MarkerBarUI", packagePrefix + "marker.ui.windows.WindowsMarkerBarUI");
      UIManager.put(packagePrefix + "marker.ui.windows.WindowsMarkerBarUI", WindowsMarkerBarUI.class);
    }
    else if (lookAndFeel.getID().equals("Metal")) {
      UIManager.put("microba.MarkerBarUI", packagePrefix + "marker.ui.metal.MetalMarkerBarUI");
      UIManager.put(packagePrefix + "marker.ui.metal.MetalMarkerBarUI", MetalMarkerBarUI.class);
    }
    else if (lookAndFeel.getID().equals("Motif")) {
      UIManager.put("microba.MarkerBarUI", packagePrefix + "marker.ui.motif.MotifMarkerBarUI");
      UIManager.put(packagePrefix + "marker.ui.motif.MotifMarkerBarUI", MotifMarkerBarUI.class);
    }
  }

  private static final class UIChangeListener implements
      PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent event) {
      if ("lookAndFeel".equals(event.getPropertyName())) {
        setLookAndFeelProperties((LookAndFeel) event.getNewValue());
      }
    }
  }

  private static Map lookAndFeelToOverride = new HashMap();

  /**
   * Sets per-Lokk&Feel map of color overrides.
   * 
   * 
   * @param lookAndFeel
   *            look&feel ID
   * @param overrides
   *            keys in the map are {@link String} constants, valuse are of
   *            type {@link Color} or of type {@link String} (in this case,
   *            {@link Color} values are obtained via
   *            {@link UIManager#getColor(Object)}). May be <code>null</code>.
   */
  public static void setColorOverrideMap(String lookAndFeel, Map overrides) {
    lookAndFeelToOverride.put(lookAndFeel, overrides);
    // TODO: refresh ui delegates
  }

  /**
   * Returns overriden color for given component in current Look&Feel. The
   * algorithms is:
   * <ul>
   * <li>If the component overrides the constant (per-instance override),
   * then it is returned.
   * <li>If the library overrides the constant (per-Look&Feel override), then
   * it is returned.
   * <li>Else <code>null</code> is returned.
   * </ul>
   * This method is actually intended to be used by UI delegates of the
   * library.
   * 
   * @param colorConstant
   *            color constant
   * @param component
   *            component of the library
   * @return overriden color or <code>null</code> if not overriden
   */
  public static synchronized Color getOverridenColor(String colorConstant,
      MicrobaComponent component) {

    Map componentOverrideMap = component.getColorOverrideMap();
    if (componentOverrideMap != null) {
      if (componentOverrideMap.containsKey(colorConstant)) {
        Object val = componentOverrideMap.get(colorConstant);
        if (val instanceof Color)
          return (Color) val;
        else
          return UIManager.getColor(val);
      }
    }

    String currentLookAndFeel = UIManager.getLookAndFeel().getID();
    Map overrides = (Map) lookAndFeelToOverride.get(currentLookAndFeel);
    if (overrides != null) {
      if (overrides.containsKey(colorConstant)) {
        Object val = overrides.get(colorConstant);
        if (val instanceof Color)
          return (Color) val;
        else
          return UIManager.getColor(val);

      }
    }

    return null;
  }

  /**
   * Returns overriden color for given component in current Look&Feel or a
   * default value. The algorithms is:
   * <ul>
   * <li>If the component overrides the constant (per-instance override),
   * then it is returned.
   * <li>If the library overrides the constant (per-Look&Feel override), then
   * it is returned.
   * <li>Else defaultColor is returned.
   * </ul>
   * This method is actually intended to be used by UI delegates of the
   * library.
   * 
   * @param colorConstant
   *            color constant
   * @param component
   *            component of the library
   * @param defaultColor
   * @return overriden color or defaultColor if not overriden
   */
  public static synchronized Color getOverridenColor(String colorConstant,
      MicrobaComponent component, Color defaultColor) {
    Color overriden = getOverridenColor(colorConstant, component);
    if (overriden != null)
      return overriden;
    else
      return defaultColor;
  }

}
