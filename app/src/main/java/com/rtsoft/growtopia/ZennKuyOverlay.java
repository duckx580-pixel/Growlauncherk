package com.rtsoft.growtopia;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Floating "ZK" button overlay drawn over the game via mViewGroup.
 * Tap it to open the Google Login fix menu.
 */
public class ZennKuyOverlay {

    // Dark "mod menu" palette.
    private static final int C_BG        = Color.parseColor("#16191F");
    private static final int C_BORDER    = Color.parseColor("#2A3038");
    private static final int C_FIELD_BG  = Color.parseColor("#0E1013");
    private static final int C_FIELD_BRD = Color.parseColor("#333B47");
    private static final int C_TEXT      = Color.parseColor("#ECEFF1");
    private static final int C_MUTED     = Color.parseColor("#8A94A6");
    private static final int C_ACCENT    = Color.parseColor("#2ECC71");
    private static final int C_ACCENT_DK = Color.parseColor("#1E9E52");

    private final Context ctx;
    private View floatBtn;

    public ZennKuyOverlay(Context context) {
        this.ctx = context;
    }

    public void attachTo(ViewGroup parent) {
        floatBtn = makeToggleButton();
        // mViewGroup is a RelativeLayout (see SharedActivity), so this needs
        // RelativeLayout.LayoutParams + addRule — FrameLayout.LayoutParams'
        // gravity field is silently dropped when handed to a RelativeLayout.
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                dp(52), dp(52));
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        lp.addRule(RelativeLayout.ALIGN_PARENT_END);
        lp.topMargin   = dp(8);
        lp.rightMargin = dp(8);
        parent.addView(floatBtn, lp);
    }

    private View makeToggleButton() {
        Button btn = new Button(ctx);
        btn.setText("ZK");
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setTextColor(C_TEXT);
        btn.setPadding(0, 0, 0, 0);
        btn.setElevation(dp(4));

        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.argb(230, 18, 20, 24));
        circle.setStroke(dp(2), C_ACCENT);
        btn.setBackground(rippled(circle, C_ACCENT));

        btn.setOnClickListener(v -> showMenu());
        btn.setOnTouchListener(new DragListener(btn));
        return btn;
    }

    private void showMenu() {
        DeviceSpoofer ds = new DeviceSpoofer(ctx);

        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(roundedRect(C_BG, C_BORDER, 1, 16));

        // ── Header bar ───────────────────────────────────────────────────────────────────
        LinearLayout header = row();
        header.setPadding(dp(16), dp(14), dp(12), dp(14));
        TextView badge = label("ZK");
        badge.setTypeface(null, Typeface.BOLD);
        badge.setTextColor(Color.parseColor("#0E1013"));
        badge.setBackground(roundedRect(C_ACCENT, C_ACCENT, 0, 8));
        badge.setPadding(dp(8), dp(3), dp(8), dp(3));
        header.addView(badge);

        TextView title = label("ZennKuy  —  Google Login Fix");
        title.setTypeface(null, Typeface.BOLD);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleLp.leftMargin = dp(10);
        header.addView(title, titleLp);
        card.addView(header);
        card.addView(divider());

        // ── Body ──────────────────────────────────────────────────────────────────────
        ScrollView scroll = new ScrollView(ctx);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(10));

        TextView intro = label("Fix Error 10 — sign in with your Google account\nto obtain an ltoken for this device.");
        intro.setTextColor(C_MUTED);
        root.addView(intro);
        root.addView(spacer(12));

        root.addView(sectionLabel("MAC ADDRESS"));
        LinearLayout macRow = row();
        EditText macEdit = field(ds.getMac());
        Button macRand = smallBtn("RANDOM");
        macRand.setOnClickListener(v -> {
            String m = DeviceSpoofer.generateMac();
            ds.setMac(m);
            macEdit.setText(m);
        });
        macRow.addView(macEdit, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        macRow.addView(macRand);
        root.addView(macRow);
        root.addView(spacer(10));

        root.addView(sectionLabel("RID"));
        LinearLayout ridRow = row();
        EditText ridEdit = field(ds.getRid());
        Button ridRand = smallBtn("RANDOM");
        ridRand.setOnClickListener(v -> {
            String r = DeviceSpoofer.generateRid();
            ds.setRid(r);
            ridEdit.setText(r);
        });
        ridRow.addView(ridEdit, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ridRow.addView(ridRand);
        root.addView(ridRow);
        root.addView(spacer(10));

        root.addView(sectionLabel("GID (WK)"));
        LinearLayout wkRow = row();
        EditText wkEdit = field(ds.getGid());
        Button wkRand = smallBtn("RANDOM");
        wkRand.setOnClickListener(v -> {
            String g = DeviceSpoofer.generateGid();
            ds.setGid(g);
            wkEdit.setText(g);
        });
        wkRow.addView(wkEdit, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        wkRow.addView(wkRand);
        root.addView(wkRow);
        root.addView(spacer(14));

        root.addView(sectionLabel("LOGIN URL TARGET"));
        String cachedUrl = WebViewManager.sLastLoginUrl;
        TextView urlTarget = label(cachedUrl != null && !cachedUrl.isEmpty()
                ? cachedUrl : "(tap Play Online in-game first)");
        urlTarget.setTextColor(cachedUrl != null && !cachedUrl.isEmpty() ? C_ACCENT : C_MUTED);
        urlTarget.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        urlTarget.setTypeface(android.graphics.Typeface.MONOSPACE);
        root.addView(urlTarget);
        root.addView(spacer(14));

        TextView hint = label("Tap Play Online in the game → tap LOGIN TOKEN → pick\nyour Google account in Chrome → game logs in.");
        hint.setTextColor(C_MUTED);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        root.addView(hint);
        root.addView(spacer(14));

        Button resolveBtn = new Button(ctx);
        resolveBtn.setText("LOGIN TOKEN");
        resolveBtn.setTextColor(Color.WHITE);
        resolveBtn.setTypeface(null, Typeface.BOLD);
        resolveBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        resolveBtn.setElevation(dp(2));
        GradientDrawable resolveBg = roundedRect(C_ACCENT_DK, C_ACCENT_DK, 0, 10);
        resolveBtn.setBackground(rippled(resolveBg, C_ACCENT));
        resolveBtn.setPadding(0, dp(12), 0, dp(12));
        root.addView(resolveBtn);
        root.addView(spacer(10));

        // ── Debug log viewer ────────────────────────────────────────────────────────────────────
        LinearLayout logRow = row();
        Button logsBtn = smallBtn("VIEW LOGS");
        logsBtn.setOnClickListener(v -> showLogs());
        Button clearBtn = smallBtn("CLEAR LOGS");
        clearBtn.setTextColor(Color.parseColor("#E74C3C"));
        GradientDrawable clearBg = roundedRect(Color.TRANSPARENT, Color.parseColor("#E74C3C"), 1, 8);
        clearBtn.setBackground(rippled(clearBg, Color.parseColor("#E74C3C")));
        clearBtn.setOnClickListener(v -> {
            AppLogger.clear();
            android.widget.Toast.makeText(ctx, "Logs cleared", android.widget.Toast.LENGTH_SHORT).show();
        });
        logRow.addView(logsBtn);
        logRow.addView(clearBtn);
        root.addView(logRow);
        root.addView(spacer(6));

        TextView close = label("CLOSE");
        close.setTextColor(C_MUTED);
        close.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        close.setGravity(Gravity.CENTER);
        close.setPadding(0, dp(10), 0, dp(4));
        close.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(close);

        scroll.addView(root);
        card.addView(scroll);

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setView(card);
        AlertDialog dialog = builder.create();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        close.setOnClickListener(v -> dialog.dismiss());

        resolveBtn.setOnClickListener(v -> {
            String mac = macEdit.getText().toString().trim();
            String rid = ridEdit.getText().toString().trim();
            String gid = wkEdit.getText().toString().trim();
            if (!mac.isEmpty()) ds.setMac(mac);
            if (!rid.isEmpty()) ds.setRid(rid);
            if (!gid.isEmpty()) ds.setGid(gid);

            dialog.dismiss();
            startResolving();
        });

        dialog.show();
    }

    private void showLogs() {
        final String logs = AppLogger.getLogs();

        android.widget.ScrollView sv = new android.widget.ScrollView(ctx);
        TextView tv = new TextView(ctx);
        tv.setText(logs);
        tv.setTextColor(C_TEXT);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        tv.setPadding(dp(12), dp(12), dp(12), dp(12));
        sv.setBackgroundColor(C_FIELD_BG);
        sv.addView(tv);
        // Scroll to bottom — newest entries are at the bottom
        sv.post(() -> sv.fullScroll(android.view.View.FOCUS_DOWN));

        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle("Login Debug Logs");
        b.setView(sv);
        b.setPositiveButton("CLOSE", null);
        b.setNeutralButton("COPY ALL", (d, w) -> {
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("ZK Logs", logs));
                Toast.makeText(ctx, "Logs copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });
        b.show();
    }

    private void startResolving() {
        ZennKuyBridge.sTokenDelivered = false;  // user explicitly starting a new login
        ZennKuyBridge.startResolving();
    }

    // ── Styled building blocks ────────────────────────────────────────────────────────────────────

    private TextView label(String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextColor(C_TEXT);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        return tv;
    }

    private TextView sectionLabel(String text) {
        TextView tv = label(text);
        tv.setTextColor(C_MUTED);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(dp(2), 0, 0, dp(4));
        return tv;
    }

    private EditText field(String value) {
        EditText et = new EditText(ctx);
        et.setText(value);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        et.setTypeface(Typeface.MONOSPACE);
        et.setTextColor(C_TEXT);
        et.setHintTextColor(C_MUTED);
        et.setSingleLine(true);
        et.setBackground(roundedRect(C_FIELD_BG, C_FIELD_BRD, 1, 8));
        et.setPadding(dp(10), dp(8), dp(10), dp(8));
        return et;
    }

    private Button smallBtn(String text) {
        Button b = new Button(ctx);
        b.setText(text);
        b.setTextColor(C_ACCENT);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        b.setPadding(dp(10), 0, dp(10), 0);
        GradientDrawable bg = roundedRect(Color.TRANSPARENT, C_ACCENT, 1, 8);
        b.setBackground(rippled(bg, C_ACCENT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(34));
        lp.leftMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    private LinearLayout row() {
        LinearLayout ll = new LinearLayout(ctx);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setGravity(Gravity.CENTER_VERTICAL);
        return ll;
    }

    private View spacer(int dpH) {
        View v = new View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(dpH)));
        return v;
    }

    private View divider() {
        View v = new View(ctx);
        v.setBackgroundColor(C_BORDER);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        return v;
    }

    private GradientDrawable roundedRect(int fill, int stroke, int strokeWidthDp, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        if (strokeWidthDp > 0) d.setStroke(dp(strokeWidthDp), stroke);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    /** Wraps a drawable in a ripple so buttons feel like a real mod-menu UI. */
    private Drawable rippled(Drawable content, int rippleColor) {
        ColorStateList ripple = ColorStateList.valueOf(Color.argb(90,
                Color.red(rippleColor), Color.green(rippleColor), Color.blue(rippleColor)));
        return new RippleDrawable(ripple, content, content);
    }

    private int dp(int v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }

    private static class DragListener implements View.OnTouchListener {
        private float startX, startY, origX, origY;
        private boolean dragging;
        private final View view;

        DragListener(View v) { this.view = v; }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = e.getRawX(); startY = e.getRawY();
                    origX  = v.getX();    origY  = v.getY();
                    dragging = false;
                    return false;
                case MotionEvent.ACTION_MOVE:
                    float dx = e.getRawX() - startX;
                    float dy = e.getRawY() - startY;
                    if (!dragging && Math.abs(dx) + Math.abs(dy) > 10) dragging = true;
                    if (dragging) {
                        v.setX(origX + dx);
                        v.setY(origY + dy);
                    }
                    return dragging;
                case MotionEvent.ACTION_UP:
                    return dragging;
            }
            return false;
        }
    }
}
