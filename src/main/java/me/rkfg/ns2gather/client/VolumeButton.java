package me.rkfg.ns2gather.client;

import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.kiouri.sliderbar.client.event.BarValueChangedEvent;
import com.kiouri.sliderbar.client.event.BarValueChangedHandler;
import com.kiouri.sliderbar.client.solution.adv.AdvancedSliderBar;
import com.kiouri.sliderbar.client.view.SliderBar;

public abstract class VolumeButton extends HorizontalPanel {
    private static final String SOUND_ON_ICON = "icons/sound.png";
    private static final String SOUND_OFF_ICON = "icons/mute.png";
    private boolean mute = false;
    private final Image muteIcon = new Image();
    private final SliderBar sliderBar_chatVolume = new AdvancedSliderBar();
    private int volume = 0;
    private final CookieSettingsManager cookieSettingsManager;

    public VolumeButton(final CookieSettingsManager cookieSettingsManager) {
        this.cookieSettingsManager = cookieSettingsManager;
        mute = cookieSettingsManager.getBooleanCookie(CookieSettingsManager.CHAT_MUTE_COOKIE, false);
        volume = cookieSettingsManager.getLongCookie(CookieSettingsManager.CHAT_VOLUME_COOKIE, 170L).intValue();
        muteIcon.getElement().getStyle().setCursor(Cursor.POINTER);
        muteIcon.addClickHandler(new VolumeButtonClickHandler());
        updateAppearance(true);
        sliderBar_chatVolume.setWidth("200px");
        sliderBar_chatVolume.setMaxValue(200);
        sliderBar_chatVolume.addBarValueChangedHandler(new BarValueChangedHandler() {

            @Override
            public void onBarValueChanged(BarValueChangedEvent event) {
                if (event.getValue() != 0 || !mute) {
                    volume = event.getValue();
                    cookieSettingsManager.setLongCookie(CookieSettingsManager.CHAT_VOLUME_COOKIE, (long) volume);
                    if (mute) {
                        mute = false;
                        updateAppearance(false);
                    }
                }
                if (event.getValue() == 0 && !mute) {
                    mute = true;
                    updateAppearance(false);
                }
                volumeChanged(event.getValue());
            }
        });
        add(sliderBar_chatVolume);
        add(muteIcon);
    }

    private class VolumeButtonClickHandler implements ClickHandler {

        @Override
        public void onClick(ClickEvent event) {
            mute = !mute;
            cookieSettingsManager.setBooleanCookie(CookieSettingsManager.CHAT_MUTE_COOKIE, mute);
            updateAppearance(true);
        }
    }

    public void setVolume(int newVolume) {
        sliderBar_chatVolume.setValue(newVolume);
    }

    public void updateAppearance(boolean setVolume) {
        muteIcon.setUrl(mute ? SOUND_OFF_ICON : SOUND_ON_ICON);
        if (setVolume) {
            if (mute) {
                setVolume(0);
            } else {
                setVolume(volume);
            }
        }
    }

    public abstract void volumeChanged(int newVolume);

}
