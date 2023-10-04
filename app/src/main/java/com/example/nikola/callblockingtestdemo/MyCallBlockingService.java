package com.example.nikola.callblockingtestdemo;

import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.util.Log;

public class MyCallBlockingService extends CallScreeningService {
    @Override
    public void onScreenCall(Call.Details details) {
        Log.e("calls",details.getHandle().toString());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (details.getCallDirection() == Call.Details.DIRECTION_INCOMING) {
                // Block the call.
                respondToCall(details, new CallResponse.Builder().setRejectCall(true).build());
            }
        }
    }
}
