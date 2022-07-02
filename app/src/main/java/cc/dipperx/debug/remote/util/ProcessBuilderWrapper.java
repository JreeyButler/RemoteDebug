package cc.dipperx.debug.remote.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * @author Dipper
 * @date 2022/7/2 11:28
 */
public class ProcessBuilderWrapper {
    private static final String TAG = ProcessBuilderWrapper.class.getSimpleName();

    private static final int MSG_KILL_PROCESS = -9;

    private static final String ROOT_PATH = "/";
    private static final String ROOT_PATH_1 = ".";
    private static final String PREV_PATH = "..";

    private static final StringBuilder EXEC_PATH = new StringBuilder(ROOT_PATH);

    private final boolean recorderErrLog;

    private ProcessBuilder builder;
    private Process process;
    private Callback callback;


    public ProcessBuilderWrapper() {
        this.recorderErrLog = false;
    }

    public ProcessBuilderWrapper(boolean recorderErrLog) {
        this.recorderErrLog = recorderErrLog;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void exec(@NonNull String cmd) {
        if ("".equals(cmd.trim())) {
            return;
        }
        String[] args = cmd.split(" ");
        exec(args);
    }

    public void exec(@NonNull String[] cmdArray) {
        if (cmdArray.length < 1) {
            return;
        }
        if (spCmdExec(cmdArray)) {
            return;
        }
        if (!ROOT_PATH.equals(EXEC_PATH.toString())) {
            exec(cmdArray, EXEC_PATH.toString());
            return;
        }
        builder = new ProcessBuilder(cmdArray);
        builder.redirectErrorStream(recorderErrLog);
        try {
            if (callback != null) {
                callback.onExecCmdWithPath(cmdArray, EXEC_PATH.toString());
            }
            process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (callback != null) {
                    callback.onExecResult(line.trim());
                }
            }
            reader.close();
            BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errReader.readLine()) != null) {
                if (callback != null) {
                    callback.onErrorMsg(line.trim());
                }
            }
            errReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            if (callback != null) {
                callback.onErrorMsg(e.getMessage());
            }
        }
    }

    private boolean spCmdExec(@NonNull String[] cmdArray) {
        final int minimumLength = 2;
        switch (cmdArray[0]) {
            case "cd":
                if (cmdArray.length < minimumLength || cmdArray[1] == null) {
                    EXEC_PATH.delete(0, EXEC_PATH.length());
                    EXEC_PATH.append(ROOT_PATH);
                } else {
                    // 返回根目录
                    if (ROOT_PATH_1.equals(cmdArray[1])) {
                        EXEC_PATH.delete(0, EXEC_PATH.length());
                        EXEC_PATH.append(ROOT_PATH);
                    } else if (PREV_PATH.equals(cmdArray[1])) {
                        String s = EXEC_PATH.toString();
                        // 排除根目录
                        if (!ROOT_PATH.equals(s)) {
                            EXEC_PATH.delete(0, EXEC_PATH.toString().length());
                            EXEC_PATH.append(s.substring(0, s.lastIndexOf(ROOT_PATH)));
                        }
                    } else {
                        if (new File(EXEC_PATH.toString(), cmdArray[1]).exists()) {
                            if (!ROOT_PATH.equals(EXEC_PATH.toString())) {
                                EXEC_PATH.append(ROOT_PATH);
                            }
                            EXEC_PATH.append(cmdArray[1]);
                        }
                    }
                }
                if (callback != null) {
                    StringBuilder signalCmd = new StringBuilder();
                    for (String s : cmdArray) {
                        signalCmd.append(s).append(" ");
                    }
                    callback.onExecCmdWithPath(signalCmd.toString(), EXEC_PATH + "$ ");
                }
                return true;
            case "logcat":
                if (cmdArray.length < minimumLength || cmdArray[1] == null) {
                    Log.d(TAG, "spCmdExec: ");
                    Handler handler = new Handler(Looper.myLooper()) {
                        @Override
                        public void handleMessage(@NonNull Message msg) {
                            Log.d(TAG, "handleMessage: " + msg);
                            super.handleMessage(msg);
                        }
                    };
                    Message msg = handler.obtainMessage();
                    msg.what = MSG_KILL_PROCESS;
                    handler.sendEmptyMessageDelayed(MSG_KILL_PROCESS, 1000L);
                }
                break;
            default:
                break;
        }
        return false;
    }

    public void exec(@NonNull String[] cmdArray, @NonNull String execPath) {
        if (cmdArray.length < 1) {
            return;
        }
        builder = new ProcessBuilder(cmdArray);
        Log.d(TAG, "exec: " + execPath + "," + Arrays.toString(cmdArray));
        builder.directory(new File(execPath));
        builder.redirectErrorStream(recorderErrLog);
        try {
            if (callback != null) {
                callback.onExecCmdWithPath(cmdArray, execPath);
            }
            process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (callback != null) {
                    callback.onExecResult(line.trim());
                }
            }
            reader.close();
            BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errReader.readLine()) != null) {
                if (callback != null) {
                    callback.onErrorMsg(line.trim());
                }
            }
            errReader.close();
            builder = new ProcessBuilder(cmdArray);
        } catch (IOException e) {
            e.printStackTrace();
            if (callback != null) {
                callback.onErrorMsg(e.getMessage());
            }
        }
    }

    public void forceExit() {
        if (process != null) {
            process.destroyForcibly();
        }
    }


    public abstract static class Callback {
        public abstract void onExecCmdWithPath(String cmd, String path);

        public void onExecCmdWithPath(String[] cmdArray, String path) {
            StringBuilder sb = new StringBuilder();
            for (String s : cmdArray) {
                sb.append(s).append(" ");
            }
            onExecCmdWithPath(sb.toString(), path + "$ ");
        }

        public abstract void onExecResult(String content);

        public abstract void onErrorMsg(String errMsg);
    }
}
