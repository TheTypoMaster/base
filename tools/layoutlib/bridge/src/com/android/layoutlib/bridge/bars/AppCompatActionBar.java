/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.layoutlib.bridge.bars;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.api.RenderResources;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.SessionParams;
import com.android.layoutlib.bridge.android.BridgeContext;
import com.android.layoutlib.bridge.impl.ResourceHelper;
import com.android.resources.ResourceType;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Assumes that the AppCompat library is present in the project's classpath and creates an
 * actionbar around it.
 */
public class AppCompatActionBar extends BridgeActionBar {

    private Object mWindowDecorActionBar;
    private static final String WINDOW_ACTION_BAR_CLASS = "android.support.v7.internal.app.WindowDecorActionBar";
    private Class<?> mWindowActionBarClass;

    /**
     * Inflate the action bar and attach it to {@code parentView}
     */
    public AppCompatActionBar(@NonNull BridgeContext context, @NonNull SessionParams params,
            @NonNull ViewGroup parentView) {
        super(context, params, parentView);
        int contentRootId = context.getProjectResourceValue(ResourceType.ID,
                "action_bar_activity_content", 0);
        View contentView = getDecorContent().findViewById(contentRootId);
        if (contentView != null) {
            assert contentView instanceof FrameLayout;
            setContentRoot(((FrameLayout) contentView));
        } else {
            // Something went wrong. Create a new FrameLayout in the enclosing layout.
            FrameLayout contentRoot = new FrameLayout(context);
            setMatchParent(contentRoot);
            mEnclosingLayout.addView(contentRoot);
            setContentRoot(contentRoot);
        }
        try {
            Class[] constructorParams = {View.class};
            Object[] constructorArgs = {getDecorContent()};
            mWindowDecorActionBar = params.getProjectCallback().loadView(WINDOW_ACTION_BAR_CLASS,
                    constructorParams, constructorArgs);

            mWindowActionBarClass = mWindowDecorActionBar == null ? null :
                    mWindowDecorActionBar.getClass();
            setupActionBar();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected ResourceValue getLayoutResource(BridgeContext context) {
        // We always assume that the app has requested the action bar.
        return context.getRenderResources().getProjectResource(ResourceType.LAYOUT,
                "abc_screen_toolbar");
    }

    @Override
    protected void setTitle(CharSequence title) {
        if (title != null && mWindowDecorActionBar != null) {
            Method setTitle = getMethod(mWindowActionBarClass, "setTitle", CharSequence.class);
            invoke(setTitle, mWindowDecorActionBar, title);
        }
    }

    @Override
    protected void setSubtitle(CharSequence subtitle) {
        if (subtitle != null && mWindowDecorActionBar != null) {
            Method setSubtitle = getMethod(mWindowActionBarClass, "setSubtitle", CharSequence.class);
            invoke(setSubtitle, mWindowDecorActionBar, subtitle);
        }
    }

    @Override
    protected void setIcon(String icon) {
        // Do this only if the action bar doesn't already have an icon.
        if (icon != null && !icon.isEmpty() && mWindowDecorActionBar != null) {
            if (((Boolean) invoke(getMethod(mWindowActionBarClass, "hasIcon"), mWindowDecorActionBar)
            )) {
                Drawable iconDrawable = getDrawable(icon, false);
                if (iconDrawable != null) {
                    Method setIcon = getMethod(mWindowActionBarClass, "setIcon", Drawable.class);
                    invoke(setIcon, mWindowDecorActionBar, iconDrawable);
                }
            }
        }
    }

    @Override
    protected void setHomeAsUp(boolean homeAsUp) {
        if (mWindowDecorActionBar != null) {
            Method setHomeAsUp = getMethod(mWindowActionBarClass,
                    "setDefaultDisplayHomeAsUpEnabled", boolean.class);
            invoke(setHomeAsUp, mWindowDecorActionBar, homeAsUp);
        }
    }

    @Override
    public void createMenuPopup() {
        // it's hard to addd menus to appcompat's actionbar, since it'll use a lot of reflection.
        // so we skip it for now.
    }

    @Nullable
    private static Method getMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            return owner == null ? null : owner.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    private static Object invoke(Method method, Object owner, Object... args) {
        try {
            return method == null ? null : method.invoke(owner, args);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    // TODO: this is duplicated from FrameworkActionBarWrapper$WindowActionBarWrapper
    @Nullable
    private Drawable getDrawable(@NonNull String name, boolean isFramework) {
        RenderResources res = mBridgeContext.getRenderResources();
        ResourceValue value = res.findResValue(name, isFramework);
        value = res.resolveResValue(value);
        if (value != null) {
            return ResourceHelper.getDrawable(value, mBridgeContext);
        }
        return null;
    }
}
