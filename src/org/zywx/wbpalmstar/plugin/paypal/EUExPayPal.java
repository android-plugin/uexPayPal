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
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;

import java.math.BigDecimal;

public class EUExPayPal extends EUExBase {

	public final String CLIENT_ID = "clientId";
	public final String AMOUNT = "amount";
	public final String CURRENCY = "currency";
	public final String ITEM_DESC = "itemDesc";
    public final String CALLBACK_PAY = "uexPayPal.cbPay";

    public final String TAG = "EUExPayPal";

    public EUExPayPal(Context context, EBrowserView eBrowserView) {
		super(context, eBrowserView);

	}

    PayPalConfiguration config = null;

    public void init(String[] params) {
        if (params.length == 0) {
            return;
        }
        String clientId = null;
        try {
            JSONObject json = new JSONObject(params[0]);
            clientId = json.optString(CLIENT_ID, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(clientId) || clientId.contains(" ")) {
            Log.i(TAG, "invalid clientId");
            return;
        }
        config = new PayPalConfiguration()
                .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
                .clientId(clientId);
        //Just for testing.
//        config = new PayPalConfiguration()
//                .environment(PayPalConfiguration.ENVIRONMENT_NO_NETWORK)
//                .clientId(clientId);
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
        try {
            JSONObject json = new JSONObject(params[0]);
            String amount = json.optString(AMOUNT);
            String currency = json.optString(CURRENCY, "CNY");
            String itemDesc = json.optString(ITEM_DESC);
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
            if (requestCode == Activity.RESULT_OK) {
                PaymentConfirmation confirm = intent.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                obj.put("status", 0);
                obj.put("data", confirm.toJSONObject());
                callBackJsObject(CALLBACK_PAY, obj);
            } else if (requestCode == Activity.RESULT_CANCELED) {
                obj.put("status", 1);
                callBackJsObject(CALLBACK_PAY, obj);
            } else if (requestCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
                obj.put("status", 2);
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
