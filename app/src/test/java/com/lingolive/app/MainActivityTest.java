package com.lingolive.app;

import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 26)
public class MainActivityTest {
    @Test public void launchesOnMinimumAndroidAndSupportsHindiInBothDirections() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            List<View> views = new ArrayList<>();
            collect(activity.getWindow().getDecorView(), views);
            List<Spinner> pickers = new ArrayList<>();
            Button swap = null;
            for (View view : views) {
                if (view instanceof Spinner) pickers.add((Spinner)view);
                if (view instanceof Button && "⇄".contentEquals(((Button)view).getText())) swap = (Button)view;
            }
            assertEquals(2, pickers.size());
            assertNotNull(swap);
            Spinner source = pickers.get(0), target = pickers.get(1);
            assertEquals(8, target.getCount());
            target.setSelection(7);
            shadowOf(Looper.getMainLooper()).idle();
            assertEquals("हिन्दी", target.getSelectedItem());
            swap.performClick();
            shadowOf(Looper.getMainLooper()).idle();
            assertEquals("हिन्दी", source.getSelectedItem());
            assertEquals("中文", target.getSelectedItem());
        }
    }

    private void collect(View view, List<View> result) {
        result.add(view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) collect(group.getChildAt(i), result);
        }
    }
}
