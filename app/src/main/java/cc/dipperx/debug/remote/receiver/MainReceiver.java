package cc.dipperx.debug.remote.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @author Dipper
 * @date 2022/7/2 12:54
 */
public class MainReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {

        }
    }
}
