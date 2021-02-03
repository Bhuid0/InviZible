package pan.alexander.tordnscrypt.itpd_fragment;

/*
    This file is part of InviZible Pro.

    InviZible Pro is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    InviZible Pro is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with InviZible Pro.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2019-2021 by Garmatin Oleksandr invizible.soft@gmail.com
*/

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.ScaleGestureDetector;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import pan.alexander.tordnscrypt.MainActivity;
import pan.alexander.tordnscrypt.R;
import pan.alexander.tordnscrypt.TopFragment;
import pan.alexander.tordnscrypt.dialogs.NotificationDialogFragment;
import pan.alexander.tordnscrypt.modules.ModulesAux;
import pan.alexander.tordnscrypt.modules.ModulesKiller;
import pan.alexander.tordnscrypt.modules.ModulesRunner;
import pan.alexander.tordnscrypt.modules.ModulesStatus;
import pan.alexander.tordnscrypt.settings.PathVars;
import pan.alexander.tordnscrypt.utils.CachedExecutor;
import pan.alexander.tordnscrypt.utils.OwnFileReader;
import pan.alexander.tordnscrypt.utils.PrefManager;
import pan.alexander.tordnscrypt.utils.enums.ModuleState;
import pan.alexander.tordnscrypt.utils.file_operations.FileOperations;

import static pan.alexander.tordnscrypt.TopFragment.ITPDVersion;
import static pan.alexander.tordnscrypt.utils.RootExecService.LOG_TAG;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.FAULT;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.RESTARTING;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.RUNNING;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.STARTING;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.STOPPED;
import static pan.alexander.tordnscrypt.utils.enums.ModuleState.STOPPING;

public class ITPDFragmentPresenter implements ITPDFragmentPresenterCallbacks {

    private boolean runI2PDWithRoot = false;
    private int displayLogPeriod = -1;

    private ITPDFragmentView view;
    private ScheduledFuture<?> scheduledFuture;
    private String appDataDir;
    private volatile OwnFileReader logFile;
    private ModulesStatus modulesStatus;
    private ModuleState fixedModuleState;
    private boolean itpdLogAutoScroll = true;
    private ScaleGestureDetector scaleGestureDetector;
    private Handler handler;


    public ITPDFragmentPresenter(ITPDFragmentView view) {
        this.view = view;
    }

    public void onStart(Context context) {
        if (context == null || view == null) {
            return;
        }

        Looper looper = Looper.getMainLooper();
        if (looper != null) {
            handler = new Handler(looper);
        }

        PathVars pathVars = PathVars.getInstance(context);
        appDataDir = pathVars.getAppDataDir();

        modulesStatus = ModulesStatus.getInstance();

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(context);
        runI2PDWithRoot = shPref.getBoolean("swUseModulesRoot", false);

        logFile = new OwnFileReader(context, appDataDir + "/logs/i2pd.log");

        if (isITPDInstalled(context)) {
            setITPDInstalled(true);

            if (modulesStatus.getItpdState() == STOPPING){
                setITPDStopping();

                displayLog(1);
            } else if (isSavedITPDStatusRunning(context) || modulesStatus.getItpdState() == RUNNING) {
                setITPDRunning();

                if (modulesStatus.getItpdState() != RESTARTING) {
                    modulesStatus.setItpdState(RUNNING);
                }

                displayLog(5);
            } else {
                setITPDStopped();
                modulesStatus.setItpdState(STOPPED);
            }
        } else {
            setITPDInstalled(false);
        }

        registerZoomGestureDetector(context);
    }

