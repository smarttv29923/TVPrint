package sk.michal.tvprint;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {

    private static final String BASE_URL = "http://192.168.1.222/tvprint/";
    private static final String CAPTURE_URL = BASE_URL + "capture.php";
    private static final String PRINT_URL   = BASE_URL + "print.php";
    private static final String CANCEL_URL  = BASE_URL + "cancel.php";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean starting = new AtomicBoolean(false);
    private final AtomicBoolean actionInProgress = new AtomicBoolean(false);

    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        beginCapture();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (dialog == null || !dialog.isShowing()) {
            beginCapture();
        }
    }

    private void beginCapture() {
        if (!starting.compareAndSet(false, true)) return;

        executor.execute(() -> {
            HttpResult result = httpGet(CAPTURE_URL);

            runOnUiThread(() -> {
                starting.set(false);

                if (isFinishing() || isDestroyed()) return;

                if (result.ok) {
                    showPrintDialog();
                } else {
                    Toast.makeText(
                            this,
                            "Screenshot zlyhal: " + result.message,
                            Toast.LENGTH_LONG
                    ).show();
                    finishAndRemoveTask();
                }
            });
        });
    }

    private void showPrintDialog() {
        if (dialog != null && dialog.isShowing()) return;

        actionInProgress.set(false);

        dialog = new AlertDialog.Builder(this)
                .setTitle("TV Print")
                .setMessage("Vytlačiť zachytený obrázok?")
                .setPositiveButton("TLAČIŤ", null)
                .setNegativeButton("NIE", null)
                .setCancelable(false)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (actionInProgress.compareAndSet(false, true)) {
                    disableButtons();
                    callActionAndClose(PRINT_URL, true);
                }
            });

            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> {
                if (actionInProgress.compareAndSet(false, true)) {
                    disableButtons();
                    callActionAndClose(CANCEL_URL, false);
                }
            });

            dialog.getButton(DialogInterface.BUTTON_POSITIVE).requestFocus();
        });

        dialog.show();
    }

    private void disableButtons() {
        if (dialog == null) return;
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
    }

    private void callActionAndClose(String url, boolean printing) {
        executor.execute(() -> {
            HttpResult result = httpGet(url);

            runOnUiThread(() -> {
                if (!result.ok) {
                    Toast.makeText(
                            this,
                            (printing ? "Tlač zlyhala: " : "Pokračovanie zlyhalo: ") + result.message,
                            Toast.LENGTH_LONG
                    ).show();
                }

                if (dialog != null) dialog.dismiss();
                finishAndRemoveTask();
            });
        });
    }

    private HttpResult httpGet(String address) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(address);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(30000);
            connection.setUseCaches(false);

            int code = connection.getResponseCode();
            InputStream stream = (code >= 200 && code < 400)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String body = readAll(stream).trim();

            if (code >= 200 && code < 300) {
                return new HttpResult(true, body.isEmpty() ? "DONE" : body);
            }

            return new HttpResult(false, "HTTP " + code + (body.isEmpty() ? "" : " - " + body));

        } catch (Exception e) {
            return new HttpResult(false, e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String readAll(InputStream input) throws IOException {
        if (input == null) return "";

        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (out.length() > 0) out.append('\n');
                out.append(line);
            }
        }
        return out.toString();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && dialog != null && dialog.isShowing()) {
            if (actionInProgress.compareAndSet(false, true)) {
                disableButtons();
                callActionAndClose(CANCEL_URL, false);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (dialog != null) dialog.dismiss();
        executor.shutdownNow();
        super.onDestroy();
    }

    private static class HttpResult {
        final boolean ok;
        final String message;

        HttpResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }
    }
}
