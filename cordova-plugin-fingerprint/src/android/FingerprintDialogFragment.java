package net.cloudseat.fingerprint;

import android.app.DialogFragment;
import android.hardware.fingerprint.FingerprintManager;

import android.os.CancellationSignal;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import javax.crypto.Cipher;

public class FingerprintDialogFragment extends DialogFragment {

    private FingerprintManager fingerprintManager;
    private CancellationSignal cancellationSignal;

    private Cipher cipher;
    private TextView message;

    // 是否用户主动取消
    private boolean isSelfCancelled;

    public void setCipher(Cipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fingerprintManager = getContext().getSystemService(FingerprintManager.class);
        // 将当前 DialogFragment 设置为无标题样式
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Material_Light_Dialog);
    }

    /**
     * 重新设置 dialog 宽度为屏幕的 80% （最小宽度样式太宽）
     * 设置宽高必须在 onStart 或 onResume 方法中实现
     */
    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        int width = window.getWindowManager().getDefaultDisplay().getWidth();
        window.setLayout((int) (width * 0.8), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String pkg = FingerprintPlugin.packageName;
        int layoutId = getResources().getIdentifier("fingerprint_dialog", "layout", pkg);
        int messageId = getResources().getIdentifier("message", "id", pkg);
        int cancelId = getResources().getIdentifier("cancel", "id", pkg);

        View layout = inflater.inflate(layoutId, container, false);
        message = (TextView) layout.findViewById(messageId);

        TextView cancel = (TextView) layout.findViewById(cancelId);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAllowingStateLoss();
                stopListening();
            }
        });
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        startListening();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopListening();
    }

    ///////////////////////////////////////////////////////
    // 私有方法
    ///////////////////////////////////////////////////////

    /**
     * 开始监听指纹输入
     */
    private void startListening() {
        isSelfCancelled = false;
        cancellationSignal = new CancellationSignal();

        fingerprintManager.authenticate(new FingerprintManager.CryptoObject(cipher), cancellationSignal, 0, new FingerprintManager.AuthenticationCallback() {

            // 多次认证失败后进入此方法，且短时间内不可再验
            // errorCode是失败的次数
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                if (!isSelfCancelled) {
                    message.setTextColor(0xffff4455);
                    message.setText(errString);

                    if (errorCode == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT) {
                        message.setText("失败次数过多，请稍后再试");
                        // 为显示错误信息，延迟 1000 毫秒执行回调
                        message.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                dismissAllowingStateLoss();
                            }
                        }, 1000);
                    }
                }
            }

            // 指纹认证失败：该指纹不是系统录入的指纹
            @Override
            public void onAuthenticationFailed() {
                message.setTextColor(0xffff4455);
                message.setText("指纹认证失败");
            }

            // 指纹认证失败：可能是手指过脏，或移动过快等原因。
            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                message.setTextColor(0xff333333);
                message.setText(helpString);
            }

            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                message.setTextColor(0xff00cc77);
                message.setText("指纹认证成功");

                // 为显示成功信息，延迟 500 毫秒执行回调
                message.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        FingerprintPlugin.onAuthenticated();
                        dismissAllowingStateLoss();
                    }
                }, 500);
            }
        }, null);
    }

    /**
     * 停止监听指纹输入
     */
    private void stopListening() {
        if (cancellationSignal != null) {
            cancellationSignal.cancel();
            cancellationSignal = null;
            isSelfCancelled = true;
        }
    }

}