    public void onStop() {

        stopDisplayLog();

        view = null;

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    private void setITPDStarting() {
        if (view == null || view.getFragmentActivity() == null || view.getFragmentActivity().isFinishing()) {
            return;
        }

        view.setITPDStatus(R.string.tvITPDStarting, R.color.textModuleStatusColorStarting);
    }

    @Override
    public void setITPDRunning() {
        if (view == null || view.getFragmentActivity() == null || view.getFragmentActivity().isFinishing()) {
            return;
        }

        view.setITPDStatus(R.string.tvITPDRunning, R.color.textModuleStatusColorRunning);
        view.setStartButtonText(R.string.btnITPDStop);
    }

    private void setITPDStopping() {
        if (view == null || view.getFragmentActivity() == null || view.getFragmentActivity().isFinishing()) {
            return;
        }

        view.setITPDStatus(R.string.tvITPDStopping, R.color.textModuleStatusColorStopping);
    }

    @Override
    public void setITPDStopped() {
        if (view == null || view.getFragmentActivity() == null || view.getFragmentActivity().isFinishing()) {
            return;
        }

        view.setITPDStatus(R.string.tvITPDStop, R.color.textModuleStatusColorStopped);
        view.setStartButtonText(R.string.btnITPDStart);
        view.setITPDLogViewText();

        view.setITPDInfoLogText();
    }

    @Override
    public void setITPDInstalling() {
        if (view == null || view.getFragmentActivity() == null || view.getFragmentActivity().isFinishing()) {
            return;
        }

        view.setITPDStatus(R.string.tvITPDInstalling, R.color.textModuleStatusColorInstalling);
    }

    @Override
    public void setITPDInstalled() {
        if (view == null || view.getFragmentActivity() == null || view.getFragmentActivity().isFinishing()) {
            return;
        }

        view.setITPDStatus(R.string.tvITPDInstalled, R.color.textModuleStatusColorInstalled);
    }

    @Override
    public void setITPDStartButtonEnabled(boolean enabled) {
        if (view == null || view.getFragmentActivity() == null || view.getFragmentActivity().isFinishing()) {
            return;
        }

        view.setITPDStartButtonEnabled(enabled);
    }

    @Override
    public void setITPDProgressBarIndeterminate(boolean indeterminate) {
        if (view == null || view.getFragmentActivity() == null || view.getFragmentActivity().isFinishing()) {
            return;
        }

        view.setITPDProgressBarIndeterminate(indeterminate);
    }

    private void setITPDInstalled(boolean installed) {
        if (view == null || view.getFragmentActivity() == null || view.getFragmentActivity().isFinishing()) {
            return;
        }

        if (installed) {
            view.setITPDStartButtonEnabled(true);
        } else {
            view.setITPDStatus(R.string.tvITPDNotInstalled, R.color.textModuleStatusColorAlert);
        }
    }

    @Override
    public void setITPDSomethingWrong() {
        if (view == null || view.getFragmentActivity() == null || view.getFragmentActivity().isFinishing() || modulesStatus == null) {
            return;
        }

        view.setITPDStatus(R.string.wrong, R.color.textModuleStatusColorAlert);
        modulesStatus.setItpdState(FAULT);
    }

    @Override
    public boolean isITPDInstalled(Context context) {
        if (context != null) {
            return new PrefManager(context).getBoolPref("I2PD Installed");
        }
        return false;
    }

    @Override
    public boolean isSavedITPDStatusRunning(Context context) {
        if (context != null) {
            return new PrefManager(context).getBoolPref("I2PD Running");
        }
        return false;
    }

    @Override
    public void saveITPDStatusRunning(Context context, boolean running) {
        if (context != null) {
            new PrefManager(context).setBoolPref("I2PD Running", running);
        }
    }

    @Override
    public void refreshITPDState(Context context) {

        if (context == null || modulesStatus == null || view == null) {
            return;
        }

        ModuleState currentModuleState = modulesStatus.getItpdState();

        if (currentModuleState.equals(fixedModuleState) && currentModuleState != STOPPED) {
            return;
        }


        if (currentModuleState == STARTING) {

            displayLog(1);

        } else if (currentModuleState == RUNNING) {

            setITPDRunning();

            view.setITPDStartButtonEnabled(true);

            saveITPDStatusRunning(context, true);

            view.setITPDProgressBarIndeterminate(false);

        } else if (currentModuleState == STOPPED) {

            stopDisplayLog();

            if (isSavedITPDStatusRunning(context)) {
                setITPDStoppedBySystem(context);
            } else {
                setITPDStopped();
            }



            saveITPDStatusRunning(context, false);

            view.setITPDStartButtonEnabled(true);
        }

        fixedModuleState = currentModuleState;
    }


    private void setITPDStoppedBySystem(Context context) {

        setITPDStopped();

        if (context != null && view != null && modulesStatus != null) {

            modulesStatus.setItpdState(STOPPED);

            ModulesAux.requestModulesStatusUpdate(context);

            if (view.getFragmentFragmentManager() != null && !view.getFragmentActivity().isFinishing()) {
                DialogFragment notification = NotificationDialogFragment.newInstance(R.string.helper_itpd_stopped);
                notification.show(view.getFragmentFragmentManager(), "NotificationDialogFragment");
            }

            Log.e(LOG_TAG, context.getText(R.string.helper_itpd_stopped).toString());
        }

    }

    @Override
    public synchronized void displayLog(int period) {

        ScheduledExecutorService timer = TopFragment.getModulesLogsTimer();

        if ((timer == null || timer.isShutdown()) && handler != null) {
            handler.postDelayed(() -> {

                if (view != null && view.getFragmentActivity() != null && !view.getFragmentActivity().isDestroyed()) {
                    displayLog(period);
                }

            }, 1000);

            return;
        }

        if (period == displayLogPeriod || timer == null) {
            return;
        }

        displayLogPeriod = period;

        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(false);
        }

        scheduledFuture = timer.scheduleAtFixedRate(new Runnable() {
            int loop = 0;
            int previousLastLinesLength = 0;

            @Override
            public void run() {

                try {
                    if (view == null || view.getFragmentActivity() == null || view.getFragmentActivity().isFinishing() || logFile == null) {
                        return;
                    }

                    final String lastLines = logFile.readLastLines();

                    final String htmlData = readITPDStatusFromHTML(view.getFragmentActivity());

                    if (++loop > 30) {
                        loop = 0;
                        displayLog(10);
                    }

                    if (view == null || view.getFragmentActivity() == null || view.getFragmentActivity().isFinishing()
                            || handler == null || lastLines == null || lastLines.isEmpty()) {
                        return;
                    }

                    Spanned htmlLastLines = Html.fromHtml(lastLines);

                    Spanned htmlDataLines;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        htmlDataLines = Html.fromHtml(htmlData, Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        htmlDataLines = Html.fromHtml(htmlData);
                    }

                    handler.post(() -> {

                        if (view == null || view.getFragmentActivity() == null || view.getFragmentActivity().isFinishing()) {
                            return;
                        }

                        if (previousLastLinesLength != lastLines.length() && htmlLastLines != null && itpdLogAutoScroll) {
                            view.setITPDInfoLogText(htmlLastLines);
                            view.scrollITPDLogViewToBottom();
                            previousLastLinesLength = lastLines.length();
                        }

                        if (htmlDataLines != null) {
                            view.setITPDLogViewText(htmlDataLines);
                        }

                        refreshITPDState(view.getFragmentActivity());
                    });
                } catch (Exception e) {
                    Log.e(LOG_TAG, "ITPDFragmentPresenter timer run() exception " + e.getMessage() + " " + e.getCause());
                }
            }
        }, 1, period, TimeUnit.SECONDS);

    }

