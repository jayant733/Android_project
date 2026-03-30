package com.example.tripsync.ui.common;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public final class EdgeToEdgeHelper {

    private EdgeToEdgeHelper() {
    }

    public static void apply(Activity activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);

        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) {
            return;
        }

        View root = content.getChildAt(0);
        int startPadding = root.getPaddingStart();
        int topPadding = root.getPaddingTop();
        int endPadding = root.getPaddingEnd();
        int bottomPadding = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );

            view.setPadding(
                    startPadding + insets.left,
                    topPadding + insets.top,
                    endPadding + insets.right,
                    bottomPadding + insets.bottom
            );
            return windowInsets;
        });

        ViewCompat.requestApplyInsets(root);
    }
}
