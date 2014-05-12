package ca.cydonian.packetsniffer.app;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.packetsniffer.app.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends ActionBarActivity {

    protected ToggleButton toggle;
    protected EditText port;
    protected TextView message;
    protected String tcpdump;
    protected String nc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get root privilege
        try {
            Process p = Runtime.getRuntime().exec("su");

            // Create the executables if they don't exist
            nc = createExecutable("nc");
            tcpdump = createExecutable("tcpdump");

        } catch (IOException e) {
            e.printStackTrace();
        }

        message = (TextView)findViewById(R.id.sniffingDescription);
        message.setVisibility(View.INVISIBLE);

        toggle = (ToggleButton)findViewById(R.id.toggleButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    new DoStiffing().execute();
                    String msg = "Connect the USB cable and run the following: adb forward tcp:12345 tcp:" + port.getText().toString() +
                            "&& netcat localhost 12345 | wireshark -k -S -i -";
                    message.setText(msg);
                    message.setVisibility(View.VISIBLE);
                }
            }
        });
        port = (EditText)findViewById(R.id.portEditText);
        port.setText("31337");
    }

    private String createExecutable(String filename) throws IOException {
        File f = new File(getFilesDir() + "/" + filename);
        try {
            if (!f.exists()) {
                AssetManager manager = getAssets();
                InputStream mInput = manager.open(filename);
                int size = mInput.available();
                byte[] buffer = new byte[size];
                mInput.read(buffer);
                mInput.close();

                FileOutputStream fos = new FileOutputStream(f);
                fos.write(buffer);
                fos.close();

                // Make executable
                Runtime.getRuntime().exec("chmod 755 " + f.getPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return f.getPath();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private class DoStiffing extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPostExecute(Void na) {
            toggle.setChecked(false);
            message.setVisibility(View.INVISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Runtime r = Runtime.getRuntime();
                Process p = r.exec(tcpdump + " -s 0 -w - | " + nc + " -l -p " + port);
                p.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