    @Override
    public void stopDisplayLog() {
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(false);

            displayLogPeriod = -1;
        }
    }

    private String readITPDStatusFromHTML(Context context) {
        String htmlData = context.getResources().getString(R.string.tvITPDDefaultLog) + " " + ITPDVersion;
        try {
            StringBuilder sb = new StringBuilder();

            URL url = new URL("http://127.0.0.1:7070/");
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            huc.setRequestMethod("GET");  //OR  huc.setRequestMethod ("HEAD");
            huc.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 9.0.1; " +
                    "Mi Mi) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Mobile Safari/537.36");
            huc.setConnectTimeout(1000);
            huc.connect();
            int code = huc.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                huc.disconnect();
                return htmlData;
            }


            BufferedReader in;
            in = new BufferedReader(
                    new InputStreamReader(
                            url.openStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains("<b>Network status:</b>") || inputLine.contains("<b>Tunnel creation success rate:</b>") ||
                        inputLine.contains("<b>Received:</b> ") || inputLine.contains("<b>Sent:</b>") || inputLine.contains("<b>Transit:</b>") ||
                        inputLine.contains("<b>Routers:</b>") || inputLine.contains("<b>Client Tunnels:</b>") || inputLine.contains("<b>Uptime:</b>")) {
                    inputLine = inputLine.replace("<div class=\"content\">", "");
                    inputLine = inputLine.replace("<br>", "<br />");
                    sb.append(inputLine);
                }
            }
            in.close();
            huc.disconnect();
            htmlData = sb.toString();
            if (htmlData.contains("<br />")) {
                htmlData = htmlData.substring(0, htmlData.lastIndexOf( "<br />"));
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to read I2PD html" + e.toString());
        }

        return htmlData;
    }

    public void startButtonOnClick(Context context) {
        if (context == null || view == null || view.getFragmentActivity() == null || view.getFragmentActivity().isFinishing() || modulesStatus == null) {
            return;
        }

        if (context instanceof MainActivity && ((MainActivity) context).childLockActive) {
            Toast.makeText(context, context.getText(R.string.action_mode_dialog_locked), Toast.LENGTH_LONG).show();
            return;
        }

        view.setITPDStartButtonEnabled(false);

        if (!new PrefManager(Objects.requireNonNull(context)).getBoolPref("I2PD Running")
                && new PrefManager(Objects.requireNonNull(context)).getBoolPref("Tor Running")
                && !new PrefManager(context).getBoolPref("DNSCrypt Running")) {

            if (modulesStatus.isContextUIDUpdateRequested()) {
                Toast.makeText(context, R.string.please_wait, Toast.LENGTH_SHORT).show();
                view.setITPDStartButtonEnabled(true);
                return;
            }

            copyCertificatesNoRootMethod(context);

            setITPDStarting();

            runITPD(context);

            displayLog(1);
        } else if (!new PrefManager(Objects.requireNonNull(context)).getBoolPref("I2PD Running") &&
                !new PrefManager(context).getBoolPref("Tor Running")
                && !new PrefManager(context).getBoolPref("DNSCrypt Running")) {

            if (modulesStatus.isContextUIDUpdateRequested()) {
                Toast.makeText(context, R.string.please_wait, Toast.LENGTH_SHORT).show();
                view.setITPDStartButtonEnabled(true);
                return;
            }

            copyCertificatesNoRootMethod(context);

            setITPDStarting();

            runITPD(context);

            displayLog(1);
        } else if (!new PrefManager(Objects.requireNonNull(context)).getBoolPref("I2PD Running") &&
                !new PrefManager(context).getBoolPref("Tor Running")
                && new PrefManager(context).getBoolPref("DNSCrypt Running")) {

            if (modulesStatus.isContextUIDUpdateRequested()) {
                Toast.makeText(context, R.string.please_wait, Toast.LENGTH_SHORT).show();
                view.setITPDStartButtonEnabled(true);
                return;
            }

            copyCertificatesNoRootMethod(context);

            setITPDStarting();

            runITPD(context);

            displayLog(1);
        } else if (!new PrefManager(Objects.requireNonNull(context)).getBoolPref("I2PD Running") &&
                new PrefManager(context).getBoolPref("Tor Running")
                && new PrefManager(context).getBoolPref("DNSCrypt Running")) {

            if (modulesStatus.isContextUIDUpdateRequested()) {
                Toast.makeText(context, R.string.please_wait, Toast.LENGTH_SHORT).show();
                view.setITPDStartButtonEnabled(true);
                return;
            }

            copyCertificatesNoRootMethod(context);

            setITPDStarting();

            runITPD(context);

            displayLog(1);
        } else if (new PrefManager(Objects.requireNonNull(context)).getBoolPref("I2PD Running")) {

            setITPDStopping();

            stopITPD(context);

            OwnFileReader ofr = new OwnFileReader(context, appDataDir + "/logs/i2pd.log");

            ofr.shortenTooTooLongFile();
        }

        view.setITPDProgressBarIndeterminate(true);
    }

    private void runITPD(Context context) {

        if (context == null || view == null || view.getFragmentActivity() == null || view.getFragmentActivity().isFinishing()) {
            return;
        }

        ModulesRunner.runITPD(context);
    }

    private void stopITPD(Context context) {

        if (context == null || view == null || view.getFragmentActivity() == null || view.getFragmentActivity().isFinishing()) {
            return;
        }

        ModulesKiller.stopITPD(context);
    }

    private void copyCertificatesNoRootMethod(Context context) {

        if (context == null || runI2PDWithRoot) {
            return;
        }

        final String certificateSource = appDataDir + "/app_data/i2pd/certificates";
        final String certificateFolder = appDataDir + "/i2pd_data/certificates";
        final String certificateDestination = appDataDir + "/i2pd_data";

        CachedExecutor.INSTANCE.getExecutorService().submit(() -> {

            File certificateFolderDir = new File(certificateFolder);

            if (certificateFolderDir.isDirectory()
                    && certificateFolderDir.listFiles() != null
                    && Objects.requireNonNull(certificateFolderDir.listFiles()).length > 0) {
                return;
            }

            FileOperations.copyFolderSynchronous(context, certificateSource, certificateDestination);
            Log.i(LOG_TAG, "Copy i2p certificates");
        });
    }

    public void itpdLogAutoScrollingAllowed(boolean allowed) {
        itpdLogAutoScroll = allowed;
    }

    private void registerZoomGestureDetector(Context context) {

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
                setLogsTextSize(context, scaleGestureDetector.getScaleFactor());
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
            }
        });
    }

    private void setLogsTextSize(Context context, float scale) {
        float logsTextSizeMin = context.getResources().getDimension(R.dimen.fragment_log_text_size);
        float logsTextSize = (float) Math.max(logsTextSizeMin, Math.min(TopFragment.logsTextSize * scale, logsTextSizeMin * 1.5));
        TopFragment.logsTextSize = logsTextSize;

        if (view != null) {
            view.setLogsTextSize(logsTextSize);
        }
    }

    public ScaleGestureDetector getScaleGestureDetector() {
        return scaleGestureDetector;
    }

}
