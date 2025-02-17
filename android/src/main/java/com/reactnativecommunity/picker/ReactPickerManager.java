/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reactnativecommunity.picker;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.*;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.EventDispatcher;

import javax.annotation.Nullable;

/**
 * {@link ViewManager} for the {@link ReactPicker} view. This is abstract because the
 * {@link Spinner} doesn't support setting the mode (dropdown/dialog) outside the constructor, so
 * that is delegated to the separate {@link ReactDropdownPickerManager} and
 * {@link ReactDialogPickerManager} components. These are merged back on the JS side into one
 * React component.
 */
public abstract class ReactPickerManager extends BaseViewManager<ReactPicker, ReactPickerShadowNode> {
  private static final ReadableArray EMPTY_ARRAY = Arguments.createArray();

  @ReactProp(name = "items")
  public void setItems(ReactPicker view, @Nullable ReadableArray items) {
    ReactPickerAdapter adapter = (ReactPickerAdapter) view.getAdapter();

    if (adapter == null) {
      adapter = new ReactPickerAdapter(view.getContext(), items);
      adapter.setPrimaryTextColor(view.getPrimaryColor());
      view.setAdapter(adapter);
    } else {
      adapter.setItems(items);
    }
  }

  @ReactProp(name = ViewProps.COLOR, customType = "Color")
  public void setColor(ReactPicker view, @Nullable Integer color) {
    view.setPrimaryColor(color);
    ReactPickerAdapter adapter = (ReactPickerAdapter) view.getAdapter();
    if (adapter != null) {
      adapter.setPrimaryTextColor(color);
    }
  }

  @ReactProp(name = "prompt")
  public void setPrompt(ReactPicker view, @Nullable String prompt) {
    view.setPrompt(prompt);
  }

  @ReactProp(name = ViewProps.ENABLED, defaultBoolean = true)
  public void setEnabled(ReactPicker view, boolean enabled) {
    view.setEnabled(enabled);
  }

  @ReactProp(name = "selected")
  public void setSelected(ReactPicker view, int selected) {
    view.setStagedSelection(selected);
  }

  @ReactProp(name = "dropdownIconColor")
  public void setDropdownIconColor(ReactPicker view, @Nullable int color) {
    view.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
  }

  @ReactProp(name = ViewProps.NUMBER_OF_LINES, defaultInt = 1)
  public void setNumberOfLines(ReactPicker view, int numberOfLines) {
    ReactPickerAdapter adapter = (ReactPickerAdapter) view.getAdapter();
    if (adapter == null) {
      adapter = new ReactPickerAdapter(view.getContext(), EMPTY_ARRAY);
      adapter.setPrimaryTextColor(view.getPrimaryColor());
      adapter.setNumberOfLines(numberOfLines);
      view.setAdapter(adapter);
    } else {
      adapter.setNumberOfLines(numberOfLines);
    }
  }

  @Override
  protected void onAfterUpdateTransaction(ReactPicker view) {
    super.onAfterUpdateTransaction(view);
    view.updateStagedSelection();
  }

  @Override
  protected void addEventEmitters(
      final ThemedReactContext reactContext,
      final ReactPicker picker) {
    picker.setOnSelectListener(
            new PickerEventEmitter(
                    picker,
                    reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher()));
  }

  @Override
  public ReactPickerShadowNode createShadowNodeInstance() {
    return new ReactPickerShadowNode();
  }

  @Override
  public Class<? extends ReactPickerShadowNode> getShadowNodeClass() {
    return ReactPickerShadowNode.class;
  }

  @Override
  public void updateExtraData(ReactPicker root, Object extraData) {
  }

  private static class ReactPickerAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private int mNumberOfLines = 1;
    private @Nullable Integer mPrimaryTextColor;
    private @Nullable ReadableArray mItems;

    public ReactPickerAdapter(Context context, @Nullable ReadableArray items) {
      super();

      mItems = items;
      mInflater = (LayoutInflater) Assertions.assertNotNull(
          context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
    }

    public void setItems(@Nullable ReadableArray items) {
      mItems = items;
      notifyDataSetChanged();
    }

    @Override
    public int getCount() {
      if (mItems == null) return 0;
      return mItems.size();
    }

    @Override
    public ReadableMap getItem(int position) {
      if (mItems == null) return null;
      return mItems.getMap(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      return getView(position, convertView, parent, false);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
      return getView(position, convertView, parent, true);
    }

    private View getView(int position, View convertView, ViewGroup parent, boolean isDropdown) {
      ReadableMap item = getItem(position);
      @Nullable ReadableMap style = null;
      boolean enabled = true;

      if (item.hasKey("style")) {
        style = item.getMap("style");
      }

      if (convertView == null) {
        int layoutResId = isDropdown
              ? R.layout.simple_spinner_dropdown_item
              : R.layout.simple_spinner_item;
        convertView = mInflater.inflate(layoutResId, parent, false);
      }

      if (item.hasKey("enabled")) {
        enabled = item.getBoolean("enabled");
      }

      convertView.setEnabled(enabled);
      // Seems counter intuitive, but this makes the specific item not clickable when enable={false}
      convertView.setClickable(!enabled);
      
      final TextView textView = (TextView) convertView;
      textView.setText(item.getString("label"));
      textView.setMaxLines(mNumberOfLines);

      if (style != null) {
        if (style.hasKey("backgroundColor") && !style.isNull("backgroundColor")) {
          convertView.setBackgroundColor(style.getInt("backgroundColor"));
        }
        
        if (style.hasKey("color") && !style.isNull("color")) {
          textView.setTextColor(style.getInt("color"));
        }

        if (style.hasKey("fontSize") && !style.isNull("fontSize")) {
          textView.setTextSize((float)style.getDouble("fontSize"));
        }
        
        if (style.hasKey("fontFamily") && !style.isNull("fontFamily")) {
          Typeface face = Typeface.create(style.getString("fontFamily"), Typeface.NORMAL);
          textView.setTypeface(face);
        }
      }

      if (!isDropdown && mPrimaryTextColor != null) {
        textView.setTextColor(mPrimaryTextColor);
      } else if (item.hasKey("color") && !item.isNull("color")) {
        textView.setTextColor(item.getInt("color"));
      }

      if (item.hasKey("fontFamily") && !item.isNull("fontFamily")) {
        Typeface face = Typeface.create(item.getString("fontFamily"), Typeface.NORMAL);
        // Typeface face = Typeface.create("MuseoSans-500", Typeface.NORMAL);
        textView.setTypeface(face);
      }

      return convertView;
    }

    public void setPrimaryTextColor(@Nullable Integer primaryTextColor) {
      mPrimaryTextColor = primaryTextColor;
      notifyDataSetChanged();
    }

    public void setNumberOfLines(int numberOfLines) {
      mNumberOfLines = numberOfLines;
      notifyDataSetChanged();
    }
  }

  private static class PickerEventEmitter implements ReactPicker.OnSelectListener {

    private final ReactPicker mReactPicker;
    private final EventDispatcher mEventDispatcher;

    public PickerEventEmitter(ReactPicker reactPicker, EventDispatcher eventDispatcher) {
      mReactPicker = reactPicker;
      mEventDispatcher = eventDispatcher;
    }

    @Override
    public void onItemSelected(int position) {
      mEventDispatcher.dispatchEvent( new PickerItemSelectEvent(
              mReactPicker.getId(), position));
    }
  }
}
