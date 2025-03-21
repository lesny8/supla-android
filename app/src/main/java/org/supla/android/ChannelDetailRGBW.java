package org.supla.android;

/*
Copyright (C) AC SOFTWARE SP. Z O.O.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.res.ResourcesCompat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import org.supla.android.db.Channel;
import org.supla.android.db.ChannelBase;
import org.supla.android.db.ChannelGroup;
import org.supla.android.db.ColorListItem;
import org.supla.android.lib.SuplaClient;
import org.supla.android.lib.SuplaConst;
import org.supla.android.listview.DetailLayout;

/**
 * @noinspection SequencedCollectionMethodCanBeUsed
 */
public class ChannelDetailRGBW extends DetailLayout
    implements View.OnClickListener,
        SuplaColorBrightnessPicker.OnColorBrightnessChangeListener,
        SuplaColorListPicker.OnColorListTouchListener {

  private static final long MIN_REMOTE_UPDATE_PERIOD = 250;
  private static final long MIN_UPDATE_DELAY = 3000;
  private SuplaColorBrightnessPicker cbPicker;
  private SuplaColorListPicker clPicker;
  private Button tabRGB;
  private Button tabDimmer;
  private Button tabWheel;
  private Button tabSlider;
  private ViewGroup tabs;
  private ViewGroup pickerTypeTabs;
  private ViewGroup llExtraButtons;
  private EditText percentageValue;
  private AppCompatImageButton btnSettings;
  private AppCompatImageButton btnInfo;
  private RelativeLayout rlMain;
  private DimmerCalibrationTool dimmerCalibrationTool = null;
  private long remoteUpdateTime;
  private long changeFinishedTime;
  private Timer delayTimer1;
  private Timer delayTimer2;
  private Handler uiHandler;
  private SuplaChannelStatus status;
  private int lastColor;
  private int lastColorBrightness;
  private int lastBrightness;
  private AppCompatImageButton btnPowerOnOff;
  private Boolean varilight;
  private Boolean zamel;
  private Boolean comelit;

  public ChannelDetailRGBW(Context context) {
    super(context);
  }

  public ChannelDetailRGBW(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ChannelDetailRGBW(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public ChannelDetailRGBW(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  protected void init() {
    super.init();

    uiHandler = new Handler();

    tabs = findViewById(R.id.llTabs);
    pickerTypeTabs = findViewById(R.id.llPickerTypeTabs);

    status = findViewById(R.id.rgbwstatus);
    status.setOnlineColor(ResourcesCompat.getColor(getResources(), R.color.primary, null));
    status.setOfflineColor(ResourcesCompat.getColor(getResources(), R.color.red, null));

    clPicker = findViewById(R.id.clPicker);
    clPicker.addItem(Color.WHITE, (short) 100);
    clPicker.addItem();
    clPicker.addItem();
    clPicker.addItem();
    clPicker.addItem();
    clPicker.addItem();
    clPicker.setOnTouchListener(this);

    cbPicker = findViewById(R.id.cbPicker);
    cbPicker.setOnChangeListener(this);

    tabRGB = findViewById(R.id.rgbTabBtn_RGB);
    tabDimmer = findViewById(R.id.rgbTabBtn_Dimmer);
    tabWheel = findViewById(R.id.rgbTabBtn_Wheel);
    tabSlider = findViewById(R.id.rgbTabBtn_Slider);

    tabRGB.setOnClickListener(this);
    tabDimmer.setOnClickListener(this);
    tabWheel.setOnClickListener(this);
    tabSlider.setOnClickListener(this);

    llExtraButtons = findViewById(R.id.llExtraButtons);
    llExtraButtons.setVisibility(GONE);

    btnInfo = findViewById(R.id.rgbwBtnInfo);
    btnSettings = findViewById(R.id.rgbwBtnSettings);
    btnInfo.setOnClickListener(this);
    btnSettings.setOnClickListener(this);

    rlMain = findViewById(R.id.rlRgbwMain);
    rlMain.setVisibility(VISIBLE);

    btnPowerOnOff = findViewById(R.id.rgbwBtnPowerOnOff);
    btnPowerOnOff.setOnClickListener(this);

    percentageValue = findViewById(R.id.percentageValue);

    remoteUpdateTime = 0;
    changeFinishedTime = 0;
    delayTimer1 = null;
    delayTimer2 = null;
  }

  private void showRGB() {
    cbPicker.setColorWheelVisible(true);
    cbPicker.setSliderVisible(false);
    clPicker.setVisibility(View.VISIBLE);
    pickerTypeTabs.setVisibility(GONE);
    llExtraButtons.setVisibility(GONE);
    btnPowerOnOff.setVisibility(GONE);

    channelDataToViews();
  }

  private void showDimmer() {

    cbPicker.setColorWheelVisible(false);
    clPicker.setVisibility(View.GONE);
    pickerTypeTabs.setVisibility(VISIBLE);

    varilight = false;
    zamel = false;
    comelit = false;

    if (getChannelBase() instanceof Channel c) {
      if (c.getManufacturerID() == SuplaConst.SUPLA_MFR_DOYLETRATT && c.getProductID() == 1) {
        varilight = true;
        if (dimmerCalibrationTool == null
            || !(dimmerCalibrationTool instanceof VLCalibrationTool)) {
          dimmerCalibrationTool = new VLCalibrationTool(this);
        }
      } else if (c.getManufacturerID() == SuplaConst.SUPLA_MFR_ZAMEL) {
        zamel = true;
        if ((c.getProductID() == SuplaConst.ZAM_PRODID_DIW_01)
            && (dimmerCalibrationTool == null
                || !(dimmerCalibrationTool instanceof VLCalibrationTool))) {
          dimmerCalibrationTool = new DiwCalibrationTool(this);
        }
      } else if (c.getManufacturerID() == SuplaConst.SUPLA_MFR_COMELIT) {
        comelit = true;
        if ((c.getProductID() == SuplaConst.COM_PRODID_WDIM100)
            && (dimmerCalibrationTool == null
                || !(dimmerCalibrationTool instanceof VLCalibrationTool))) {
          dimmerCalibrationTool = new DiwCalibrationTool(this);
        }
      }
    }

    llExtraButtons.setVisibility(dimmerCalibrationTool == null ? GONE : VISIBLE);

    Preferences prefs = new Preferences(getContext());

    Boolean typeSlider = prefs.isBrightnessPickerTypeSlider();
    if (typeSlider == null) {
      typeSlider = varilight;
    }

    cbPicker.setMinBrightness(varilight || zamel || comelit ? 1f : 0f);

    onClick(typeSlider ? tabSlider : tabWheel);
    channelDataToViews();
  }

  @Override
  public void onDetailHide() {
    super.onDetailHide();

    if (dimmerCalibrationTool != null) {
      dimmerCalibrationTool.Hide();
      dimmerCalibrationTool = null;
    }
  }

  public void onDetailShow() {
    if (dimmerCalibrationTool != null) {
      dimmerCalibrationTool.Hide();
    }
    rlMain.setVisibility(VISIBLE);
  }

  private void hideDimmerConfigurationToolIfNotLocked() {
    if (dimmerCalibrationTool != null && dimmerCalibrationTool.isExitUnlocked()) {
      dimmerCalibrationTool.Hide();
    }
  }

  public void setData(ChannelBase channel) {

    super.setData(channel);
    llExtraButtons.setVisibility(GONE);
    pickerTypeTabs.setVisibility(GONE);
    percentageValue.setVisibility(GONE);

    varilight = false;
    zamel = false;
    comelit = false;

    hideDimmerConfigurationToolIfNotLocked();

    switch (channel.getFunc()) {
      case SuplaConst.SUPLA_CHANNELFNC_DIMMER:
        showDimmer();
        tabs.setVisibility(View.GONE);
        break;

      case SuplaConst.SUPLA_CHANNELFNC_RGBLIGHTING:
        showRGB();
        tabs.setVisibility(View.GONE);
        break;

      case SuplaConst.SUPLA_CHANNELFNC_DIMMERANDRGBLIGHTING:
        cbPicker.setColorWheelVisible(true);

        onClick(tabRGB);
        tabs.setVisibility(View.VISIBLE);

        break;
    }

    channelDataToViews();
  }

  private void channelDataToViews() {

    cbPicker.setColorMarkers(null);
    cbPicker.setBrightnessMarkers(null);

    if (isGroup()) {
      ChannelGroup cgroup = (ChannelGroup) getChannelFromDatabase();

      status.setVisibility(View.VISIBLE);
      status.setPercent(cgroup.getOnLinePercent());

      ArrayList<Double> markers;

      markers =
          cbPicker.isColorWheelVisible() ? cgroup.getColorBrightness() : cgroup.getBrightness();

      if (markers != null) {
        if (markers.size() == 1) {
          if (markers.get(0).intValue() != (int) cbPicker.getBrightnessValue()) {
            cbPicker.setBrightnessValue(markers.get(0));
          }
        } else {
          cbPicker.setBrightnessMarkers(markers);
        }
      }

      if (cbPicker.isColorWheelVisible()) {

        markers = cgroup.getColors();

        if (markers != null) {
          if (markers.size() == 1) {
            if (markers.get(0).intValue() != cbPicker.getColor()) {
              cbPicker.setColor(markers.get(0).intValue());
            }
          } else {
            cbPicker.setColorMarkers(markers);
          }
        }
      }

    } else {
      Channel channel = (Channel) getChannelFromDatabase();

      status.setVisibility(View.GONE);
      percentageValue.setVisibility(VISIBLE);

      if (cbPicker.isColorWheelVisible()
          && (int) cbPicker.getBrightnessValue() != (int) channel.getColorBrightness()) {
        cbPicker.setBrightnessValue(channel.getColorBrightness());

      } else if (!cbPicker.isColorWheelVisible()
          && (int) cbPicker.getBrightnessValue() != (int) channel.getBrightness()) {
        cbPicker.setBrightnessValue(channel.getBrightness());
      }

      if (cbPicker.isColorWheelVisible()) {
        cbPicker.setColor(channel.getColor());
        percentageValue.setText(
            String.format(
                Locale.getDefault(), "%d%%", Byte.toUnsignedInt(channel.getColorBrightness())));
      } else {
        percentageValue.setText(
            String.format(
                Locale.getDefault(), "%d%%", Byte.toUnsignedInt(channel.getBrightness())));
      }
    }

    for (int a = 1; a < 6; a++) {
      ColorListItem cli = DBH.getColorListItem(getRemoteId(), isGroup(), a);

      if (cli != null) {
        clPicker.setItemColor(a, cli.getColor());
        clPicker.setItemPercent(a, cli.getBrightness());
      } else {
        clPicker.setItemColor(a, Color.TRANSPARENT);
        clPicker.setItemPercent(a, (short) 0);
      }
    }

    pickerToUI();
  }

  @Override
  public View inflateContentView() {
    return inflateLayout(R.layout.detail_rgbw);
  }

  private void setBtnBackground(Button btn, int id) {
    Drawable d = id == 0 ? null : ResourcesCompat.getDrawable(getResources(), id, null);
    btn.setBackground(d);
  }

  private void setPowerBtnOn(boolean on) {
    cbPicker.setPowerButtonOn(on);
    int color =
        ResourcesCompat.getColor(getResources(), on ? R.color.primary : R.color.error, null);
    btnPowerOnOff.setImageTintList(ColorStateList.valueOf(color));
  }

  private boolean isAnyOn() {
    ArrayList<Double> markers = cbPicker.getBrightnessMarkers();
    if (markers != null) {
      for (Double marker : markers) {
        if (marker > 0) {
          return true;
        }
      }
    }

    return false;
  }

  @SuppressLint("SetTextI18n")
  private void pickerToUI() {

    lastColor = cbPicker.getColor();
    int brightness = (int) cbPicker.getBrightnessValue();

    setPowerBtnOn(brightness > 0 || isAnyOn());

    if (cbPicker.isColorWheelVisible()) {
      lastColorBrightness = brightness;
    } else {
      lastBrightness = brightness;
    }
  }

  @Override
  public boolean onBackPressed() {
    if (dimmerCalibrationTool != null && dimmerCalibrationTool.isVisible()) {
      return dimmerCalibrationTool.onBackPressed();
    }
    return true;
  }

  public boolean detailWillHide(boolean offlineReason) {
    if (super.detailWillHide(offlineReason)) {
      return !offlineReason
          || dimmerCalibrationTool == null
          || !dimmerCalibrationTool.isVisible()
          || dimmerCalibrationTool.isExitUnlocked();
    }
    return false;
  }

  private void sendNewValues(boolean force, boolean turnOnOff) {

    if (delayTimer1 != null) {
      delayTimer1.cancel();
      delayTimer1 = null;
    }

    SuplaClient client = SuplaApp.getApp().getSuplaClient();

    if (client == null || (!isDetailVisible() && !force)) {
      return;
    }

    if ((turnOnOff || System.currentTimeMillis() - remoteUpdateTime >= MIN_REMOTE_UPDATE_PERIOD)
        && client.setRGBW(
            getRemoteId(), isGroup(), lastColor, lastColorBrightness, lastBrightness, turnOnOff)) {
      remoteUpdateTime = System.currentTimeMillis();
      refreshViewsWithDelay(MIN_UPDATE_DELAY);
    } else {

      long delayTime = 1;

      if (System.currentTimeMillis() - remoteUpdateTime < MIN_REMOTE_UPDATE_PERIOD) {
        delayTime = MIN_REMOTE_UPDATE_PERIOD - (System.currentTimeMillis() - remoteUpdateTime) + 1;
      }

      delayTimer1 = new Timer();

      delayTimer1.schedule(
          new TimerTask() {
            @Override
            public void run() {
              uiHandler.post(() -> sendNewValues());
            }
          },
          delayTime,
          1000);
    }
  }

  private void sendNewValues() {
    sendNewValues(false, false);
  }

  private void showInformationDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    ViewGroup viewGroup = findViewById(android.R.id.content);
    View dialogView =
        LayoutInflater.from(getContext()).inflate(R.layout.dimmer_info, viewGroup, false);
    builder.setView(dialogView);
    final AlertDialog alertDialog = builder.create();

    dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> alertDialog.dismiss());

    alertDialog.show();
  }

  @Override
  public void onClick(View v) {
    int onBackgroundColor = ResourcesCompat.getColor(getResources(), R.color.on_background, null);
    int onPrimaryColor = ResourcesCompat.getColor(getResources(), R.color.on_primary, null);
    if (v == tabRGB) {
      showRGB();

      setBtnBackground(tabRGB, R.drawable.rounded_sel_btn);
      setBtnBackground(tabDimmer, 0);

      tabRGB.setTextColor(onPrimaryColor);
      tabDimmer.setTextColor(onBackgroundColor);
    } else if (v == tabDimmer) {
      showDimmer();

      setBtnBackground(tabDimmer, R.drawable.rounded_sel_btn);
      setBtnBackground(tabRGB, 0);

      tabRGB.setTextColor(onBackgroundColor);
      tabDimmer.setTextColor(onPrimaryColor);
    } else if (v == tabWheel) {
      setBtnBackground(tabWheel, R.drawable.rounded_sel_btn);
      setBtnBackground(tabSlider, 0);

      tabWheel.setTextColor(onPrimaryColor);
      tabSlider.setTextColor(onBackgroundColor);
      btnPowerOnOff.setVisibility(GONE);

    } else if (v == tabSlider) {
      setBtnBackground(tabWheel, 0);
      setBtnBackground(tabSlider, R.drawable.rounded_sel_btn);

      tabWheel.setTextColor(onBackgroundColor);
      tabSlider.setTextColor(onPrimaryColor);
      btnPowerOnOff.setVisibility(VISIBLE);
    } else if (v == btnSettings && dimmerCalibrationTool != null) {
      dimmerCalibrationTool.Show();
    } else if (v == btnPowerOnOff) {
      cbPicker.setPowerButtonOn(!cbPicker.isPowerButtonOn());
      onPowerButtonClick(cbPicker);
    } else if (v == btnInfo) {
      if (dimmerCalibrationTool != null) {
        showInformationDialog();
      }
    }

    if (v == tabDimmer || v == tabRGB) {
      channelDataToViews();
    }

    if (v == tabWheel || v == tabSlider) {
      cbPicker.setSliderVisible(v == tabSlider);

      Preferences prefs = new Preferences(getContext());
      prefs.setBrightnessPickerTypeToSlider(cbPicker.isSliderVisible());
    }
  }

  @Override
  public void onColorChanged(SuplaColorBrightnessPicker scbPicker, int color) {
    pickerToUI();
    sendNewValues();
  }

  @Override
  public void onBrightnessChanged(SuplaColorBrightnessPicker scbPicker, double brightness) {
    pickerToUI();
    sendNewValues();
    percentageValue.setText(String.format(Locale.getDefault(), "%d%%", Math.round(brightness)));
  }

  @Override
  public void onPowerButtonClick(SuplaColorBrightnessPicker scbPicker) {
    scbPicker.setBrightnessValue(scbPicker.isPowerButtonOn() ? 100 : 0);
    pickerToUI();
    sendNewValues(true, true);
  }

  private void refreshViewsWithDelay(long delayTime) {

    if (delayTimer2 != null) {
      delayTimer2.cancel();
      delayTimer2 = null;
    }

    if (!isDetailVisible() || cbPicker.isMoving()) {
      return;
    }

    if (delayTime == 0 && System.currentTimeMillis() - changeFinishedTime >= MIN_UPDATE_DELAY) {

      channelDataToViews();

    } else {

      if (delayTime == 0) {
        delayTime = 1;

        if (System.currentTimeMillis() - changeFinishedTime < MIN_UPDATE_DELAY) {
          delayTime = MIN_UPDATE_DELAY - (System.currentTimeMillis() - changeFinishedTime) + 1;
        }
      }

      delayTimer2 = new Timer();
      delayTimer2.schedule(
          new TimerTask() {
            @Override
            public void run() {
              uiHandler.post(() -> refreshViewsWithDelay());
            }
          },
          delayTime,
          1000);
    }
  }

  private void refreshViewsWithDelay() {
    refreshViewsWithDelay(0);
  }

  @Override
  public void onChangeFinished(SuplaColorBrightnessPicker scbPicker) {
    changeFinishedTime = System.currentTimeMillis();
    refreshViewsWithDelay();
  }

  @Override
  public void OnChannelDataChanged() {
    refreshViewsWithDelay();
    hideDimmerConfigurationToolIfNotLocked();
  }

  @Override
  public void onColorTouched(SuplaColorListPicker sclPicker, int color, short percent) {

    if (color != Color.TRANSPARENT && cbPicker.isColorWheelVisible()) {
      cbPicker.setColor(color);
      cbPicker.setBrightnessValue(percent);

      onColorChanged(cbPicker, color);
    }
  }

  @Override
  public void onEdit(SuplaColorListPicker sclPicker, int idx) {

    if (idx > 0 && cbPicker.isColorWheelVisible()) {
      sclPicker.setItemColor(idx, cbPicker.getColor());
      sclPicker.setItemPercent(idx, (short) cbPicker.getBrightnessValue());

      if (getRemoteId() != 0) {

        ColorListItem cli = new ColorListItem();
        cli.setRemoteId(getRemoteId());
        cli.setGroup(isGroup());
        cli.setIdx(idx);
        cli.setColor(cbPicker.getColor());
        cli.setBrightness((short) cbPicker.getBrightnessValue());

        DBH.updateColorListItemValue(cli);
      }
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    if (dimmerCalibrationTool != null && dimmerCalibrationTool.isVisible()) {
      return false;
    }
    return super.onTouchEvent(ev);
  }
}
