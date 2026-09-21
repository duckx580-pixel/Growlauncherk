package com.gentz.launcher;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rtsoft.growtopia.DeviceSpoofer;
import com.rtsoft.growtopia.LoginSpoof;

/**
 * Settings screen: MAC, GID, RID spoofing, OpenGL spoof, fullscreen toggle.
 */
public class SettingsActivity extends AppCompatActivity {

    private DeviceSpoofer spoofer;
    private LoginSpoof loginSpoof;

    private EditText etMac;
    private EditText etGid;
    private EditText etRid;
    private Switch swOpenGL;
    private EditText etOglVersion;
    private EditText etOglExtensions;
    private LinearLayout layoutOpenGLFields;
    private Switch swFullscreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        spoofer = new DeviceSpoofer(this);
        loginSpoof = new LoginSpoof(this);
        bindViews();
        loadCurrentValues();
        hookListeners();
    }

    private void bindViews() {
        ImageButton btnBack = findViewById(R.id.btnSettingsBack);
        etMac = findViewById(R.id.etMac);
        etGid = findViewById(R.id.etGid);
        etRid = findViewById(R.id.etRid);
        swOpenGL = findViewById(R.id.swOpenGL);
        etOglVersion = findViewById(R.id.etOglVersion);
        etOglExtensions = findViewById(R.id.etOglExtensions);
        layoutOpenGLFields = findViewById(R.id.layoutOpenGLFields);
        swFullscreen = findViewById(R.id.swFullscreen);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void loadCurrentValues() {
        etMac.setText(spoofer.getMac());
        etGid.setText(spoofer.getGid());
        etRid.setText(spoofer.getRid());
        swOpenGL.setChecked(spoofer.isSpoofOpenGL());
        etOglVersion.setText(spoofer.getOpenGLVersion());
        etOglExtensions.setText(spoofer.getOpenGLExtensions());
        swFullscreen.setChecked(spoofer.isFullscreen());
        layoutOpenGLFields.setVisibility(
            spoofer.isSpoofOpenGL() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void clearLoginTokens() {
        loginSpoof.clearLtoken();
        loginSpoof.clearGoogleToken();
        toast("Token cleared — login again after spoof change");
    }

    private void hookListeners() {
        Button btnSaveMac = findViewById(R.id.btnSaveMac);
        Button btnRandMac = findViewById(R.id.btnRandMac);
        btnSaveMac.setOnClickListener(v -> {
            String val = etMac.getText().toString().toUpperCase().trim();
            if (!DeviceSpoofer.isValidMac(val)) {
                toast("Invalid MAC — format: XX:XX:XX:XX:XX:XX");
                return;
            }
            spoofer.setMac(val);
            etMac.setText(val);
            clearLoginTokens();
        });
        btnRandMac.setOnClickListener(v -> {
            String mac = DeviceSpoofer.generateMac();
            etMac.setText(mac);
            spoofer.setMac(mac);
            clearLoginTokens();
        });

        Button btnSaveGid = findViewById(R.id.btnSaveGid);
        Button btnRandGid = findViewById(R.id.btnRandGid);
        btnSaveGid.setOnClickListener(v -> {
            String val = etGid.getText().toString().trim();
            if (!DeviceSpoofer.isValidGid(val)) {
                toast("Invalid GID — format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx");
                return;
            }
            spoofer.setGid(val);
            clearLoginTokens();
        });
        btnRandGid.setOnClickListener(v -> {
            String gid = DeviceSpoofer.generateGid();
            etGid.setText(gid);
            spoofer.setGid(gid);
            clearLoginTokens();
        });

        Button btnSaveRid = findViewById(R.id.btnSaveRid);
        Button btnRandRid = findViewById(R.id.btnRandRid);
        btnSaveRid.setOnClickListener(v -> {
            String val = etRid.getText().toString().toUpperCase().trim();
            if (!DeviceSpoofer.isValidRid(val)) {
                toast("Invalid RID — must be 32 hex characters");
                return;
            }
            spoofer.setRid(val);
            etRid.setText(val);
            clearLoginTokens();
        });
        btnRandRid.setOnClickListener(v -> {
            String rid = DeviceSpoofer.generateRid();
            etRid.setText(rid);
            spoofer.setRid(rid);
            clearLoginTokens();
        });

        swOpenGL.setOnCheckedChangeListener((btn, checked) -> {
            spoofer.setSpoofOpenGL(checked);
            layoutOpenGLFields.setVisibility(
                checked ? android.view.View.VISIBLE : android.view.View.GONE);
        });

        Button btnSaveOglVersion = findViewById(R.id.btnSaveOglVersion);
        if (btnSaveOglVersion != null) {
            btnSaveOglVersion.setOnClickListener(v -> {
                String val = etOglVersion.getText().toString().trim();
                if (TextUtils.isEmpty(val)) {
                    toast("OpenGL version cannot be empty");
                    return;
                }
                spoofer.setOpenGLVersion(val);
                toast("OpenGL version saved");
            });
        }

        Button btnSaveOglExt = findViewById(R.id.btnSaveOglExtensions);
        if (btnSaveOglExt != null) {
            btnSaveOglExt.setOnClickListener(v -> {
                String val = etOglExtensions.getText().toString().trim();
                spoofer.setOpenGLExtensions(val);
                toast("OpenGL extensions saved");
            });
        }

        swFullscreen.setOnCheckedChangeListener((btn, checked) ->
            spoofer.setFullscreen(checked));
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
