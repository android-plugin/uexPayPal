package org.zywx.wbpalmstar.plugin.paypal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;

import java.math.BigDecimal;

public class EUExPayPal extends EUExBase {

    public final String CLIENT_ID = "clientId";
    public final String MODE = "mode";
    public final String AMOUNT = "amount";
    public final String CURRENCY = "currency";
    public final String DESC = "desc";
    public final String CALLBACK_PAY = "uexPayPal.cbPay";

    public final String MODE_PRODUCTION = "production";
    public final String MODE_SANDBOX = "sandbox";
    public final String MODE_NO_NETWORK = "noNetwork";
    public final String TAG = "EUExPayPal";

    int funcId = -1;
    public EUExPayPal(Context context, EBrowserView eBrowserView) {
        super(context, eBrowserView);

    }

    PayPalConfiguration config = null;

    public void init(String[] params) {
        if (params.length == 0) {
            return;
        }
        String clientId = null;
        String mode  = null;
        try {
            JSONObject json = new JSONObject(params[0]);
            clientId = json.optString(CLIENT_ID, null);
            mode = json.optString(MODE, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //默认生产环境
        if (TextUtils.isEmpty(mode)) {
            mode = MODE_PRODUCTION;
        }
        if (!MODE_SANDBOX.equalsIgnoreCase(mode) && !MODE_PRODUCTION.equalsIgnoreCase(mode) && !MODE_NO_NETWORK.equals(mode)) {
            Log.i(TAG, "invalid mode");
        }
        //将mode值转成paypal所能识别的
        if (MODE_SANDBOX.equalsIgnoreCase(mode)) {
            mode = MODE_SANDBOX;
        } else if (MODE_NO_NETWORK.equalsIgnoreCase(mode)) {
            mode = PayPalConfiguration.ENVIRONMENT_NO_NETWORK;
        } else {
            mode = PayPalConfiguration.ENVIRONMENT_PRODUCTION;
        }
        //非测试环境下对clientId进行校验
        if ((mode != PayPalConfiguration.ENVIRONMENT_NO_NETWORK) && (TextUtils.isEmpty(clientId) || clientId.contains(" "))) {
            Log.i(TAG, "invalid clientId");
            return;
        }
        config = new PayPalConfiguration()
                .environment(mode)
                .clientId(clientId);
        Intent intent = new Intent(mContext, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        mContext.startService(intent);
    }

    public void pay(String [] params) {
        if (config == null) {
            errorCallback(0, 0, "please call init method first!");
            return;
        }
        if (params.length == 0) {
            return;
        }
        if (params.length == 2 && BUtility.isNumeric(params[1])) {
            funcId = Integer.parseInt(params[1]);
        }
        try {
            JSONObject json = new JSONObject(params[0]);
            String amount = json.optString(AMOUNT);
            String currency = json.optString(CURRENCY, "CNY");
            String itemDesc = json.optString(DESC);
            PayPalPayment payment = new PayPalPayment(new BigDecimal(amount), currency, itemDesc,
                    PayPalPayment.PAYMENT_INTENT_SALE);

            Intent intent = new Intent(mContext, PaymentActivity.class);
            intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
            intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);
            startActivityForResult(intent, 0);
        } catch (JSONException e) {
            BDebug.e(e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        try {
            JSONObject obj = new JSONObject();
            int status = 0;
            if (resultCode == Activity.RESULT_OK) {
                PaymentConfirmation confirm = intent.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                obj.put("data", confirm.toJSONObject());
            } else if (resultCode == Activity.RESULT_CANCELED) {
                status = 1;
            } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
                status = 2;
            }
            if (funcId != -1) {
                callbackToJs(funcId, false, status, obj);
            } else {
                obj.put("status", status);
                callBackJsObject(CALLBACK_PAY, obj);
            }
        } catch (JSONException e) {
            BDebug.e(e);
        }
    }

    @Override
    protected boolean clean() {
        return true;
    }

}
