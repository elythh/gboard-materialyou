package dev.materialyou.gboardtheme;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Launcher entry. Besides showing setup help, it makes the module visible to managers (such as
 * NPatch) that can only list apps with a launcher icon when the full app list is restricted.
 */
public final class InfoActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int padding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        TextView text = new TextView(this);
        text.setText(R.string.info_text);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        text.setPadding(padding, padding, padding, padding);
        text.setTextIsSelectable(true);
        ScrollView scroll = new ScrollView(this);
        scroll.setFitsSystemWindows(true);
        scroll.addView(text);
        setContentView(scroll);
    }
}
