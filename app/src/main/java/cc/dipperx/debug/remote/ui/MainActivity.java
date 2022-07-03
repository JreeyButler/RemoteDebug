package cc.dipperx.debug.remote.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import cc.dipperx.debug.remote.R;
import cc.dipperx.debug.remote.util.ProcessBuilderWrapper;
import cc.dipperx.debug.remote.view.QuickMenu;
import cc.dipperx.debug.remote.view.QuickMenuAdapter;

/**
 * @author Dipper
 * @date 2022/7/2 12:57
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private EditText mInputView;
    private EditText mResultView;
    private TextView mCmdPath;
    private HandlerThread thread;
    private Handler execHandler;
    private Handler mainHandler;
    private ProcessBuilderWrapper wrapper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        mInputView = findViewById(R.id.cmd_input_view);
        mResultView = findViewById(R.id.exec_result);
        mCmdPath = findViewById(R.id.cmd_input_path);

        mInputView.addTextChangedListener(watcher);

        GridView menuGridView = findViewById(R.id.quick_menu_view);
        QuickMenuAdapter adapter = new QuickMenuAdapter(this, new Handler(Looper.getMainLooper(), msgCallback));
        menuGridView.setAdapter(adapter);
    }

    private final TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if (charSequence.length() > 0) {
                int index = charSequence.toString().indexOf(System.lineSeparator());
                char c = 0;
                if (charSequence.length() > 1) {
                    c = charSequence.toString().charAt(charSequence.toString().length() - 2);
                }
                // "\" = 92
                final int backslashCode = 92;
                if (index >= 0 && c != backslashCode) {
                    execCmd();
                }
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private final Handler.Callback msgCallback = message -> {
        if (message.what == QuickMenuAdapter.MSG_VIEW_CLICK) {
            int id = message.arg1;
            switch (id) {
                case QuickMenu.CLEAR_INPUT_VIEW_CONTENT:
                    mInputView.setText("");
                    break;
                case QuickMenu.FORCE_EXIT:
                    if (wrapper != null) {
                        wrapper.forceExit();
                    }
                    break;
                default:
                    break;
            }
        }
        return true;
    };

    private void execCmd() {
        if (thread == null || !thread.isAlive()) {
            thread = new HandlerThread("CmdExec");
            thread.start();
            execHandler = new Handler(thread.getLooper());
        }
        execHandler.post(() -> {
            String cmd = mInputView.getText().toString().trim();
            wrapper = new ProcessBuilderWrapper();
            if (mainHandler == null) {
                mainHandler = new Handler(Looper.getMainLooper());
            }
            MyCallback callback = new MyCallback(mCmdPath, mInputView, mResultView, mainHandler);
            wrapper.setCallback(callback);
            wrapper.exec(cmd);
        });
    }


    private static class MyCallback extends ProcessBuilderWrapper.Callback {
        private static final int MAX_LENGTH = 10000;

        public static final StringBuilder STRING_BUILDER = new StringBuilder();
        private final EditText mResultView;
        private final Handler mainHandler;
        private final TextView mPathView;
        private final EditText mInputView;


        public MyCallback(TextView mPathView, EditText mInputView, EditText mResultView, Handler mainHandler) {
            this.mPathView = mPathView;
            this.mInputView = mInputView;
            this.mResultView = mResultView;
            this.mainHandler = mainHandler;
        }

        @Override
        public void onExecCmdWithPath(String cmd, String path) {
            mainHandler.post(() -> mPathView.setText(path));
            mainHandler.post(() -> show(path + cmd));
        }

        @Override
        public void onExecResult(String content) {
            mainHandler.post(() -> show(content));
        }

        @Override
        public void onErrorMsg(String errMsg) {
            mainHandler.post(() -> show(errMsg));
        }

        public void show(String content) {
            if ("".equals(content.trim())) {
                return;
            }
            mainHandler.post(() -> {
                STRING_BUILDER.append(content).append(System.lineSeparator());
                String totalContent = STRING_BUILDER.toString();
                if (totalContent.length() > MAX_LENGTH) {
                    totalContent = totalContent.substring(STRING_BUILDER.length() - MAX_LENGTH, STRING_BUILDER.length());
                    STRING_BUILDER.delete(0, STRING_BUILDER.length());
                    STRING_BUILDER.append(totalContent);
                }
                totalContent = totalContent.trim();
                if (!"".equals(totalContent)) {
                    mResultView.setVisibility(View.VISIBLE);
                    mResultView.setText(totalContent);
                    mResultView.setSelection(totalContent.length());
                }
                mInputView.setText("");
            });
        }
    }
}
