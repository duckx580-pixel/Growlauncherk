package com.gentz.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.gentz.launcher.databinding.ActivityLauncherBinding
import com.rtsoft.growtopia.NativeLibraries
import java.io.File

class LauncherActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLauncherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load the game first so ZennKuy can resolve its rendering symbols.
        System.loadLibrary("growtopia")
        System.loadLibrary("zennkuy")
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.greeting.text = getString(R.string.greeting, LauncherConfig.DEFAULT_USER)
        binding.growtopiaVersion.text =
            getString(R.string.version_value, LauncherConfig.GROWTOPIA_VERSION)
        binding.launcherVersion.text =
            getString(R.string.version_value, LauncherConfig.LAUNCHER_VERSION)
        binding.discordValue.text = LauncherConfig.DISCORD
        binding.roleValue.text = LauncherConfig.DEFAULT_ROLE
        binding.versionChanger.text =
            getString(R.string.version_changer_value, LauncherConfig.GROWTOPIA_VERSION)

        binding.launchButton.setOnClickListener { launchGrowtopia() }
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.scriptHubButton.setOnClickListener { comingSoon(R.string.menu_script_hub) }
        binding.myScriptButton.setOnClickListener { comingSoon(R.string.menu_my_script) }
        binding.themePickerButton.setOnClickListener { comingSoon(R.string.menu_theme_picker) }
        binding.crashLogButton.setOnClickListener {
            val report = CrashLogger.latestReport()
            if (report == null) {
                Toast.makeText(this, R.string.crash_log_empty, Toast.LENGTH_SHORT).show()
            } else {
                shareReport(report)
            }
        }
        binding.crashLogButton.setOnLongClickListener {
            CrashLogger.selfTestCrash()
            true
        }

        CrashLogger.consumePendingNativeCrash()?.let { report ->
            AlertDialog.Builder(this)
                .setTitle(R.string.crash_detected_title)
                .setMessage(getString(R.string.crash_detected_message, report.name))
                .setPositiveButton(R.string.crash_share) { _, _ -> shareReport(report) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun shareReport(report: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.crashlogs", report)
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, report.name)
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_TEXT, report.readText().take(100_000))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                getString(R.string.crash_share)
            )
        )
    }

    private fun comingSoon(labelRes: Int) {
        Toast.makeText(this, getString(R.string.coming_soon, getString(labelRes)), Toast.LENGTH_SHORT)
            .show()
    }

    private fun launchGrowtopia() {
        if (!NativeLibraries.isGameLibraryPresent(this)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.engine_missing_title)
                .setMessage(
                    getString(
                        R.string.engine_missing_message,
                        NativeLibraries.GAME_LIBRARY,
                        LauncherConfig.GROWTOPIA_VERSION
                    )
                )
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        val gameIntent = Intent().setClassName(this, "com.rtsoft.growtopia.Main")
            .putExtra("growtopia_version", LauncherConfig.GROWTOPIA_VERSION)
            .putExtra("launcher_version", LauncherConfig.LAUNCHER_VERSION)
            .putExtra("user_name", LauncherConfig.DEFAULT_USER)
            .putExtra("user_role", LauncherConfig.DEFAULT_ROLE)
        CrashLogger.markLaunchStarted()
        startActivity(gameIntent)
    }

}
