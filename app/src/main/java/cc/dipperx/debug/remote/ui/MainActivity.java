package cc.dipperx.debug.remote.ui;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.Editable;
import android.text.PrecomputedText;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import cc.dipperx.debug.remote.R;
import cc.dipperx.debug.remote.util.ExecutorServiceBuilder;
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
    private AppCompatTextView mResultView;
    private AppCompatTextView mCmdPath;
    private HandlerThread thread;
    private Handler execHandler;
    private Handler mainHandler;
    private ProcessBuilderWrapper wrapper;
    private MyCallback callback;

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
        ScrollView mScrollView = findViewById(R.id.scroll_view);

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
                    MyCallback.clear();
                    mResultView.setText("");
                    mResultView.setVisibility(View.GONE);
                    break;
                case QuickMenu.FORCE_EXIT:
                    if (wrapper != null) {
                        wrapper.forceExit();
                    }
                    if (callback != null) {
                        callback.cancel();
                    }
                    if (mainHandler != null) {
                        mainHandler.removeCallbacksAndMessages(null);
                        mainHandler = null;
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
            callback = new MyCallback(mCmdPath, mInputView, mResultView, mainHandler);
            wrapper.setCallback(callback);
            wrapper.exec(cmd);
        });
    }


    private static class MyCallback extends ProcessBuilderWrapper.Callback {
        private static final int MAX_LENGTH = 5000;

        public static final StringBuilder STRING_BUILDER = new StringBuilder();
        private final TextView mResultView;
        private final Handler mainHandler;
        private final TextView mPathView;
        private final EditText mInputView;
        private ScrollView mScrollView;
        private static final ExecutorService mUpdateService;
        private static Future<?> future;

        static {
            mUpdateService = new ExecutorServiceBuilder().poolSize(1).threadName("Update").build();
        }

        public MyCallback(TextView mPathView, EditText mInputView,
                          TextView mResultView, Handler mainHandler) {
            this.mPathView = mPathView;
            this.mInputView = mInputView;
            this.mResultView = mResultView;
            this.mainHandler = mainHandler;
        }

        public void setScrollView(ScrollView mScrollView) {
            this.mScrollView = mScrollView;
        }

        @Override
        public void onExecCmdWithPath(String cmd, String path) {
            mainHandler.post(() -> mPathView.setText(path));
            show(path + cmd + System.lineSeparator());
        }

        @Override
        public void onExecResult(String content) {
            show(content);
        }

        @Override
        public void onErrorMsg(String errMsg) {
            show(errMsg);
        }

        public void show(String content) {
            if ("".equals(content.trim())) {
                return;
            }
            mainHandler.post(() -> {
                STRING_BUILDER.append(content);
                String totalContent = STRING_BUILDER.toString();
                if (totalContent.length() > MAX_LENGTH) {
                    totalContent = totalContent.substring(STRING_BUILDER.length() - MAX_LENGTH, STRING_BUILDER.length());
                    STRING_BUILDER.delete(0, STRING_BUILDER.length());
                    STRING_BUILDER.append(totalContent);
                }
                totalContent = totalContent.trim();
                if (!"".equals(totalContent)) {
                    mResultView.setVisibility(View.VISIBLE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        asyncSetText(mResultView, totalContent);
                    }
                }
                mInputView.setText("");
                mInputView.clearComposingText();
                if (mScrollView != null) {
                    mScrollView.post(() -> mScrollView.scrollTo(0, 3000));
                }
            });
        }

        public static void clear() {
            STRING_BUILDER.delete(0, STRING_BUILDER.length());
        }

        @RequiresApi(api = Build.VERSION_CODES.P)
        private static void asyncSetText(TextView textView, final String longString) {
            // construct precompute related parameters using the TextView that we will set the text on.
            final PrecomputedText.Params params = textView.getTextMetricsParams();
            final Reference<TextView> textViewRef = new WeakReference<>(textView);

            future = mUpdateService.submit(() -> {
                TextView view = textViewRef.get();
                if (view == null) {
                    return;
                }
                final PrecomputedText precomputedText = PrecomputedText.create(longString, params);
                view.post(() -> {
                    TextView view1 = textViewRef.get();
                    if (view1 == null) {
                        return;
                    }
                    view1.setText(precomputedText);
                });
            });
        }

        public void cancel() {
            if (future != null) {
                future.cancel(true);
            }
        }
    }
}
