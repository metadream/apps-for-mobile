package net.cloudseat.fingerprint;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.FragmentTransaction;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class FingerprintPlugin extends CordovaPlugin {

    public static String packageName;

    private static final String DEFAULT_KEY_NAME = "_CORDOVA_FPA_KEY_";
    private static final String DIALOG_FRAGMENT_TAG = "_DIALOG_FRAGMENT_TAG_";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    private KeyStore keyStore;
    private Cipher cipher;
    private static CallbackContext mCallback;

    /**
     * 插件初始化
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        packageName = cordova.getActivity().getApplicationContext().getPackageName();
        initKey();
        initCipher();
    }

    /**
     * 插件方法调用
     */
    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callback)
        throws JSONException {
        mCallback = callback;

        if ("auth".equals(action)) {
            authenticate();
            return true;
        }
        callback.error("Undefined method:" + action);
        return false;
    }

    /**
     * 指纹授权登录
     */
    private void authenticate() {
        FingerprintDialogFragment fragment = new FingerprintDialogFragment();
        // 设置为不可取消（点击空白处无法关闭）
        fragment.setCancelable(false);
        fragment.setCipher(cipher);

        FragmentTransaction ft = cordova.getActivity().getFragmentManager().beginTransaction();
        ft.add(fragment, DIALOG_FRAGMENT_TAG);
        ft.commitAllowingStateLoss();

        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        mCallback.sendPluginResult(result);
    }

    /**
     * Key 初始化
     */
    private void initKey() {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(DEFAULT_KEY_NAME, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setUserAuthenticationRequired(true)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);

            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
            keyGenerator.init(builder.build());
            keyGenerator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Cipher 初始化
     */
    private void initCipher() {
        try {
            SecretKey key = (SecretKey) keyStore.getKey(DEFAULT_KEY_NAME, null);
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 指纹验证成功后回调（由 Fragment 类回调）
     */
    public static void onAuthenticated() {
        PluginResult result = new PluginResult(PluginResult.Status.OK);
        mCallback.sendPluginResult(result);
    }

}
