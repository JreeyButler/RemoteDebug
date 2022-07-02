package cc.dipperx.debug.remote.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import cc.dipperx.debug.remote.R;

/**
 * @author Dipper
 * @date 2022/7/2 22:17
 */
public class QuickMenuAdapter extends BaseAdapter implements View.OnClickListener {
    public static final int MSG_VIEW_CLICK = 1;

    private final Context context;
    private final Handler handler;

    public QuickMenuAdapter(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
    }

    @Override
    public int getCount() {
        return QuickMenu.QUICK_MENU_NAME_RES_IDS.length;
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Holder holder;
        if (view == null) {
            holder = new Holder();
            view = LayoutInflater.from(context).inflate(R.layout.view_quick_menu_item, null);
            holder.mView = view.findViewById(R.id.quick_menu_name);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }

        holder.mView.setText(QuickMenu.QUICK_MENU_NAME_RES_IDS[i]);
        holder.mView.setTag(i);
        holder.mView.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View view) {
        int id = Integer.parseInt(String.valueOf(view.getTag()));
        if (handler != null) {
            Message msg = handler.obtainMessage();
            msg.what = MSG_VIEW_CLICK;
            msg.arg1 = id;
            msg.sendToTarget();
        }
    }

    private static class Holder {
        TextView mView;
    }
}
